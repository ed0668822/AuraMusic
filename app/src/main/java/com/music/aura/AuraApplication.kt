package com.music.aura

import android.app.Application
import com.music.aura.data.SettingsRepository

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)
    }
}
