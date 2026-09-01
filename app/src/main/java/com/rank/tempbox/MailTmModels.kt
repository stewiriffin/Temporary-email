package com.rank.tempbox

import com.google.gson.annotations.SerializedName

data class Domain(
    @SerializedName("id") val id: String,
    @SerializedName("domain") val domain: String,
    @SerializedName("isActive") val isActive: Boolean
)

data class DomainResponse(
    @SerializedName("hydra:member") val members: List<Domain>
)

data class AccountRequest(
    @SerializedName("address") val address: String,
    @SerializedName("password") val password: String
)

data class TokenRequest(
    @SerializedName("address") val address: String,
    @SerializedName("password") val password: String
)

data class TokenResponse(
    @SerializedName("token") val token: String,
    @SerializedName("id") val id: String
)

data class EmailFrom(
    @SerializedName("address") val address: String? = null,
    @SerializedName("name") val name: String? = null,
)

data class MessagesResponse(
    @SerializedName("hydra:member") val members: List<EmailMessage>? = null,
)
