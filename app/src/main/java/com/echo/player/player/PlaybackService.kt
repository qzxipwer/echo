package com.echo.player.player

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.echo.player.EchoApp
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** Фоновое воспроизведение + уведомление с кнопками + управление с экрана блокировки. */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Ссылка на файл запрашивается в момент загрузки трека, т.к. она быстро протухает.
        val resolver = ResolvingDataSource.Resolver { spec ->
            val uri = spec.uri
            if (uri.scheme == "ymusic") {
                spec.withUri(Uri.parse(EchoApp.api.directLink(uri.lastPathSegment!!)))
            } else {
                spec
            }
        }
        val dataSourceFactory = ResolvingDataSource.Factory(DefaultHttpDataSource.Factory(), resolver)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        session = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>,
                ): ListenableFuture<MutableList<MediaItem>> {
                    val items = mediaItems.map {
                        it.buildUpon()
                            .setUri("ymusic://track/${it.mediaId}")
                            .setMimeType(MimeTypes.AUDIO_MPEG)
                            .build()
                    }.toMutableList()
                    return Futures.immediateFuture(items)
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
