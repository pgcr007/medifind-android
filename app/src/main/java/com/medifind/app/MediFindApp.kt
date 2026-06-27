package com.medifind.app

import android.app.Application
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

class MediFindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )
        Configuration.getInstance().userAgentValue = packageName
    }
}