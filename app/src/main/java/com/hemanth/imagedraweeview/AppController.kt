package com.hemanth.imagedraweeview

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco

class AppContrgitoller :Application() {

    override fun onCreate() {
        super.onCreate()
        Fresco.initialize(applicationContext)
    }
}
