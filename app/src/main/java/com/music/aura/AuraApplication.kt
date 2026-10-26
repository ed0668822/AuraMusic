package com.music.aura

import android.app.Application
import com.music.aura.data.SettingsRepository
import com.music.aura.data.stats.ListeningStats

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)
        ListeningStats.init(this) // <--- AÑADIR ESTO
    }
}
