package com.valsi.invoicesystem

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. [HiltAndroidApp] triggers Hilt code generation and creates
 * the application-level dependency container (database, repositories, etc.).
 */
@HiltAndroidApp
class ValsiApplication : Application()
