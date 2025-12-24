package com.mailfirekindle.app

import android.app.Application

class MailFireKindleApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: MailFireKindleApp
            private set
    }
}

