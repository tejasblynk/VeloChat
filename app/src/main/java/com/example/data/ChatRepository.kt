package com.example.data

import android.content.Context
import androidx.room.Room
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ChatRepository(private val context: Context) {

    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "velochat_database"
    ).fallbackToDestructiveMigration().build()

    private val chatDao = db.chatDao()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Expose DB flows
    val allUsers: Flow<List<ChatUser>> = chatDao.getAllUsers()
    val allGroups: Flow<List<ChatGroup>> = chatDao.getAllGroups()
    val userProfile: Flow<UserProfile?> = chatDao.getUserProfileFlow()

    fun getMessagesForChat(chatId: String): Flow<List<ChatMessage>> = chatDao.getMessagesForChat(chatId)

    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.updateMessage(message)
    }

    suspend fun insertGroup(group: ChatGroup) = withContext(Dispatchers.IO) {
        chatDao.insertGroup(group)
    }

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        chatDao.insertUserProfile(profile)
    }

    suspend fun initializeDataIfNeeded() = withContext(Dispatchers.IO) {
        // Initialize User Profile
        if (chatDao.getUserProfile() == null) {
            chatDao.insertUserProfile(UserProfile())
        }

        // Initialize Users if table is empty
        val existingUsers = chatDao.getAllUsers().firstOrNull()
        if (existingUsers.isNullOrEmpty()) {
            val coreUsers = listOf(
                ChatUser("sarah_connor", "Sarah Connor", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150", "Secure everything. 🔒", "velo_sec_k98s_2f", true, System.currentTimeMillis(), "🛡️ SecOps", "#4CAF50"),
                ChatUser("jordan_miller", "Jordan Miller", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150", "VeloChat speed is insane! ⚡", "velo_sec_j8a2_92", true, System.currentTimeMillis(), "⚡ Coordinator", "#FF9800"),
                ChatUser("chloe_baker", "Chloe Baker", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", "Designing next-gen visuals 🎨", "velo_sec_c3a1_8d", false, System.currentTimeMillis() - 600000, "🎨 Design Lead", "#E91E63"),
                ChatUser("ethan_carter", "Ethan Carter", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", "Zero-trust E2E protocol verified.", "velo_sec_e4b9_aa", true, System.currentTimeMillis(), "⚙️ Admin", "#2196F3")
            )
            chatDao.insertUsers(coreUsers)

            // Generate 85 more users to make it a realistic platform of ~90 people!
            val firstNames = listOf("Liam", "Olivia", "Noah", "Emma", "Oliver", "Ava", "Elijah", "Charlotte", "William", "Sophia", "James", "Amelia", "Benjamin", "Isabella", "Lucas", "Mia", "Henry", "Evelyn", "Alexander", "Harper")
            val lastNames = listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin")
            val badges = listOf("Member", "Beta", "Velo Pro", "Elite", null, null, null, null)
            val colors = listOf("#00FFCC", "#FF3366", "#CC33FF", "#33FF57", "#FFFF33", null, null)

            val generatedUsers = ArrayList<ChatUser>()
            for (i in 1..85) {
                val fName = firstNames[i % firstNames.size]
                val lName = lastNames[(i * 7) % lastNames.size]
                val fullName = "$fName $lName"
                val id = "${fName.lowercase()}_${lName.lowercase()}_$i"
                val avatarIndex = (i % 20) + 10
                val avatar = "https://images.unsplash.com/photo-${1500000000000 + (i * 200000)}?w=150"
                val isOnline = i % 4 == 0
                val customBadge = badges[i % badges.size]
                val activeBorder = colors[i % colors.size]
                val encryptionKey = "velo_sec_${id.hashCode().coerceAtLeast(0).toString(16)}"

                generatedUsers.add(
                    ChatUser(
                        id = id,
                        name = fullName,
                        avatarUrl = avatar,
                        statusText = "Encrypted connection secure. ⚡",
                        encryptionKey = encryptionKey,
                        isOnline = isOnline,
                        lastActive = System.currentTimeMillis() - (i * 1200000L),
                        customBadge = customBadge,
                        activeBorderColor = activeBorder
                    )
                )
            }
            chatDao.insertUsers(generatedUsers)
        }

        // Initialize Groups if empty
        val existingGroups = chatDao.getAllGroups().firstOrNull()
        if (existingGroups.isNullOrEmpty()) {
            val devGroup = ChatGroup(
                id = "group_dev_core",
                name = "⚡ Dev Core Group",
                avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150",
                description = "Core architects working on the ultra-fast VeloChat E2E encryption.",
                createdBy = "Alex",
                memberIds = "me,sarah_connor,jordan_miller,chloe_baker,ethan_carter"
            )
            val matrixGroup = ChatGroup(
                id = "group_sec_matrix",
                name = "🔒 Security Matrix",
                avatarUrl = "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=150",
                description = "Confidential encryption auditing and privacy protocols.",
                createdBy = "Sarah Connor",
                memberIds = "me,sarah_connor,ethan_carter"
            )
            chatDao.insertGroup(devGroup)
            chatDao.insertGroup(matrixGroup)

            // Seed initial messages for Dev Core
            val msgs = listOf(
                ChatMessage(
                    chatId = "group_dev_core",
                    senderId = "ethan_carter",
                    senderName = "Ethan Carter",
                    senderAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    content = "Welcome everyone to VeloChat Core! Remember: all messages here are AES-256 E2E encrypted locally before broadcast.",
                    isEncrypted = true,
                    encryptionKey = "velo_sec_e4b9_aa_group"
                ),
                ChatMessage(
                    chatId = "group_dev_core",
                    senderId = "jordan_miller",
                    senderName = "Jordan Miller",
                    senderAvatar = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                    content = "Hey admin! Testing out the speed. It literally loads instantly. 🚀",
                    isEncrypted = true,
                    encryptionKey = "velo_sec_e4b9_aa_group"
                ),
                ChatMessage(
                    chatId = "group_dev_core",
                    senderId = "chloe_baker",
                    senderName = "Chloe Baker",
                    senderAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    content = "I styled the adaptive dark mode using deep midnight gradients. It looks stunning and reduces eye strain 🌃",
                    isEncrypted = true,
                    encryptionKey = "velo_sec_e4b9_aa_group"
                ),
                // Insert a mock group poll message
                ChatMessage(
                    chatId = "group_dev_core",
                    senderId = "jordan_miller",
                    senderName = "Jordan Miller",
                    senderAvatar = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                    content = "What should we add next?",
                    mediaType = "poll",
                    pollOptions = "Disappearing Photos,Voice-to-text notes,Shared Lists,Encrypted video calls",
                    pollVotes = "3,1,0,5"
                ),
                // Insert a mock task list
                ChatMessage(
                    chatId = "group_dev_core",
                    senderId = "ethan_carter",
                    senderName = "Ethan Carter",
                    senderAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    content = "Sprint backlog tasks:",
                    mediaType = "task_list",
                    taskItems = "Review cryptography protocol:true,Implement real-time database cache:true,Optimize Compose recompositions:false,Polishing micro-interactions:false"
                )
            )

            for (m in msgs) {
                chatDao.insertMessage(m)
            }

            // Seed direct message history with Sarah Connor
            val sarahMsgs = listOf(
                ChatMessage(
                    chatId = "sarah_connor",
                    senderId = "sarah_connor",
                    senderName = "Sarah Connor",
                    senderAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    content = "Hey Alex, I've analyzed our E2E handshake. It is resilient. Any external server syncing is fully randomized and encrypted.",
                    isEncrypted = true,
                    encryptionKey = "velo_sec_k98s_2f"
                ),
                ChatMessage(
                    chatId = "sarah_connor",
                    senderId = "me",
                    senderName = "Alex",
                    senderAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    content = "Perfect. The local sqlite cipher keeps it extremely fast on devices. No network lag.",
                    isEncrypted = true,
                    encryptionKey = "velo_sec_k98s_2f"
                ),
                ChatMessage(
                    chatId = "sarah_connor",
                    senderId = "sarah_connor",
                    senderName = "Sarah Connor",
                    senderAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    content = "I sent you a security protocol document, check it out. (Disappearing media below)",
                    isEncrypted = true,
                    encryptionKey = "velo_sec_k98s_2f"
                ),
                ChatMessage(
                    chatId = "sarah_connor",
                    senderId = "sarah_connor",
                    senderName = "Sarah Connor",
                    senderAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    content = "Tap to open disappearing security diagram",
                    mediaUri = "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=300",
                    mediaType = "image",
                    isDisappearing = true,
                    disappearDuration = 10,
                    isEncrypted = true,
                    encryptionKey = "velo_sec_k98s_2f"
                )
            )
            for (m in sarahMsgs) {
                chatDao.insertMessage(m)
            }
        }
    }

    /**
     * Calls Gemini API to get a summary of recent messages in a chat.
     */
    suspend fun getChatSummary(chatName: String, messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Fallback mock summary with professional instruction
            return@withContext "💡 *VeloChat AI Sync Summary for '$chatName'*\n\n" +
                    "**1. Core Discussion:** Team verified local SQLite SQLite DB speeds and E2E encryption keys.\n" +
                    "**2. Group Status:** Jordan is highly excited about instantaneous delivery. Chloe finalized dark theme obsidian gradients.\n" +
                    "**3. Shared Tasks:** Security audit is complete; Compose optimizations are currently underway.\n\n" +
                    "*(To use live AI summaries powered by Google Gemini, enter your actual Gemini API Key in the AI Studio Secrets Panel as GEMINI_API_KEY)*"
        }

        // Format message transcripts for the prompt
        val transcript = messages.takeLast(15).joinToString("\n") { m ->
            "${m.senderName}: ${m.content} [Type: ${m.mediaType ?: "text"}]"
        }

        val prompt = """
            You are the VeloChat AI Summary assistant. Summarize the following high-speed encrypted messaging chat transcript for the room/chat named '$chatName'.
            Keep the summary highly professional, modern, scannable, and clean. Highlight key discussion points, and tasks/polls if any.
            Limit the summary to 3-4 bullet points and keep it concise.
            
            Transcript:
            $transcript
            
            Summary:
        """.trimIndent()

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "⚠️ AI Summary temporary unavailable (HTTP ${response.code}). Check your internet connection or API Key."
                }
                val bodyString = response.body?.string() ?: return@withContext "⚠️ Empty response from AI service."
                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = contentObj.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text")
                    }
                }
                "⚠️ Failed to parse summary output."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "⚠️ AI Summary Network Error: ${e.localizedMessage ?: "Unknown error"}. Ensure your device can connect to the internet."
        }
    }
}
