package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatAppUI(
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val videoCallState by viewModel.videoCallState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Main Screen Router
        Crossfade(
            targetState = currentScreen,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                Screen.CHATS_LIST -> ChatsListScreen(viewModel)
                Screen.ACTIVE_CHAT -> ActiveChatScreen(viewModel)
                Screen.PROFILE_EDIT -> ProfileEditScreen(viewModel)
            }
        }

        // Full Screen Video Call Overlay
        AnimatedVisibility(
            visible = videoCallState.isActive,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            VideoCallOverlay(viewModel, videoCallState)
        }
    }
}

@Composable
fun ChatsListScreen(viewModel: ChatViewModel) {
    val users by viewModel.users.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showCreateGroupSheet by remember { mutableStateOf(false) }

    val filteredUsers = users.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val filteredGroups = groups.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            MainHeader(profile) {
                viewModel.navigateTo(Screen.PROFILE_EDIT)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateGroupSheet = true },
                containerColor = NeonCyan,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.GroupAdd, contentDescription = "New Group") },
                text = { Text("New Group", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("create_group_fab")
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live synchronization / speed tracker banner
            LatencyBar()

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_bar"),
                placeholder = { Text("Search 90+ encrypted channels...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Online Quick Status Horizontal Row
            Text(
                text = "⚡ ACTIVE CHANNELS (75-150 PEERS)",
                fontSize = 11.sp,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
                letterSpacing = 1.sp
            )

            HorizontalOnlineRow(users.filter { it.isOnline }) { user ->
                viewModel.selectChat(
                    id = user.id,
                    name = user.name,
                    avatar = user.avatarUrl,
                    isGroup = false,
                    encKey = user.encryptionKey
                )
            }

            Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))

            // Unified list of Chats and Groups
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (filteredGroups.isNotEmpty()) {
                    item {
                        Text(
                            text = "COLLABORATIVE GROUPS",
                            fontSize = 11.sp,
                            color = CyberPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    items(filteredGroups) { group ->
                        GroupChatItem(group) {
                            viewModel.selectChat(
                                id = group.id,
                                name = group.name,
                                avatar = group.avatarUrl,
                                isGroup = true,
                                encKey = "velo_sec_group_aes256"
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "E2E DIRECT CONVERSATIONS",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                        letterSpacing = 1.sp
                    )
                }

                if (filteredUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LockOpen, contentDescription = "No users", tint = TextSecondary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No private peers found", color = TextSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    items(filteredUsers) { user ->
                        DirectChatItem(user) {
                            viewModel.selectChat(
                                id = user.id,
                                name = user.name,
                                avatar = user.avatarUrl,
                                isGroup = false,
                                encKey = user.encryptionKey
                            )
                        }
                    }
                }
            }
        }
    }

    // New Group Sheet
    if (showCreateGroupSheet) {
        CreateGroupDialog(
            users = users,
            onDismiss = { showCreateGroupSheet = false },
            onCreate = { name, desc, members ->
                viewModel.createGroupChat(name, desc, members)
                showCreateGroupSheet = false
            }
        )
    }
}

@Composable
fun LatencyBar() {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(pulseAlpha)
                .background(E2EGreen, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "E2E LOCAL SYNC: LIVE (0.1ms CACHE LATENCY) ● 100% PRIVATE",
            fontSize = 9.sp,
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun MainHeader(profile: UserProfile, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VeloChat",
                    fontSize = 24.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "⚡",
                    fontSize = 22.sp,
                    color = NeonCyan
                )
            }
            Text(
                text = "Secure Private Messenger",
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        // Clickable Profile Button
        Surface(
            onClick = onProfileClick,
            shape = RoundedCornerShape(12.dp),
            color = SurfaceDark,
            border = BorderStroke(1.5.dp, Color(android.graphics.Color.parseColor(profile.activeBorderColor))),
            modifier = Modifier
                .size(44.dp)
                .testTag("my_profile_header_btn")
        ) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = "My Profile",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun HorizontalOnlineRow(onlineUsers: List<ChatUser>, onUserClick: (ChatUser) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(onlineUsers.take(15)) { user ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onUserClick(user) }
                    .width(60.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    // Profile Image with Custom Glowing Border
                    val borderColor = user.activeBorderColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: NeonCyan
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .border(2.dp, borderColor, CircleShape)
                    ) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = user.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Green Active Dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(E2EGreen, CircleShape)
                            .border(1.5.dp, ObsidianBg, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.name.split(" ").first(),
                    fontSize = 10.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DirectChatItem(user: ChatUser, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            val borderColor = user.activeBorderColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: BorderColor
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, borderColor, CircleShape)
            ) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = user.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (user.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(E2EGreen, CircleShape)
                        .border(2.dp, ObsidianBg, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.name,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (user.customBadge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = user.customBadge,
                        fontSize = 9.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .background(SurfaceLightDark, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = user.statusText,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Icon(Icons.Default.Lock, contentDescription = "Encrypted", tint = E2EGreen.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Key: ${user.encryptionKey.take(6)}...",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun GroupChatItem(group: ChatGroup, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, CyberPurple, RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = group.avatarUrl,
                contentDescription = group.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GROUP",
                    fontSize = 8.sp,
                    color = CyberPurple,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .background(CyberPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = group.description,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Icon(Icons.Default.Hub, contentDescription = "Collaborative", tint = CyberPurple.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            val memberCount = group.memberIds.split(",").size
            Text(
                text = "$memberCount peers",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.activeMessages.collectAsState()
    val chatName by viewModel.activeChatName.collectAsState()
    val chatAvatar by viewModel.activeChatAvatar.collectAsState()
    val isGroup by viewModel.activeChatIsGroup.collectAsState()
    val encKey by viewModel.activeChatEncryptionKey.collectAsState()
    val aiSummary by viewModel.aiSummary.collectAsState()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsState()
    val currentReply by viewModel.replyToMessage.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    var showAttachmentsMenu by remember { mutableStateOf(false) }
    var showCreatePollDialog by remember { mutableStateOf(false) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var showCreateTasksDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(if (isGroup) RoundedCornerShape(10.dp) else CircleShape)
                                .border(1.dp, if (isGroup) CyberPurple else NeonCyan, if (isGroup) RoundedCornerShape(10.dp) else CircleShape)
                        ) {
                            AsyncImage(
                                model = chatAvatar,
                                contentDescription = chatName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(chatName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = "E2E", tint = E2EGreen, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "E2E ECRYPTED | KEY: ${encKey.take(12)}",
                                    fontSize = 9.sp,
                                    color = E2EGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.deselectChat() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // AI Summary Trigger Button
                    IconButton(
                        onClick = { viewModel.generateChatSummary() },
                        modifier = Modifier.testTag("ai_summary_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Summary", tint = NeonCyan)
                    }

                    // Encrypted Video Call Launcher
                    IconButton(
                        onClick = { viewModel.startVideoCall() },
                        modifier = Modifier.testTag("video_call_button")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = CyberPurple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            Column {
                // Reply Preview Panel
                AnimatedVisibility(
                    visible = currentReply != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    currentReply?.let { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark)
                                .border(BorderStroke(1.dp, BorderColor))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = "Replying", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Replying to ${msg.senderName}", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                Text(msg.content, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { viewModel.setReplyTo(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // AI Summary Result Panel
                AnimatedVisibility(
                    visible = aiSummary != null || isGeneratingSummary,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .border(1.5.dp, NeonCyan, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLightDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Summary", tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("VELOCHAT AI CONTEXT SYNC", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 12.sp)
                                }
                                IconButton(onClick = { viewModel.clearSummary() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isGeneratingSummary) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Synthesizing E2E transcripts via Gemini...", color = TextSecondary, fontSize = 13.sp)
                                }
                            } else {
                                Text(
                                    text = aiSummary ?: "",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Bottom Chat Input Bar
                ChatInputBar(
                    viewModel = viewModel,
                    onAttachClick = { showAttachmentsMenu = !showAttachmentsMenu }
                )
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        onVote = { optionIdx -> viewModel.voteOnPoll(message, optionIdx) },
                        onRsvp = { status -> viewModel.rsvpToEvent(message, "Alex", status) },
                        onToggleTask = { taskIdx -> viewModel.toggleTaskItem(message, taskIdx) },
                        onSwipeToReply = { viewModel.setReplyTo(message) },
                        onMediaViewTrigger = { viewModel.triggerDisappearingMessageCountdown(message) }
                    )
                }
            }

            // Quick Attachments Panel
            AnimatedVisibility(
                visible = showAttachmentsMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("attachments_panel"),
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.5.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("COLLABORATIVE GROUP TOOLS", color = CyberPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            AttachmentOptionButton(Icons.Default.Poll, "Create Poll", CyberPurple) {
                                showCreatePollDialog = true
                                showAttachmentsMenu = false
                            }
                            AttachmentOptionButton(Icons.Default.Event, "Plan Event", ElectricBlue) {
                                showCreateEventDialog = true
                                showAttachmentsMenu = false
                            }
                            AttachmentOptionButton(Icons.Default.ListAlt, "Group Tasks", NeonCyan) {
                                showCreateTasksDialog = true
                                showAttachmentsMenu = false
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("DISAPPEARING MEDIA", color = NeonPink, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            AttachmentOptionButton(Icons.Default.Image, "Disappear Photo", NeonPink) {
                                viewModel.disappearingDurationSelected.value = 10L // auto disappear in 10s
                                viewModel.sendMessage(
                                    mediaUri = "https://images.unsplash.com/photo-1579202673506-ca3ce28943ef?w=400",
                                    mediaType = "image"
                                )
                                showAttachmentsMenu = false
                            }
                            AttachmentOptionButton(Icons.Default.VideoCameraBack, "Disappear Video", SafetyYellow) {
                                viewModel.disappearingDurationSelected.value = 5L // auto disappear in 5s
                                viewModel.sendMessage(
                                    mediaUri = "https://assets.mixkit.co/videos/preview/mixkit-starry-night-sky-and-milky-way-4091-large.mp4",
                                    mediaType = "video"
                                )
                                showAttachmentsMenu = false
                            }
                            AttachmentOptionButton(Icons.Default.AttachFile, "Secure File", TextSecondary) {
                                viewModel.sendMessage(
                                    mediaUri = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400",
                                    mediaType = "image"
                                )
                                showAttachmentsMenu = false
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreatePollDialog) {
        CreatePollDialog(
            onDismiss = { showCreatePollDialog = false },
            onCreate = { question, options ->
                viewModel.createGroupPoll(question, options)
                showCreatePollDialog = false
            }
        )
    }

    if (showCreateEventDialog) {
        CreateEventDialog(
            onDismiss = { showCreateEventDialog = false },
            onCreate = { title, time ->
                viewModel.createGroupEvent(title, time)
                showCreateEventDialog = false
            }
        )
    }

    if (showCreateTasksDialog) {
        CreateTasksDialog(
            onDismiss = { showCreateTasksDialog = false },
            onCreate = { tasks ->
                viewModel.createGroupTaskList(tasks)
                showCreateTasksDialog = false
            }
        )
    }
}

@Composable
fun AttachmentOptionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(color.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onVote: (Int) -> Unit,
    onRsvp: (String) -> Unit,
    onToggleTask: (Int) -> Unit,
    onSwipeToReply: () -> Unit,
    onMediaViewTrigger: () -> Unit
) {
    val isMe = message.senderId == "me"

    // Gesture swipe detection for replying
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "swipe")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX < -120f) {
                            onSwipeToReply()
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        // Only swipe left to reply
                        if (dragAmount < 0 || offsetX < 0) {
                            offsetX = (offsetX + dragAmount).coerceIn(-150f, 0f)
                        }
                    }
                )
            }
            .offset(x = animatedOffset.dp)
            .padding(vertical = 4.dp),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isMe) {
                // Sender Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .align(Alignment.Top)
                ) {
                    AsyncImage(
                        model = message.senderAvatar,
                        contentDescription = message.senderName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message Box
            Column(
                modifier = Modifier.widthIn(max = 280.dp),
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                if (!isMe) {
                    Text(message.senderName, fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 2.dp,
                        bottomEnd = if (isMe) 2.dp else 16.dp
                    ),
                    color = if (isMe) SurfaceLightDark else SurfaceDark,
                    border = BorderStroke(
                        1.dp,
                        when {
                            message.isDisappearing -> NeonPink
                            message.mediaType == "poll" -> CyberPurple
                            message.mediaType == "event_planner" -> ElectricBlue
                            message.mediaType == "task_list" -> NeonCyan
                            else -> BorderColor
                        }
                    ),
                    modifier = Modifier.testTag("message_bubble")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Reply block header if replied
                        if (message.replyToSender != null) {
                            Row(
                                modifier = Modifier
                                    .background(ObsidianBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(0.5.dp, BorderColor), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .fillMaxWidth()
                            ) {
                                Column {
                                    Text("↵ Reply to ${message.replyToSender}", fontSize = 9.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                    Text(message.replyToContent ?: "", fontSize = 10.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Disappearing Media or standard media
                        if (message.mediaUri != null) {
                            if (message.isDisappearing) {
                                DisappearingMediaComponent(message, onMediaViewTrigger)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = message.mediaUri,
                                        contentDescription = "Media File",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Custom content templates (Poll, Event, Task List, Voice note)
                        when (message.mediaType) {
                            "poll" -> PollComponent(message, onVote)
                            "event_planner" -> EventPlannerComponent(message, onRsvp)
                            "task_list" -> TaskListComponent(message, onToggleTask)
                            "voice" -> VoiceNoteComponent(message)
                            else -> {
                                if (message.content.isNotEmpty()) {
                                    Text(message.content, fontSize = 14.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisappearingMediaComponent(message: ChatMessage, onViewTrigger: () -> Unit) {
    var viewed by remember { mutableStateOf(false) }
    var countdownActive by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(message.disappearDuration) }

    LaunchedEffect(countdownActive) {
        if (countdownActive) {
            onViewTrigger()
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
            viewed = true
        }
    }

    if (message.disappeared || viewed) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, NeonPink, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Burned", tint = NeonPink, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Media securely disappeared", color = NeonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    } else {
        if (!countdownActive) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { countdownActive = true }
                    .background(ObsidianBg, RoundedCornerShape(12.dp))
                    .border(1.5.dp, NeonPink, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.VisibilityOff, contentDescription = "Disappearing", tint = NeonPink, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("TAP TO REVEAL MEDIA", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Self-destructs in ${message.disappearDuration}s after viewing", color = NeonPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, NeonPink, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = message.mediaUri,
                        contentDescription = "Disappearing Media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentScale = ContentScale.Crop
                    )

                    // Countdown Floating Timer Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(ObsidianBg.copy(alpha = 0.8f), CircleShape)
                            .border(1.dp, NeonPink, CircleShape)
                            .padding(8.dp)
                    ) {
                        Text("${secondsLeft}s", color = NeonPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PollComponent(message: ChatMessage, onVote: (Int) -> Unit) {
    val options = message.pollOptions?.split(",") ?: emptyList()
    val votes = message.pollVotes?.split(",")?.map { it.toInt() } ?: List(options.size) { 0 }
    val totalVotes = votes.sum().coerceAtLeast(1)

    Column {
        Text(
            text = "📊 GROUP POLL: ${message.content}",
            fontWeight = FontWeight.Bold,
            color = CyberPurple,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        options.forEachIndexed { idx, opt ->
            val voteCount = votes.getOrElse(idx) { 0 }
            val progress = voteCount.toFloat() / totalVotes.toFloat()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onVote(idx) },
                colors = CardDefaults.cardColors(containerColor = ObsidianBg),
                border = BorderStroke(0.5.dp, BorderColor)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Custom Progress Bar background
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(CyberPurple.copy(alpha = 0.2f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(opt, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("$voteCount votes (${(progress * 100).toInt()}%)", color = CyberPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EventPlannerComponent(message: ChatMessage, onRsvp: (String) -> Unit) {
    val rsvps = message.eventRsvps?.split(",") ?: emptyList()
    val yesCount = rsvps.count { it.endsWith(":YES") }
    val maybeCount = rsvps.count { it.endsWith(":MAYBE") }

    Column {
        Text(
            text = "📅 EVENT SCHEDULED",
            fontWeight = FontWeight.Bold,
            color = ElectricBlue,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(message.eventTitle ?: "", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("🕒 ${message.eventTime ?: ""}", color = TextSecondary, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(8.dp))
        Text("RSVPs: $yesCount Yes | $maybeCount Maybe", fontSize = 11.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { onRsvp("YES") },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Text("Join", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { onRsvp("MAYBE") },
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("Maybe", color = TextPrimary, fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { onRsvp("NO") },
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Text("Decline", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun TaskListComponent(message: ChatMessage, onToggleTask: (Int) -> Unit) {
    val items = message.taskItems?.split(",") ?: emptyList()

    Column {
        Text(
            text = message.content,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        items.forEachIndexed { idx, item ->
            val parts = item.split(":")
            if (parts.size >= 2) {
                val name = parts[0]
                val checked = parts[1].toBoolean()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTask(idx) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleTask(idx) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NeonCyan,
                            checkmarkColor = Color.Black,
                            uncheckedColor = BorderColor
                        )
                    )
                    Text(
                        text = name,
                        color = if (checked) TextSecondary else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceNoteComponent(message: ChatMessage) {
    var isPlaying by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { isPlaying = !isPlaying }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = "Play",
                    tint = NeonCyan,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Simulated voice wave visualizer
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (i in 1..24) {
                    val h = if (isPlaying) (4..24).random() else 8
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(h.dp)
                            .background(if (isPlaying) NeonCyan else TextSecondary, RoundedCornerShape(1.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("${message.voiceDuration}s", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        // Integrated dynamic Speech-to-Text Transcription Card!
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianBg),
            border = BorderStroke(0.5.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hearing, contentDescription = "Transcript", tint = NeonCyan, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SPEECH-TO-TEXT VOICE TYPING TRANSCRIPT", fontSize = 8.sp, color = NeonCyan, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.voiceTranscription ?: "Analyzing audio package...",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(viewModel: ChatViewModel, onAttachClick: () -> Unit) {
    val input by viewModel.inputText.collectAsState()
    val isRecording by viewModel.isVoiceRecording.collectAsState()
    val recordSeconds by viewModel.voiceRecordDuration.collectAsState()
    val recordWaves by viewModel.voiceWaves.collectAsState()
    val selectedDisappearing by viewModel.disappearingDurationSelected.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Toggle attachments (collaborative lists, disappearing photos, etc)
        IconButton(
            onClick = onAttachClick,
            modifier = Modifier.testTag("attach_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Attach", tint = ElectricBlue)
        }

        // Toggle Disappearing countdown timer limit
        IconButton(
            onClick = {
                // cycle durations: 0 -> 5s -> 10s -> 30s -> 0
                val next = when (selectedDisappearing) {
                    0L -> 5L
                    5L -> 10L
                    10L -> 30L
                    else -> 0L
                }
                viewModel.disappearingDurationSelected.value = next
            },
            modifier = Modifier.testTag("disappear_toggle")
        ) {
            Icon(
                imageVector = if (selectedDisappearing > 0L) Icons.Default.Timer else Icons.Outlined.Timer,
                contentDescription = "Disappearing timer",
                tint = if (selectedDisappearing > 0L) NeonPink else TextSecondary
            )
        }

        if (selectedDisappearing > 0L) {
            Text(
                text = "${selectedDisappearing}s",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonPink,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        if (isRecording) {
            // Recording Wave UI
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(ObsidianBg, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NeonPink, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RECORDING ${recordSeconds}s",
                    color = NeonPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Animated sound waves
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    recordWaves.take(15).forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height((h / 4).dp)
                                .background(NeonPink, RoundedCornerShape(1.dp))
                        )
                    }
                }
                IconButton(onClick = { viewModel.cancelVoiceRecording() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = TextSecondary)
                }
                IconButton(onClick = { viewModel.stopAndSendVoiceRecording() }) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = NeonCyan)
                }
            }
        } else {
            // Normal Text Input Box
            OutlinedTextField(
                value = input,
                onValueChange = { viewModel.inputText.value = it },
                placeholder = { Text("Encrypted message...", color = TextSecondary, fontSize = 14.sp) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = ObsidianBg,
                    unfocusedContainerColor = ObsidianBg,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                singleLine = false,
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (input.isBlank()) {
                // Record Voice Note Button
                IconButton(
                    onClick = { viewModel.startVoiceRecording() },
                    modifier = Modifier
                        .testTag("voice_record_btn")
                        .background(CyberPurple.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice typing notes", tint = CyberPurple)
                }
            } else {
                // Send Message Button
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    modifier = Modifier
                        .testTag("send_btn")
                        .background(ElectricBlue.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send message", tint = ElectricBlue)
                }
            }
        }
    }
}

@Composable
fun ProfileEditScreen(viewModel: ChatViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    var nameInput by remember { mutableStateOf(profile.name) }
    var statusInput by remember { mutableStateOf(profile.statusText) }
    var badgeInput by remember { mutableStateOf(profile.customBadge) }
    var colorSelected by remember { mutableStateOf(profile.activeBorderColor) }

    val presetColors = listOf("#00FFCC", "#00B2FF", "#B55DFF", "#FF2E93", "#FFFF33", "#10B981")

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Account Customization", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CHATS_LIST) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Card Preview
            Text("LIVE CHAT CARD PREVIEW", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(2.dp, Color(android.graphics.Color.parseColor(colorSelected)))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(android.graphics.Color.parseColor(colorSelected)), CircleShape)
                    ) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Avatar preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(nameInput, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            if (badgeInput.isNotEmpty()) {
                                Text(
                                    badgeInput,
                                    fontSize = 9.sp,
                                    color = Color(android.graphics.Color.parseColor(colorSelected)),
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier
                                        .background(SurfaceLightDark, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(statusInput, color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Display Name", color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("profile_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = statusInput,
                onValueChange = { statusInput = it },
                label = { Text("Custom status text", color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = badgeInput,
                onValueChange = { badgeInput = it },
                label = { Text("Role Badge (e.g. Beta, Founder, Pro)", color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("SELECT GLOW BORDER COLOR", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))

            // Preset Colors Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                presetColors.forEach { cStr ->
                    val color = Color(android.graphics.Color.parseColor(cStr))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                3.dp,
                                if (colorSelected == cStr) TextPrimary else Color.Transparent,
                                CircleShape
                            )
                            .clickable { colorSelected = cStr }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.updateProfile(nameInput, statusInput, badgeInput, colorSelected)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("SAVE PROFILE CHANGES", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun VideoCallOverlay(viewModel: ChatViewModel, state: VideoCallState) {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main call area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Encryption padlock call title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = "Secured", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("E2E SECURED VIDEO CALL", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Handshake key: ${state.connectionSecureKey}",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Central Call Screen Visualizer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .drawBehind {
                        // draw pulsing E2E circles
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.15f),
                            radius = size.minDimension / 2 * waveScale,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = CyberPurple.copy(alpha = 0.1f),
                            radius = size.minDimension / 1.7f * waveScale,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
            ) {
                if (state.isCameraOff) {
                    // Muted placeholder avatar
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .border(3.dp, CyberPurple, CircleShape)
                    ) {
                        AsyncImage(
                            model = state.participantAvatar,
                            contentDescription = state.participantName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // Simulated visual stream representing Sarah Connor
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(2.5.dp, NeonCyan, RoundedCornerShape(24.dp))
                    ) {
                        AsyncImage(
                            model = state.participantAvatar,
                            contentDescription = state.participantName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sarah Connor (Live)", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Participant info & Timer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.participantName, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val minutes = state.durationSeconds / 60
                val seconds = state.durationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    color = NeonCyan,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Call Action controls bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute toggle
                IconButton(
                    onClick = { viewModel.toggleMuteCall() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(if (state.isMuted) Color.Red.copy(alpha = 0.2f) else SurfaceDark, CircleShape)
                        .border(1.dp, if (state.isMuted) Color.Red else BorderColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (state.isMuted) Color.Red else TextPrimary
                    )
                }

                // Shutter Camera toggle
                IconButton(
                    onClick = { viewModel.toggleCameraCall() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(if (state.isCameraOff) Color.Red.copy(alpha = 0.2f) else SurfaceDark, CircleShape)
                        .border(1.dp, if (state.isCameraOff) Color.Red else BorderColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (state.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "Camera Shutter",
                        tint = if (state.isCameraOff) Color.Red else TextPrimary
                    )
                }

                // Red End Call Button
                IconButton(
                    onClick = { viewModel.endVideoCall() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Red, CircleShape)
                        .testTag("end_call_btn")
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

// Dialog Creators for attachments & groups

@Composable
fun CreateGroupDialog(
    users: List<ChatUser>,
    onDismiss: () -> Unit,
    onCreate: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedPeers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assemble Private Group", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Title", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberPurple
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Topic Description", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberPurple
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("SELECT GROUP PEERS (75-150 MAX)", fontSize = 11.sp, color = CyberPurple, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                users.take(15).forEach { user ->
                    val isChecked = selectedPeers.contains(user.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedPeers.remove(user.id) else selectedPeers.add(user.id)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked == true) selectedPeers.add(user.id) else selectedPeers.remove(user.id)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = CyberPurple)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(user.name, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotEmpty()) onCreate(name, description, selectedPeers.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
            ) {
                Text("Broadcast Group", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun CreatePollDialog(onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    var question by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var opt3 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deploy Collaborative Poll", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Poll Question", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = CyberPurple)
                )
                OutlinedTextField(
                    value = opt1,
                    onValueChange = { opt1 = it },
                    label = { Text("Option A", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = CyberPurple)
                )
                OutlinedTextField(
                    value = opt2,
                    onValueChange = { opt2 = it },
                    label = { Text("Option B", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = CyberPurple)
                )
                OutlinedTextField(
                    value = opt3,
                    onValueChange = { opt3 = it },
                    label = { Text("Option C (Optional)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = CyberPurple)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val opts = listOf(opt1, opt2, opt3).filter { it.isNotEmpty() }
                    if (question.isNotEmpty() && opts.size >= 2) {
                        onCreate(question, opts)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
            ) {
                Text("Deploy Poll", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun CreateEventDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan Joint Hangout", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Hangout / Event Title", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ElectricBlue)
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Schedule Time (e.g. Tonight 8 PM)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = ElectricBlue)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotEmpty() && time.isNotEmpty()) onCreate(title, time) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Publish Event", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun CreateTasksDialog(onDismiss: () -> Unit, onCreate: (List<String>) -> Unit) {
    var t1 by remember { mutableStateOf("") }
    var t2 by remember { mutableStateOf("") }
    var t3 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spawn Shared Tasks", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = t1,
                    onValueChange = { t1 = it },
                    label = { Text("Task Checklist 1", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = NeonCyan)
                )
                OutlinedTextField(
                    value = t2,
                    onValueChange = { t2 = it },
                    label = { Text("Task Checklist 2", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = NeonCyan)
                )
                OutlinedTextField(
                    value = t3,
                    onValueChange = { t3 = it },
                    label = { Text("Task Checklist 3 (Optional)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = NeonCyan)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val list = listOf(t1, t2, t3).filter { it.isNotEmpty() }
                    if (list.isNotEmpty()) onCreate(list)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Spawn List", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceDark
    )
}
