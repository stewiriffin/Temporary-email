package com.rank.tempbox

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

data class ExpiryInfo(
    val daysRemaining: Long,
    val hoursRemaining: Long,
    val isExpired: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

    private val _emailAddress = MutableLiveData<String>()
    val emailAddress: LiveData<String> = _emailAddress

    private val _messages = MutableLiveData<List<EmailMessage>>()
    val messages: LiveData<List<EmailMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _countdown = MutableLiveData<Int>()
    val countdown: LiveData<Int> = _countdown

    private val _expiryInfo = MutableLiveData<ExpiryInfo>()
    val expiryInfo: LiveData<ExpiryInfo> = _expiryInfo

    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline

    private val _retryAfterSeconds = MutableLiveData(0)
    private val _countdownMessage = MutableLiveData<String?>()
    val countdownMessage: LiveData<String?> = _countdownMessage

    private val _generationsRemaining = MutableLiveData(generationsRemainingToday())
    val generationsRemaining: LiveData<Int> = _generationsRemaining

    private var cachedDomains: List<Domain>? = null
    private var domainsLastFetchedAt: Long = 0L

    private var authToken: String? = null
    private var autoRefreshJob: Job? = null
    private var emailGenerationJob: Job? = null
    private var refreshJob: Job? = null
    private val inboxRefreshMutex = Mutex()
    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var previousMessageCount: Int = 0

    private val refreshInterval: Int
        get() = prefs.getInt("auto_refresh_interval", 15)

    private val autoRefreshEnabled: Boolean
        get() = prefs.getBoolean("auto_refresh_enabled", true)

    private val _pendingOpenMessageId = MutableLiveData<String?>()
    val pendingOpenMessageId: LiveData<String?> = _pendingOpenMessageId

    init {
        validateAndRepairPrefs()
        migrateToSlotKeys()
        syncExpiryTimestamps()

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val wasOffline = _isOnline.value == false
                _isOnline.postValue(true)
                if (_emailAddress.value.isNullOrBlank() && emailGenerationJob?.isActive != true) {
                    generateEmailForSlot(1)
                    return
                }
                if (wasOffline) {
                    val createdAt = Integrity.getLong(PrefKeys.createdAt(1), 0L)
                    val remaining = createdAt + 7L * 24 * 60 * 60 * 1000 - System.currentTimeMillis()
                    if (createdAt > 0L && remaining <= 0) {
                        generateEmailForSlot(1)
                    } else {
                        startAutoRefresh()
                    }
                }
            }
            override fun onLost(network: Network) {
                _isOnline.postValue(false)
            }
        }
        networkCallback = callback
        connectivityManager.registerNetworkCallback(networkRequest, callback)

        val savedEmail = prefs.getString(PrefKeys.email(1), null)
        val savedPassword = prefs.getString(PrefKeys.password(1), null)
        authToken = prefs.getString(PrefKeys.token(1), null)
            ?: prefs.getString("auth_token", null)

        if (savedEmail != null && savedPassword != null) {
            val createdAt = Integrity.getLong(PrefKeys.createdAt(1), 0L)
                .takeIf { it > 0L }
                ?: prefs.getLong("account_created_at", 0L)
            val remaining = if (createdAt > 0L) {
                createdAt + 7L * 24 * 60 * 60 * 1000 - System.currentTimeMillis()
            } else Long.MAX_VALUE

            if (remaining <= 0) {
                generateEmailForSlot(1)
            } else {
                _emailAddress.value = savedEmail
                reAuthenticate(savedEmail, savedPassword, 1)
            }
        } else {
            generateEmailForSlot(1)
        }
        updateExpiryInfo()
    }

    private fun validateAndRepairPrefs() {
        val email = prefs.getString(PrefKeys.email(1), null)
        val token = prefs.getString(PrefKeys.token(1), null)
        val password = prefs.getString(PrefKeys.password(1), null)
        if (token != null && email.isNullOrBlank()) {
            prefs.edit().remove(PrefKeys.token(1)).remove(PrefKeys.password(1)).apply()
            android.util.Log.w("TempBox", "Repaired pref: cleared token_1 because email_1 was blank")
        }
    }

    private fun migrateToSlotKeys() {
        if (prefs.getString(PrefKeys.email(1), null) == null) {
            val legacy = prefs.getString("email_address", null) ?: return
            val pw = prefs.getString("email_password", null) ?: ""
            val tok = prefs.getString("auth_token", null) ?: ""
            prefs.edit()
                .putString(PrefKeys.email(1), legacy)
                .putString(PrefKeys.password(1), pw)
                .putString(PrefKeys.token(1), tok)
                .apply()
        }
    }

    private fun syncExpiryTimestamps() {
        val plain = prefs.getLong("account_created_at", 0L)
        val secure = Integrity.getLong(PrefKeys.createdAt(1), 0L)
        when {
            plain > 0L && secure == 0L -> Integrity.putLong(PrefKeys.createdAt(1), plain)
            secure > 0L && plain == 0L -> prefs.edit().putLong("account_created_at", secure).apply()
        }
    }

    private fun setAccountCreatedAt(slot: Int, timestamp: Long) {
        prefs.edit().putLong("account_created_at", timestamp).apply()
        Integrity.putLong(PrefKeys.createdAt(slot), timestamp)
    }

    fun generateNewEmail(): Boolean {
        val today = today()
        val lastGenDate = prefs.getString("last_email_gen_date", null)
        var genCount = if (today == lastGenDate) prefs.getInt("daily_email_generations", 0) else 0

        if (genCount >= DAILY_GENERATION_LIMIT) {
            _generationsRemaining.value = 0
            _error.postValue("Daily email generation limit reached. Watch an ad on the Home screen to unlock one more.")
            return false
        }

        genCount++
        prefs.edit()
            .putString("last_email_gen_date", today)
            .putInt("daily_email_generations", genCount)
            .apply()
        _generationsRemaining.value = (DAILY_GENERATION_LIMIT - genCount).coerceAtLeast(0)

        _messages.value = emptyList()
        WidgetUpdater.updateFromMessages(getApplication(), emptyList())
        generateEmailForSlot(1)
        return true
    }

    /** Reward from a watched rewarded video: grants one extra daily address generation. */
    fun grantExtraGeneration() {
        val today = today()
        val lastGenDate = prefs.getString("last_email_gen_date", null)
        val count = prefs.getInt("daily_email_generations", 0)
        if (today == lastGenDate && count > 0) {
            prefs.edit().putInt("daily_email_generations", count - 1).apply()
        }
        _generationsRemaining.value = generationsRemainingToday()
    }

    private fun today(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())

    private fun generationsRemainingToday(): Int {
        val today = today()
        val lastGenDate = prefs.getString("last_email_gen_date", null)
        val used = if (today == lastGenDate) prefs.getInt("daily_email_generations", 0) else 0
        return (DAILY_GENERATION_LIMIT - used).coerceAtLeast(0)
    }

    companion object {
        const val DAILY_GENERATION_LIMIT = 3
    }

    private fun generateEmailForSlot(slot: Int) {
        if (emailGenerationJob?.isActive == true) return
        autoRefreshJob?.cancel()
        emailGenerationJob = viewModelScope.launch {
            _messages.value = emptyList()
            _isLoading.value = true
            var lastError: Exception? = null

            for (attempt in 1..3) {
                try {
                    android.util.Log.d("TempBox", "Generate email attempt #$attempt for slot $slot")

                    val cacheAgeMs = System.currentTimeMillis() - domainsLastFetchedAt
                    val domainResponse = if (cachedDomains != null && cacheAgeMs < 300_000L) {
                        DomainResponse(cachedDomains!!)
                    } else {
                        val response = withTimeout(15_000) {
                            RetrofitClient.unauthenticatedApi.getDomains()
                        }
                        if (response.members.isEmpty()) {
                            cachedDomains = null
                            throw Exception("No domains available from server")
                        }
                        cachedDomains = response.members
                        domainsLastFetchedAt = java.lang.System.currentTimeMillis()
                        response
                    }
                    val domain = domainResponse.members.firstOrNull { it.isActive }
                        ?: domainResponse.members.firstOrNull()
                        ?: throw Exception("No domains available from server")

                    android.util.Log.d("TempBox", "Using domain: ${domain.domain}")

                    val username = randomString(10)
                    val password = randomString(12)
                    val address = "$username@${domain.domain}"

                    android.util.Log.d("TempBox", "Creating account: $address")

                    try {
                        withTimeout(15_000) {
                            RetrofitClient.unauthenticatedApi.createAccount(AccountRequest(address, password))
                        }
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 422) {
                            android.util.Log.w("TempBox", "Address already used, retrying...")
                            continue
                        }
                        throw e
                    }

                    android.util.Log.d("TempBox", "Account created, fetching token...")

                    val tokenResponse = withTimeout(15_000) {
                        RetrofitClient.unauthenticatedApi.getToken(TokenRequest(address, password))
                    }

                    android.util.Log.d("TempBox", "Token received, saving credentials...")

                    val now = System.currentTimeMillis()
                    prefs.edit()
                        .putString(PrefKeys.email(slot), address)
                        .putString(PrefKeys.password(slot), password)
                        .putString(PrefKeys.token(slot), tokenResponse.token)
                        .putString("email_address", address)
                        .putString("email_password", password)
                        .putString("auth_token", tokenResponse.token)
                        .apply()
                    setAccountCreatedAt(slot, now)

                    authToken = tokenResponse.token
                    _emailAddress.value = address
                    _messages.value = emptyList()
                    updateExpiryInfo()
                    startAutoRefresh()

                    android.util.Log.d("TempBox", "Email generated successfully: $address")
                    _isLoading.value = false
                    return@launch

                } catch (e: Exception) {
                    if (e is retrofit2.HttpException && e.code() == 401) cachedDomains = null
                    lastError = e
                    val detail = when (e) {
                        is retrofit2.HttpException ->
                            "HTTP ${e.code()}: ${e.response()?.errorBody()?.string()}"
                        is TimeoutCancellationException -> "timeout"
                        else -> "${e.javaClass.simpleName}: ${e.message}"
                    }
                    android.util.Log.e("TempBox", "Attempt #$attempt failed: $detail", e)
                    if (e is javax.net.ssl.SSLPeerUnverifiedException ||
                        e.cause is javax.net.ssl.SSLPeerUnverifiedException
                    ) {
                        lastError = Exception("Secure connection failed. Please update the app.")
                    }
                    if (attempt < 3) {
                        val backoff = if (e is retrofit2.HttpException && e.code() == 429)
                            (RetrofitClient.retryAfter.value?.toLong() ?: 3L) * 1000L else 1_000L * attempt
                        delay(backoff)
                    }
                }
            }

            _error.value = when (lastError) {
                is retrofit2.HttpException -> when (lastError.code()) {
                    429 -> "Server is busy. Please try again in a moment."
                    422 -> "Could not generate a unique address. Please try again."
                    else -> "Server error (${lastError.code()}). Please try again."
                }
                is TimeoutCancellationException -> "Connection timed out. Check your internet."
                else -> "Error: ${lastError?.javaClass?.simpleName}: ${lastError?.message}"
            }
            _isLoading.value = false
        }
    }

    private fun reAuthenticate(address: String, password: String, slot: Int) {
        viewModelScope.launch {
            try {
                val tokenResponse = withTimeout(15_000) {
                    RetrofitClient.unauthenticatedApi.getToken(TokenRequest(address, password))
                }
                authToken = tokenResponse.token
                prefs.edit().putString(PrefKeys.token(slot), tokenResponse.token)
                    .putString("auth_token", tokenResponse.token)
                    .apply()
                startAutoRefresh()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) {
                    android.util.Log.w("TempBox", "Re-auth 401 for slot $slot — generating new email")
                    generateEmailForSlot(slot)
                } else {
                    android.util.Log.w("TempBox", "Re-auth HTTP ${e.code()} for slot $slot — keeping saved email")
                    startAutoRefresh()
                }
            } catch (e: Exception) {
                android.util.Log.w("TempBox", "Re-auth failed for slot $slot (${e.javaClass.simpleName}) — keeping saved email")
                startAutoRefresh()
            }
        }
    }

    fun refreshInbox() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshInboxSuspend()
        }
    }

    private suspend fun refreshInboxSuspend() {
        inboxRefreshMutex.withLock {
            if (emailGenerationJob?.isActive == true && authToken.isNullOrBlank()) return
            _isLoading.value = true
            try {
                var token = resolveAuthToken()
                if (token == null) {
                    if (emailGenerationJob?.isActive != true) {
                        android.util.Log.w("TempBox", "refreshInbox: no auth token available")
                    }
                    return
                }
                fetchInboxMessages(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: TimeoutCancellationException) {
                _error.value = "Inbox refresh timed out. Will retry shortly."
            } catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    401 -> {
                        authToken = null
                        val refreshed = resolveAuthToken()
                        if (refreshed != null) {
                            fetchInboxMessages(refreshed)
                        } else {
                            generateEmailForSlot(1)
                            _error.value = "Session expired. Generating a new address…"
                        }
                    }
                    429 -> {
                        _retryAfterSeconds.value = RetrofitClient.retryAfter.value ?: 5
                        _error.value = "Server is busy. Please wait."
                    }
                    else -> _error.value = "Server error (${e.code()}). Please try again."
                }
            } catch (e: Exception) {
                android.util.Log.e("TempBox", "refreshInbox failed", e)
                _error.value = "Failed to fetch inbox: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchInboxMessages(token: String) {
        val messagesResponse = withTimeout(15_000L) {
            RetrofitClient.authenticatedApi.getMessages("Bearer $token", 1, 20)
        }
        val newMessages = messagesResponse.members.orEmpty().filter { it.id.isNotBlank() }
        val oldCount = previousMessageCount
        previousMessageCount = newMessages.size
        _messages.value = newMessages

        if (oldCount > 0 && newMessages.size > oldCount) {
            val fresh = newMessages.take(newMessages.size - oldCount)
            for (msg in fresh) {
                val otp = OtpExtractor.extract(msg.intro.orEmpty())
                if (otp != null) {
                    showOtpNotification(getApplication(), otp)
                }
            }
        }
        WidgetUpdater.updateFromMessages(getApplication(), newMessages)
    }

    private suspend fun resolveAuthToken(slot: Int = 1): String? {
        authToken?.takeIf { it.isNotBlank() }?.let { return it }

        prefs.getString(PrefKeys.token(slot), null)?.takeIf { it.isNotBlank() }?.let {
            authToken = it
            return it
        }
        prefs.getString("auth_token", null)?.takeIf { it.isNotBlank() }?.let {
            authToken = it
            return it
        }

        val email = prefs.getString(PrefKeys.email(slot), null)
        val password = prefs.getString(PrefKeys.password(slot), null)
        if (email.isNullOrBlank() || password.isNullOrBlank()) return null

        return try {
            val tokenResponse = withTimeout(15_000) {
                RetrofitClient.unauthenticatedApi.getToken(TokenRequest(email, password))
            }
            authToken = tokenResponse.token
            prefs.edit()
                .putString(PrefKeys.token(slot), tokenResponse.token)
                .putString("auth_token", tokenResponse.token)
                .apply()
            tokenResponse.token
        } catch (e: retrofit2.HttpException) {
            android.util.Log.w("TempBox", "resolveAuthToken HTTP ${e.code()}")
            if (e.code() == 401) {
                authToken = null
                prefs.edit().remove(PrefKeys.token(slot)).remove("auth_token").apply()
            }
            null
        } catch (e: Exception) {
            android.util.Log.w("TempBox", "resolveAuthToken failed: ${e.message}")
            null
        }
    }

    fun deleteMessage(messageId: String) {
        val token = authToken ?: return
        _messages.value = _messages.value?.filter { it.id != messageId } ?: return
        viewModelScope.launch {
            try {
                RetrofitClient.authenticatedApi.deleteMessage("Bearer $token", messageId)
            } catch (_: Exception) {}
        }
    }

    fun updateExpiryInfo() {
        syncExpiryTimestamps()
        val createdAt = Integrity.getLong(PrefKeys.createdAt(1), 0L)
            .takeIf { it > 0L }
            ?: prefs.getLong("account_created_at", 0L)
        if (createdAt == 0L) {
            _expiryInfo.value = ExpiryInfo(7, 0, false)
            return
        }
        val remaining = createdAt + 7L * 24 * 60 * 60 * 1000 - System.currentTimeMillis()
        if (remaining <= 0) {
            _expiryInfo.value = ExpiryInfo(0, 0, true)
        } else {
            _expiryInfo.value = ExpiryInfo(
                remaining / (24L * 60 * 60 * 1000),
                (remaining % (24L * 60 * 60 * 1000)) / (60 * 60 * 1000),
                false
            )
        }
    }

    fun openMessage(messageId: String) {
        _pendingOpenMessageId.value = messageId
    }

    fun clearPendingOpenMessage() {
        _pendingOpenMessageId.value = null
    }

    fun restartAutoRefresh() {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                if (!autoRefreshEnabled) {
                    delay(2000)
                    continue
                }
                if (_isOnline.value == true) {
                    refreshInboxSuspend()
                    val interval = refreshInterval
                    for (i in interval downTo 1) {
                        if (!isActive) return@launch
                        if (!autoRefreshEnabled) break
                        if (!isOnline.value!!) {
                            _countdown.postValue(0)
                            break
                        }

                        _retryAfterSeconds.value = RetrofitClient.retryAfter.value ?: 0
                        val retrySec = _retryAfterSeconds.value ?: 0
                        if (retrySec > 0) {
                            _countdown.postValue(0)
                            for (j in retrySec downTo 1) {
                                _countdownMessage.postValue(getApplication<Application>().getString(R.string.rate_limit_message, j))
                                delay(1000)
                                if (!isActive) return@launch
                                if (!autoRefreshEnabled) break
                            }
                            _retryAfterSeconds.value = 0
                            _countdownMessage.postValue(null)
                            break
                        }

                        while (_isOnline.value == false) {
                            delay(1000)
                            if (!isActive) return@launch
                            if (!autoRefreshEnabled) break
                        }
                        if (!autoRefreshEnabled) break
                        _countdown.postValue(i)
                        delay(1_000)
                    }
                } else {
                    delay(2000)
                }
            }
        }
    }

    private fun randomString(length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return buildString { repeat(length) { append(chars.random()) } }
    }

    fun clearError() { _error.value = null }

    fun postError(message: String) { _error.postValue(message) }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
    }
}
