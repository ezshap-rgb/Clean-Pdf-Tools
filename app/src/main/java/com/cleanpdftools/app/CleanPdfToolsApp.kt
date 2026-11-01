package com.cleanpdftools.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class CleanPdfToolsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}

