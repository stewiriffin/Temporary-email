package com.rank.tempbox

import com.google.gson.annotations.SerializedName

data class EmailMessage(
    @SerializedName("id") val id: String = "",
    @SerializedName("from") val from: EmailFrom? = null,
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("intro") val intro: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("seen") val seen: Boolean = false,
)

fun EmailMessage.senderLabel(): String {
    val from = from ?: return ""
    return from.name?.takeIf { it.isNotBlank() } ?: from.address.orEmpty()
}
