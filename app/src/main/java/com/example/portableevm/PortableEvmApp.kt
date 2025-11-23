package com.example.portableevm

import android.app.Application
import com.example.portableevm.data.AppContainer

class PortableEvmApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: PortableEvmApp
            private set
    }
}
