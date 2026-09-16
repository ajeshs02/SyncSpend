package com.ajesh.syncspend

import android.app.Application
import com.ajesh.syncspend.di.AppContainer
import com.ajesh.syncspend.di.DefaultAppContainer

/** Owns the app-wide manual-DI [AppContainer]. */
class SyncSpendApp : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}
