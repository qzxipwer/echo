package com.echo.player.data

import android.util.Base64
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Клиент НЕОФИЦИАЛЬНОГО API Яндекс Музыки (api.music.yandex.net).
 * Яндекс может изменить его в любой момент, тогда придётся править этот файл.
 */
class YandexApi(private val tokenProvider: () -> String?) {

    private val http = OkHttpClient()
    private val base = "https://api.music.yandex.net"
    private var cachedUid: String? = null

    fun resetUid() {
        cachedUid = null
    }

    private fun call(path: String, form: Map<String, String>? = null): JSONObject {
        val token = tokenProvider() ?: throw IOException("Нет токена")
        val builder = Request.Builder()
            .url(base + path)
            .header("Authorization", "OAuth $token")
            .header("X-Yandex-Music-Client", "YandexMusicAndroid/24023621")
        if (form != null) {
            val body = FormBody.Builder()
            form.forEach { (k, v) -> body.add(k, v) }
            builder.post(body.build())
        }
        http.newCall(builder.build()).execute().use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("Ошибка сервера: HTTP ${r.code}")
            return JSONObject(text)
        }
    }

    /** Проверяет токен и возвращает uid аккаунта. */
    fun uid(): String {
        cachedUid?.let { return it }
        val res = call("/account/status").getJSONObject("result")
        val uid = res.getJSONObject("account").get("uid").toString()
        cachedUid = uid
        return uid
    }

    private fun cover(raw: String?): String? =
        raw?.takeIf { it.isNotEmpty() }?.let { "https://" + it.replace("%%", "400x400") }

    private fun parseTrack(o: JSONObject): Track {
        val artists = o.optJSONArray("artists")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name") }
        }.orEmpty()
        val album = o.optJSONArray("albums")?.optJSONObject(0)?.optString("title").orEmpty()
        return Track(
            id = o.get("id").toString().substringBefore(':'),
            title = o.optString("title"),
            artist = artists.joinToString(", "),
            album = album,
            coverUrl = cover(o.optString("coverUri")),
            durationMs = o.optLong("durationMs"),
        )
    }

    private fun tracksByIds(ids: List<String>): List<Track> {
        val out = ArrayList<Track>()
        ids.chunked(100).forEach { chunk ->
            val arr = call("/tracks", mapOf("track-ids" to chunk.joinToString(",")))
                .getJSONArray("result")
            for (i in 0 until arr.length()) out.add(parseTrack(arr.getJSONObject(i)))
        }
        return out
    }

    fun likes(): List<Track> {
        val arr = call("/users/${uid()}/likes/tracks")
            .getJSONObject("result").getJSONObject("library").getJSONArray("tracks")
        val ids = (0 until arr.length()).map { arr.getJSONObject(it).get("id").toString() }
        return tracksByIds(ids)
    }

    fun playlists(): List<Playlist> {
        val arr = call("/users/${uid()}/playlists/list").getJSONArray("result")
        return (0 until arr.length()).map { i ->
            val p = arr.getJSONObject(i)
            val c = p.optJSONObject("cover")
            val uri = c?.optString("uri")?.takeIf { it.isNotEmpty() }
                ?: c?.optJSONArray("itemsUri")?.optString(0)
            Playlist(
                kind = p.get("kind").toString(),
                title = p.optString("title"),
                trackCount = p.optInt("trackCount"),
                coverUrl = cover(uri),
            )
        }
    }

    fun playlistTracks(kind: String): List<Track> {
        val arr: JSONArray = call("/users/${uid()}/playlists/$kind")
            .getJSONObject("result").optJSONArray("tracks") ?: return emptyList()
        val ready = ArrayList<Track>()
        val missing = ArrayList<String>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val t = item.optJSONObject("track")
            if (t != null) ready.add(parseTrack(t)) else missing.add(item.get("id").toString())
        }
        return ready + tracksByIds(missing)
    }

    fun search(query: String): List<Track> {
        val q = URLEncoder.encode(query, "UTF-8")
        val res = call("/search?text=$q&type=track&page=0&nocorrect=false").getJSONObject("result")
        val arr = res.optJSONObject("tracks")?.optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).map { parseTrack(arr.getJSONObject(it)) }
    }

    /** Прямая ссылка на аудио. Ссылки живут недолго, поэтому запрашиваем перед воспроизведением. */
    fun directLink(trackId: String): String {
        return try {
            fileInfoLink(trackId)
        } catch (e: Exception) {
            legacyLink(trackId)
        }
    }

    private fun fileInfoLink(trackId: String): String {
        val ts = System.currentTimeMillis() / 1000
        val quality = "nq"
        val codecs = "mp3"
        val transports = "raw"
        val data = "$ts$trackId$quality$codecs$transports"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec("kzqU4XhfCaY6B6JTHODeq5".toByteArray(), "HmacSHA256"))
        val sign = Base64.encodeToString(mac.doFinal(data.toByteArray()), Base64.NO_WRAP or Base64.NO_PADDING)
        val path = "/get-file-info?ts=$ts&trackId=$trackId&quality=$quality&codecs=$codecs" +
            "&transports=$transports&sign=${URLEncoder.encode(sign, "UTF-8")}"
        return call(path).getJSONObject("result").getJSONObject("downloadInfo").getString("url")
    }

    private fun legacyLink(trackId: String): String {
        val infos = call("/tracks/$trackId/download-info").getJSONArray("result")
        var best: JSONObject? = null
        for (i in 0 until infos.length()) {
            val o = infos.getJSONObject(i)
            if (o.optString("codec") != "mp3") continue
            if (best == null || o.optInt("bitrateInKbps") > best.optInt("bitrateInKbps")) best = o
        }
        val infoUrl = best?.optString("downloadInfoUrl")?.takeIf { it.isNotEmpty() }
            ?: throw IOException("Нет ссылки на трек")
        val token = tokenProvider().orEmpty()
        val xml = http.newCall(
            Request.Builder().url(infoUrl).header("Authorization", "OAuth $token").build()
        ).execute().use { it.body?.string().orEmpty() }

        fun tag(name: String) = Regex("<$name>(.*?)</$name>").find(xml)?.groupValues?.get(1)
            ?: throw IOException("Неожиданный ответ сервера")

        val host = tag("host")
        val path = tag("path")
        val ts = tag("ts")
        val s = tag("s")
        val raw = "XGRlBW9FXlekgbPrRHuSiA" + path.substring(1) + s
        val md5 = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "https://$host/get-mp3/$md5/$ts$path"
    }
}
