package com.rank.tempbox

import com.google.gson.annotations.SerializedName

data class MessageDetail(
    @SerializedName("id") val id: String = "",
    @SerializedName("from") val from: EmailFrom? = null,
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("intro") val intro: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("html") val html: List<String>? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("seen") val seen: Boolean = false,
)
