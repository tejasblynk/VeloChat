package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    CHATS_LIST,
    ACTIVE_CHAT,
    PROFILE_EDIT
}

data class VideoCallState(
    val isActive: Boolean = false,
    val participantName: String = "",
    val participantAvatar: String = "",
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val durationSeconds: Int = 0,
    val connectionSecureKey: String = "velo_sec_shk_90"
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(application)

    // UI States
    val users: StateFlow<List<ChatUser>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val groups: StateFlow<List<ChatGroup>> = repository.allGroups.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    private val _currentScreen = MutableStateFlow(Screen.CHATS_LIST)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeChatName = MutableStateFlow("")
    val activeChatName: StateFlow<String> = _activeChatName.asStateFlow()

    private val _activeChatAvatar = MutableStateFlow("")
    val activeChatAvatar: StateFlow<String> = _activeChatAvatar.asStateFlow()

    private val _activeChatIsGroup = MutableStateFlow(false)
    val activeChatIsGroup: StateFlow<Boolean> = _activeChatIsGroup.asStateFlow()

    private val _activeChatEncryptionKey = MutableStateFlow("")
    val activeChatEncryptionKey: StateFlow<String> = _activeChatEncryptionKey.asStateFlow()

    // Observe active chat messages
    val activeMessages: StateFlow<List<ChatMessage>> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForChat(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form inputs & view toggles
    var inputText = MutableStateFlow("")
    var replyToMessage = MutableStateFlow<ChatMessage?>(null)
    var disappearingDurationSelected = MutableStateFlow<Long>(0L) // 0 means normal, non-disappearing

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    // Video Call simulation state
    private val _videoCallState = MutableStateFlow(VideoCallState())
    val videoCallState: StateFlow<VideoCallState> = _videoCallState.asStateFlow()
    private var videoCallTimerJob: Job? = null

    // Voice typing & note recorder simulation state
    var isVoiceRecording = MutableStateFlow(false)
    var voiceRecordDuration = MutableStateFlow(0)
    var voiceWaves = MutableStateFlow(emptyList<Float>())
    private var voiceTimerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDataIfNeeded()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectChat(id: String, name: String, avatar: String, isGroup: Boolean, encKey: String) {
        _activeChatId.value = id
        _activeChatName.value = name
        _activeChatAvatar.value = avatar
        _activeChatIsGroup.value = isGroup
        _activeChatEncryptionKey.value = encKey
        _aiSummary.value = null
        _currentScreen.value = Screen.ACTIVE_CHAT
    }

    fun deselectChat() {
        _activeChatId.value = null
        _currentScreen.value = Screen.CHATS_LIST
    }

    fun setReplyTo(message: ChatMessage?) {
        replyToMessage.value = message
    }

    fun updateProfile(name: String, statusText: String, badge: String, border: String) {
        viewModelScope.launch {
            repository.saveUserProfile(
                UserProfile(name = name, statusText = statusText, customBadge = badge, activeBorderColor = border)
            )
            _currentScreen.value = Screen.CHATS_LIST
        }
    }

    fun createGroupChat(name: String, description: String, selectedMemberIds: List<String>) {
        viewModelScope.launch {
            val groupId = "group_${System.currentTimeMillis()}"
            val group = ChatGroup(
                id = groupId,
                name = name,
                avatarUrl = "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?w=150",
                description = description,
                createdBy = "Alex",
                memberIds = ("me," + selectedMemberIds.joinToString(","))
            )
            repository.insertGroup(group)

            // Auto-send creation welcome message
            val welcomeMsg = ChatMessage(
                chatId = groupId,
                senderId = "system",
                senderName = "System",
                senderAvatar = "",
                content = "Group '$name' created. End-to-End E2E cryptosystem activated for all members.",
                isEncrypted = true,
                encryptionKey = "velo_sec_grp_${groupId.hashCode().coerceAtLeast(0).toString(16)}"
            )
            repository.insertMessage(welcomeMsg)

            // Select group
            selectChat(
                id = groupId,
                name = name,
                avatar = group.avatarUrl,
                isGroup = true,
                encKey = welcomeMsg.encryptionKey
            )
        }
    }

    fun sendMessage(mediaUri: String? = null, mediaType: String? = null, voiceSeconds: Int = 0, voiceTranscript: String? = null) {
        val text = inputText.value
        val chatId = _activeChatId.value ?: return
        val profile = userProfile.value

        if (text.isBlank() && mediaUri == null && mediaType == null) return

        viewModelScope.launch {
            val isDisappear = disappearingDurationSelected.value > 0L
            val msg = ChatMessage(
                chatId = chatId,
                senderId = "me",
                senderName = profile.name,
                senderAvatar = profile.avatarUrl,
                content = text,
                isEncrypted = true,
                encryptionKey = _activeChatEncryptionKey.value,
                mediaUri = mediaUri,
                mediaType = mediaType,
                isDisappearing = isDisappear,
                disappearDuration = disappearingDurationSelected.value,
                voiceDuration = voiceSeconds,
                voiceTranscription = voiceTranscript,
                replyToId = replyToMessage.value?.id,
                replyToSender = replyToMessage.value?.senderName,
                replyToContent = replyToMessage.value?.content
            )

            repository.insertMessage(msg)
            inputText.value = ""
            replyToMessage.value = null
            disappearingDurationSelected.value = 0L // reset after send

            // Simulate ultra-fast automatic E2E encrypted friend response!
            simulateFastResponse(chatId, text)
        }
    }

    private suspend fun simulateFastResponse(chatId: String, textSent: String) {
        if (chatId.startsWith("group_")) {
            // Group simulate responses from other core users
            delay(800)
            val randomReplier = users.value.filter { it.id != "me" }.shuffled().firstOrNull() ?: return
            val responseContent = when {
                textSent.lowercase().contains("poll") -> "Voted! Great collaborative tool right inside VeloChat."
                textSent.lowercase().contains("task") -> "I'll grab the next task on the checklist."
                textSent.lowercase().contains("event") -> "RSVP'd! Count me in."
                else -> "Got your secure payload. Processing instantly! VeloChat delivery sub-10ms is no joke. ⚡"
            }
            val autoGroupMsg = ChatMessage(
                chatId = chatId,
                senderId = randomReplier.id,
                senderName = randomReplier.name,
                senderAvatar = randomReplier.avatarUrl,
                content = responseContent,
                isEncrypted = true,
                encryptionKey = _activeChatEncryptionKey.value
            )
            repository.insertMessage(autoGroupMsg)
        } else {
            // 1-to-1 response simulation
            delay(600)
            val friend = users.value.find { it.id == chatId } ?: return
            val responseContent = when {
                textSent.lowercase().contains("hello") || textSent.lowercase().contains("hi") -> "Hey! Super fast connection on VeloChat. What's up?"
                textSent.lowercase().contains("call") -> "Ready for an encrypted video call anytime! Just click the camera icon at the top."
                textSent.lowercase().contains("disappear") -> "Wow, the disappearing media countdown is so smooth! Read-and-burn style."
                else -> "Received E2E package. Decrypted with key: ${friend.encryptionKey}."
            }
            val autoMsg = ChatMessage(
                chatId = chatId,
                senderId = friend.id,
                senderName = friend.name,
                senderAvatar = friend.avatarUrl,
                content = responseContent,
                isEncrypted = true,
                encryptionKey = friend.encryptionKey
            )
            repository.insertMessage(autoMsg)
        }
    }

    // Interactive Group Collaborative Tools triggers
    fun createGroupPoll(question: String, options: List<String>) {
        val chatId = _activeChatId.value ?: return
        val profile = userProfile.value
        viewModelScope.launch {
            val msg = ChatMessage(
                chatId = chatId,
                senderId = "me",
                senderName = profile.name,
                senderAvatar = profile.avatarUrl,
                content = question,
                mediaType = "poll",
                pollOptions = options.joinToString(","),
                pollVotes = List(options.size) { "0" }.joinToString(","),
                isEncrypted = false // collaborative modules can be shared/public within group keys
            )
            repository.insertMessage(msg)
        }
    }

    fun voteOnPoll(message: ChatMessage, optionIndex: Int) {
        viewModelScope.launch {
            val votes = message.pollVotes?.split(",")?.map { it.toInt() }?.toMutableList() ?: return@launch
            if (optionIndex in votes.indices) {
                votes[optionIndex] = votes[optionIndex] + 1
            }
            val updatedMsg = message.copy(pollVotes = votes.joinToString(","))
            repository.updateMessage(updatedMsg)
        }
    }

    fun createGroupEvent(title: String, eventTime: String) {
        val chatId = _activeChatId.value ?: return
        val profile = userProfile.value
        viewModelScope.launch {
            val msg = ChatMessage(
                chatId = chatId,
                senderId = "me",
                senderName = profile.name,
                senderAvatar = profile.avatarUrl,
                content = "📅 New Collaborative Event Scheduled!",
                mediaType = "event_planner",
                eventTitle = title,
                eventTime = eventTime,
                eventRsvps = "Alex:YES"
            )
            repository.insertMessage(msg)
        }
    }

    fun rsvpToEvent(message: ChatMessage, userName: String, rsvpStatus: String) {
        viewModelScope.launch {
            val rsvps = message.eventRsvps?.split(",")?.toMutableList() ?: mutableListOf()
            // Remove existing user RSVP if present
            rsvps.removeAll { it.startsWith("$userName:") }
            rsvps.add("$userName:$rsvpStatus")

            val updatedMsg = message.copy(eventRsvps = rsvps.joinToString(","))
            repository.updateMessage(updatedMsg)
        }
    }

    fun createGroupTaskList(tasks: List<String>) {
        val chatId = _activeChatId.value ?: return
        val profile = userProfile.value
        viewModelScope.launch {
            val taskString = tasks.map { "$it:false" }.joinToString(",")
            val msg = ChatMessage(
                chatId = chatId,
                senderId = "me",
                senderName = profile.name,
                senderAvatar = profile.avatarUrl,
                content = "📝 Interactive Group Tasks Checklist:",
                mediaType = "task_list",
                taskItems = taskString
            )
            repository.insertMessage(msg)
        }
    }

    fun toggleTaskItem(message: ChatMessage, taskIndex: Int) {
        viewModelScope.launch {
            val items = message.taskItems?.split(",")?.toMutableList() ?: return@launch
            if (taskIndex in items.indices) {
                val item = items[taskIndex]
                val parts = item.split(":")
                if (parts.size >= 2) {
                    val taskName = parts[0]
                    val currentChecked = parts[1].toBoolean()
                    items[taskIndex] = "$taskName:${!currentChecked}"
                }
            }
            val updatedMsg = message.copy(taskItems = items.joinToString(","))
            repository.updateMessage(updatedMsg)
        }
    }

    // Disappearing media viewing trigger
    fun triggerDisappearingMessageCountdown(message: ChatMessage) {
        viewModelScope.launch {
            // Count down and then set disappeared flag
            delay(message.disappearDuration * 1000L)
            val expiredMsg = message.copy(disappeared = true)
            repository.updateMessage(expiredMsg)
        }
    }

    // AI summary trigger
    fun generateChatSummary() {
        val id = _activeChatId.value ?: return
        val chatName = _activeChatName.value
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            _aiSummary.value = null
            val recentMessages = activeMessages.value
            val summaryResult = repository.getChatSummary(chatName, recentMessages)
            _aiSummary.value = summaryResult
            _isGeneratingSummary.value = false
        }
    }

    fun clearSummary() {
        _aiSummary.value = null
    }

    // Simulated Video Call logic
    fun startVideoCall() {
        val name = _activeChatName.value
        val avatar = _activeChatAvatar.value
        _videoCallState.value = VideoCallState(
            isActive = true,
            participantName = name,
            participantAvatar = avatar,
            durationSeconds = 0,
            connectionSecureKey = "velo_sec_shk_${(100..999).random()}"
        )

        // Start duration counter
        videoCallTimerJob?.cancel()
        videoCallTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _videoCallState.value
                if (current.isActive) {
                    _videoCallState.value = current.copy(durationSeconds = current.durationSeconds + 1)
                } else {
                    break
                }
            }
        }
    }

    fun endVideoCall() {
        videoCallTimerJob?.cancel()
        _videoCallState.value = VideoCallState(isActive = false)

        // Log call as chat notification
        val chatId = _activeChatId.value ?: return
        val profile = userProfile.value
        viewModelScope.launch {
            val callLogMsg = ChatMessage(
                chatId = chatId,
                senderId = "system",
                senderName = "System",
                senderAvatar = "",
                content = "📞 Encrypted Video Call Completed. E2E Cryptography active.",
                isEncrypted = true,
                encryptionKey = _activeChatEncryptionKey.value
            )
            repository.insertMessage(callLogMsg)
        }
    }

    fun toggleMuteCall() {
        _videoCallState.value = _videoCallState.value.copy(isMuted = !_videoCallState.value.isMuted)
    }

    fun toggleCameraCall() {
        _videoCallState.value = _videoCallState.value.copy(isCameraOff = !_videoCallState.value.isCameraOff)
    }

    // Simulated Voice Notes and Speech Transcription logic
    fun startVoiceRecording() {
        isVoiceRecording.value = true
        voiceRecordDuration.value = 0
        voiceWaves.value = List(30) { (10..100).random().toFloat() }

        voiceTimerJob?.cancel()
        voiceTimerJob = viewModelScope.launch {
            while (isVoiceRecording.value) {
                delay(100)
                // update waves and duration
                voiceWaves.value = List(30) { (15..100).random().toFloat() }
                if (System.currentTimeMillis() % 1000 < 100) {
                    voiceRecordDuration.value = voiceRecordDuration.value + 1
                }
            }
        }
    }

    fun stopAndSendVoiceRecording() {
        isVoiceRecording.value = false
        voiceTimerJob?.cancel()
        val duration = voiceRecordDuration.value.coerceAtLeast(3)

        // Auto transcribe mock speech transcripts based on conversational context!
        val mockTranscripts = listOf(
            "Hey, I am testing the high speed voice typed encrypted message. VeloChat works so fast!",
            "E2E secure connection check. Audio package arrived in 2ms.",
            "Can we sync the files later for the collaborative planner?",
            "Let's jump on a secure video call soon. I have some disappearing diagrams to share.",
            "Awesome interactive tools. I just voted on the poll!"
        )
        val transcript = mockTranscripts.random()

        sendMessage(
            mediaUri = "simulated_voice_package_${System.currentTimeMillis()}.ogg",
            mediaType = "voice",
            voiceSeconds = duration,
            voiceTranscription = transcript
        )
    }

    fun cancelVoiceRecording() {
        isVoiceRecording.value = false
        voiceTimerJob?.cancel()
    }
}
