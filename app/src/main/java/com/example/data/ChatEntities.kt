package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_users")
data class ChatUser(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String,
    val statusText: String,
    val encryptionKey: String,
    val isOnline: Boolean,
    val lastActive: Long = System.currentTimeMillis(),
    val customBadge: String? = null,
    val activeBorderColor: String? = null
)

@Entity(tableName = "chat_groups")
data class ChatGroup(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String,
    val description: String,
    val createdBy: String,
    val memberIds: String, // Comma separated user IDs
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: String, // Can be user.id or group.id
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = true,
    val encryptionKey: String = "",
    val mediaUri: String? = null,
    val mediaType: String? = null, // "image", "video", "voice", "poll", "event_planner", "task_list"
    val isDisappearing: Boolean = false,
    val disappearDuration: Long = 0, // e.g. 5, 10, 30 seconds
    val disappeared: Boolean = false,
    val voiceDuration: Int = 0, // in seconds
    val voiceTranscription: String? = null,
    val pollOptions: String? = null, // comma separated
    val pollVotes: String? = null, // index-based count: e.g. "0,2,1"
    val eventTitle: String? = null,
    val eventTime: String? = null,
    val eventRsvps: String? = null, // format "user1:YES,user2:NO"
    val taskItems: String? = null, // format "Task1:true,Task2:false"
    val replyToId: Int? = null,
    val replyToSender: String? = null,
    val replyToContent: String? = null
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "me",
    val name: String = "Alex",
    val avatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
    val statusText: String = "Building VeloChat! ⚡",
    val activeBorderColor: String = "#00FFCC", // Electric cyan
    val customBadge: String = "⚡ Founder"
)
