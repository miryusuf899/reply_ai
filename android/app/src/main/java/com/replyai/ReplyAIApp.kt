package com.replyai

import android.app.Application
import com.replyai.utils.TokenManager

class ReplyAIApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        TokenManager.getInstance(this)
    }

    companion object {
        lateinit var instance: ReplyAIApp
            private set
    }
}
