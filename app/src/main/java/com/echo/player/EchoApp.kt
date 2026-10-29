package com.echo.player

import android.app.Application
import com.echo.player.data.TokenStore
import com.echo.player.data.YandexApi

class EchoApp : Application() {
    companion object {
        lateinit var tokens: TokenStore
        lateinit var api: YandexApi
    }

    override fun onCreate() {
        super.onCreate()
        tokens = TokenStore(this)
        api = YandexApi { tokens.token }
    }
}
