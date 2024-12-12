package me.kifio.kreader.android.utils.extensions

import android.content.Context

val Context.screenWidth: Int
    get() = resources.displayMetrics.widthPixels

val Context.screenHeight: Int
    get() = resources.displayMetrics.heightPixels
