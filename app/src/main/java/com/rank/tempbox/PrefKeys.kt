package com.rank.tempbox

object PrefKeys {
    const val PREFS_NAME = "tempbox_prefs"
    const val EMAIL = "email_"
    const val PASSWORD = "password_"
    const val TOKEN = "token_"
    const val ACCOUNT_CREATED_AT = "account_created_at"

    fun email(slot: Int) = "email_$slot"
    fun password(slot: Int) = "password_$slot"
    fun token(slot: Int) = "token_$slot"
    fun createdAt(slot: Int) = "account_created_at_$slot"
}
