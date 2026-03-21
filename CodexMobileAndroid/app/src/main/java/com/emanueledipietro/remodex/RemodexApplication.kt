package com.emanueledipietro.remodex

import android.app.Application
import com.emanueledipietro.remodex.data.app.RemodexAppContainer

class RemodexApplication : Application() {
    lateinit var container: RemodexAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = RemodexAppContainer(this)
    }
}
