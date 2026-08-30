package com.meow.assistant

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

lateinit var assistantApp: MeowAssistantApplication

class MeowAssistantApplication : Application(), ViewModelStoreOwner {
    private val appViewModelStore by lazy { ViewModelStore() }

    override fun onCreate() {
        super.onCreate()
        assistantApp = this
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}

