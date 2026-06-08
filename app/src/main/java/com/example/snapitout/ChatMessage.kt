package com.example.snapitout

data class ChatMessage(
    val text: String? = null,
    val imageUrl: String? = null,
    val isUser: Boolean
)