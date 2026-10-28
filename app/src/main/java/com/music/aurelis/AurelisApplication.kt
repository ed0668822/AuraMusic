package com.music.aurelis

import android.app.Application
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.stats.ListeningStats

class AurelisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)
        ProfileRepository.init(this)
        ListeningStats.init(this)
    }
}
