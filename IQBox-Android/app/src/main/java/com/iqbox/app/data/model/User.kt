package com.iqbox.app.data.model

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val subscription_status: String,
    val subscription_expiry: String? = null
)
