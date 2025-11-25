package com.example.snapitout

import android.graphics.Color

data class Template(
    val id: Long,
    val name: String,
    val slotUris: ArrayList<String>,
    val frameColor: Int = Color.WHITE
)
