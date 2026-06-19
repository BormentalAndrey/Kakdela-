// Сохранить в app/src/main/java/com/vasilisina/azbuka/VasilisaApp.kt

package com.vasilisina.azbuka

import android.app.Application
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.data.GameState
import com.vasilisina.azbuka.data.ProgressManager

class VasilisaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ProgressManager.init(this)
        AudioPlayer.init(this)
        val saved = ProgressManager.load()
        GameState.loadFrom(saved)
    }

    override fun onTerminate() {
        super.onTerminate()
        AudioPlayer.release()
    }
}
