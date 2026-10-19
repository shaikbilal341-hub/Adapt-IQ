package com.example.adaptiq.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.adaptiq.admin.auth.AdminAuthManager
import com.example.adaptiq.admin.sync.AdminApiClient
import com.example.adaptiq.admin.sync.AdminSessionDto
import com.example.adaptiq.admin.sync.SessionDetailDto
import com.example.adaptiq.admin.ui.screens.*
import com.example.adaptiq.admin.ui.theme.AdaptIQAdminTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AdminScreen {
    LOGIN,
    DASHBOARD,
    PARTICIPANTS,
    SESSIONS,
    SESSION_DETAIL,
    EXPORT
}

class AdminMainActivity : ComponentActivity() {

    private lateinit var authManager: AdminAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AdminAuthManager(this)
        AdminApiClient.setServerUrl(authManager.getServerUrl())

        setContent {
            AdaptIQAdminTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdminAppRouter()
                }
            }
        }
    }

    @Composable
    private fun AdminAppRouter() {
        var currentScreen by remember {
            mutableStateOf(if (authManager.isLoggedIn()) AdminScreen.DASHBOARD else AdminScreen.LOGIN)
        }

        var sessionsList by remember { mutableStateOf<List<AdminSessionDto>>(emptyList()) }
        var selectedSessionDetail by remember { mutableStateOf<SessionDetailDto?>(null) }
        var isLoadingSessions by remember { mutableStateOf(false) }
        var isLoadingDetail by remember { mutableStateOf(false) }
        var sessionsError by remember { mutableStateOf<String?>(null) }
        var detailError by remember { mutableStateOf<String?>(null) }

        val coroutineScope = rememberCoroutineScope()

        fun loadSessions() {
            val token = authManager.getToken() ?: return
            isLoadingSessions = true
            sessionsError = null
            coroutineScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        AdminApiClient.api.getSessions("Bearer $token")
                    }
                    isLoadingSessions = false
                    if (response.isSuccessful && response.body() != null) {
                        sessionsList = response.body()!!
                    } else {
                        sessionsError = "Failed to load sessions (Code: ${response.code()})"
                    }
                } catch (e: Exception) {
                    isLoadingSessions = false
                    sessionsError = "Backend unreachable (${e.localizedMessage})"
                }
            }
        }

        fun loadSessionDetail(sessionId: String) {
            val token = authManager.getToken() ?: return
            isLoadingDetail = true
            detailError = null
            selectedSessionDetail = null
            currentScreen = AdminScreen.SESSION_DETAIL

            coroutineScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        AdminApiClient.api.getSessionDetail("Bearer $token", sessionId)
                    }
                    isLoadingDetail = false
                    if (response.isSuccessful && response.body() != null) {
                        selectedSessionDetail = response.body()!!
                    } else {
                        detailError = "Failed to load session details (Code: ${response.code()})"
                    }
                } catch (e: Exception) {
                    isLoadingDetail = false
                    detailError = "Backend unreachable (${e.localizedMessage})"
                }
            }
        }

        when (currentScreen) {
            AdminScreen.LOGIN -> AdminLoginScreen(
                initialServerUrl = authManager.getServerUrl(),
                onSaveServerUrl = { url -> authManager.saveServerUrl(url) },
                onLoginSuccess = { token ->
                    authManager.saveToken(token)
                    currentScreen = AdminScreen.DASHBOARD
                    loadSessions()
                }
            )

            AdminScreen.DASHBOARD -> AdminDashboardScreen(
                sessionCount = sessionsList.size,
                onNavigateParticipants = { currentScreen = AdminScreen.PARTICIPANTS },
                onNavigateSessions = {
                    currentScreen = AdminScreen.SESSIONS
                    loadSessions()
                },
                onNavigateExport = { currentScreen = AdminScreen.EXPORT },
                onLogout = {
                    authManager.clearToken()
                    currentScreen = AdminScreen.LOGIN
                }
            )

            AdminScreen.PARTICIPANTS -> ParticipantListScreen(
                sessions = sessionsList,
                onBack = { currentScreen = AdminScreen.DASHBOARD }
            )

            AdminScreen.SESSIONS -> SessionHistoryScreen(
                sessions = sessionsList,
                isLoading = isLoadingSessions,
                errorMessage = sessionsError,
                onSelectSession = { sessionId -> loadSessionDetail(sessionId) },
                onRefresh = { loadSessions() },
                onBack = { currentScreen = AdminScreen.DASHBOARD }
            )

            AdminScreen.SESSION_DETAIL -> SessionDetailScreen(
                sessionDetail = selectedSessionDetail,
                isLoading = isLoadingDetail,
                errorMessage = detailError,
                onBack = { currentScreen = AdminScreen.SESSIONS }
            )

            AdminScreen.EXPORT -> DatasetExportScreen(
                onBack = { currentScreen = AdminScreen.DASHBOARD }
            )
        }
    }
}
