package com.docvault.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.PdfDecoder

class DocVaultApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(PdfDecoder.Factory()) }
            .crossfade(true)
            .build()
}