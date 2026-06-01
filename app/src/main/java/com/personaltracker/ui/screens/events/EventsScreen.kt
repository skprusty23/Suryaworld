package com.personaltracker.ui.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.EventEntity
import com.personaltracker.domain.repository.EventRepository
import com.personaltracker.ui.components.PTTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// State & ViewModel
// ──────────────────────────────────────────────────────────────────────────────

data class EventsState(
    val upcomingEvents: List<EventEntity> = emptyList(),
    val allEvents: List<EventEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val showAll: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventsState())
    val state: StateFlow<EventsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val today = LocalDate.now()
            val future = today.plusDays(30)
            repository.getUpcomingEvents(today, future).collect { events ->
                _state.update { it.copy(upcomingEvents = events, isLoading = false) }
            }
        }
        viewModelScope.launch {
            repository.getAllActiveEvents().collect { events ->
                _state.update { it.copy(allEvents = events) }
            }
        }
        viewModelScope.launch {
            repository.getAllCategories().collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }
    }

    fun setShowAll(v: Boolean) = _state.update { it.copy(showAll = v) }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            _state.update { it.copy(message = "Event deleted") }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

// ──────────────────────────────────────────────────────────────────────────────
// Events List Screen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun EventsScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var eventToDelete by remember { mutableStateOf<EventEntity?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    Scaffold(
        topBar = { PTTopBar(title = "Events & Reminders", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, contentDescription = "Add Event", tint = Color.White) }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Toggle chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !state.showAll,
                    onClick = { viewModel.setShowAll(false) },
                    label = { Text("Upcoming (30 days)") },
                    leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = state.showAll,
                    onClick = { viewModel.setShowAll(true) },
                    label = { Text("All Events") },
                    leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) }
                )
            }

            val displayList = if (state.showAll) state.allEvents else state.upcomingEvents

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DateRange, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (state.showAll) "No events yet" else "No upcoming events in the next 30 days",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Tap + to add an event",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayList, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            onClick = { onNavigateToDetail(event.id) },
                            onDelete = { eventToDelete = event }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Event?") },
            text = { Text("\"${event.title}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteEvent(event); eventToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { eventToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun EventCard(event: EventEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val today = LocalDate.now()
    val daysUntil = today.until(event.eventDate).days
    val priorityColor = when (event.priority) {
        "HIGH"   -> Color(0xFFE53935)
        "LOW"    -> Color(0xFF43A047)
        else     -> Color(0xFFFFA000)
    }
    val isOverdue = event.eventDate.isBefore(today)
    val isToday   = event.eventDate == today

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                isToday   -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                else      -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            // Date badge
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = event.eventDate.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = event.eventDate.month.name.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Priority dot
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(priorityColor))
                    Spacer(Modifier.width(6.dp))
                    Text(event.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                if (event.description.isNotBlank()) {
                    Text(event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    SuggestionChip(onClick = {}, label = { Text(event.category, style = MaterialTheme.typography.labelSmall) })
                    if (event.repeatType != "NONE") {
                        Icon(Icons.Default.Repeat, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Text(event.repeatType.lowercase().replaceFirstChar { it.uppercaseChar() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val label = when {
                    isOverdue -> "Overdue"
                    isToday   -> "Today"
                    daysUntil == 1L -> "Tomorrow"
                    else -> "In ${daysUntil}d"
                }
                val labelColor = when {
                    isOverdue -> MaterialTheme.colorScheme.error
                    isToday   -> Color(0xFFE65100)
                    else      -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor, fontWeight = FontWeight.SemiBold)
                if (event.reminderDate != null) {
                    Icon(Icons.Default.NotificationsActive, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Add / Edit Event Screen
// ──────────────────────────────────────────────────────────────────────────────

data class AddEventState(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val eventDate: LocalDate = LocalDate.now().plusDays(1),
    val hasReminder: Boolean = false,
    val reminderDate: LocalDate? = null,
    val category: String = "General",
    val priority: String = "MEDIUM",
    val repeatType: String = "NONE",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val titleError: String? = null
)

@HiltViewModel
class AddEventViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEventState())
    val state: StateFlow<AddEventState> = _state.asStateFlow()

    fun loadEvent(id: Long) {
        viewModelScope.launch {
            repository.getEventById(id)?.let { e ->
                _state.update {
                    it.copy(
                        id = e.id, title = e.title, description = e.description,
                        eventDate = e.eventDate, hasReminder = e.reminderDate != null,
                        reminderDate = e.reminderDate, category = e.category,
                        priority = e.priority, repeatType = e.repeatType
                    )
                }
            }
        }
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v, titleError = null) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setEventDate(v: LocalDate) = _state.update { it.copy(eventDate = v) }
    fun setHasReminder(v: Boolean) = _state.update { it.copy(hasReminder = v, reminderDate = if (v) it.eventDate.minusDays(1) else null) }
    fun setReminderDate(v: LocalDate) = _state.update { it.copy(reminderDate = v) }
    fun setCategory(v: String) = _state.update { it.copy(category = v) }
    fun setPriority(v: String) = _state.update { it.copy(priority = v) }
    fun setRepeatType(v: String) = _state.update { it.copy(repeatType = v) }

    fun save(context: android.content.Context) {
        val s = _state.value
        if (s.title.isBlank()) { _state.update { it.copy(titleError = "Title is required") }; return }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val entity = EventEntity(
                id = s.id,
                title = s.title.trim(),
                description = s.description.trim(),
                eventDate = s.eventDate,
                reminderDate = if (s.hasReminder) s.reminderDate else null,
                category = s.category.ifBlank { "General" },
                priority = s.priority,
                repeatType = s.repeatType,
                isActive = true
            )
            val savedId = if (s.id == 0L) repository.insertEvent(entity) else { repository.updateEvent(entity); s.id }
            val savedEntity = entity.copy(id = savedId)
            // Schedule reminder if set
            if (s.hasReminder) {
                EventReminderScheduler.createNotificationChannel(context)
                EventReminderScheduler.scheduleReminder(context, savedEntity)
            } else {
                EventReminderScheduler.cancelReminder(context, savedId)
            }
            _state.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    eventId: Long = 0L,
    onBack: () -> Unit,
    viewModel: AddEventViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var repeatMenuExpanded by remember { mutableStateOf(false) }

    // Simple date state for pickers
    var showEventDatePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) { if (eventId != 0L) viewModel.loadEvent(eventId) }
    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }

    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    if (showEventDatePicker) {
        SimpleDatePickerDialog(
            initialDate = state.eventDate,
            onDismiss = { showEventDatePicker = false },
            onDateSelected = { viewModel.setEventDate(it); showEventDatePicker = false }
        )
    }
    if (showReminderDatePicker) {
        SimpleDatePickerDialog(
            initialDate = state.reminderDate ?: state.eventDate.minusDays(1),
            onDismiss = { showReminderDatePicker = false },
            onDateSelected = { viewModel.setReminderDate(it); showReminderDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            PTTopBar(
                title = if (eventId == 0L) "Add Event" else "Edit Event",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { viewModel.save(context) }, enabled = !state.isSaving) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.title, onValueChange = { viewModel.setTitle(it) },
                    label = { Text("Event Title *") },
                    isError = state.titleError != null,
                    supportingText = state.titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.description, onValueChange = { viewModel.setDescription(it) },
                    label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 3
                )
            }
            // Event date
            item {
                OutlinedTextField(
                    value = state.eventDate.format(dateFmt), onValueChange = {},
                    label = { Text("Event Date") }, readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showEventDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Category
            item {
                Box {
                    OutlinedTextField(
                        value = state.category, onValueChange = { viewModel.setCategory(it) },
                        label = { Text("Category") }, singleLine = true,
                        trailingIcon = { IconButton(onClick = { categoryMenuExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        listOf("General", "Health", "Finance", "Work", "Family", "Vehicle", "Insurance", "Education").forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { viewModel.setCategory(cat); categoryMenuExpanded = false })
                        }
                    }
                }
            }
            // Priority
            item {
                Box {
                    OutlinedTextField(
                        value = state.priority, onValueChange = {},
                        label = { Text("Priority") }, readOnly = true,
                        trailingIcon = { IconButton(onClick = { priorityMenuExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = priorityMenuExpanded, onDismissRequest = { priorityMenuExpanded = false }) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                            DropdownMenuItem(text = { Text(p) }, onClick = { viewModel.setPriority(p); priorityMenuExpanded = false })
                        }
                    }
                }
            }
            // Repeat type
            item {
                Box {
                    OutlinedTextField(
                        value = state.repeatType, onValueChange = {},
                        label = { Text("Repeat") }, readOnly = true,
                        trailingIcon = { IconButton(onClick = { repeatMenuExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Repeat, null) }
                    )
                    DropdownMenu(expanded = repeatMenuExpanded, onDismissRequest = { repeatMenuExpanded = false }) {
                        listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { r ->
                            DropdownMenuItem(text = { Text(r) }, onClick = { viewModel.setRepeatType(r); repeatMenuExpanded = false })
                        }
                    }
                }
            }
            // Reminder toggle
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Set Reminder", style = MaterialTheme.typography.bodyLarge)
                                Text("Get notified before the event",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = state.hasReminder, onCheckedChange = { viewModel.setHasReminder(it) })
                        }
                        if (state.hasReminder) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = (state.reminderDate ?: state.eventDate.minusDays(1)).format(dateFmt),
                                onValueChange = {}, readOnly = true,
                                label = { Text("Reminder Date") },
                                trailingIcon = {
                                    IconButton(onClick = { showReminderDatePicker = true }) {
                                        Icon(Icons.Default.CalendarToday, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            item {
                if (state.isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Event Detail Screen
// ──────────────────────────────────────────────────────────────────────────────

data class EventDetailState(
    val event: EventEntity? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventDetailState())
    val state: StateFlow<EventDetailState> = _state.asStateFlow()

    fun loadEvent(id: Long) {
        viewModelScope.launch {
            val event = repository.getEventById(id)
            _state.update { it.copy(event = event, isLoading = false) }
        }
    }

    fun deleteEvent(context: android.content.Context) {
        viewModelScope.launch {
            val event = _state.value.event ?: return@launch
            EventReminderScheduler.cancelReminder(context, event.id)
            repository.deleteEvent(event)
            _state.update { it.copy(isDeleted = true) }
        }
    }
}

@Composable
fun EventDetailScreen(
    eventId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    LaunchedEffect(eventId) { viewModel.loadEvent(eventId) }
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Event Details",
                onBack = onBack,
                actions = {
                    state.event?.let {
                        IconButton(onClick = { onEdit(eventId) }) { Icon(Icons.Default.Edit, "Edit") }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.event == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Event not found") }
            else -> {
                val event = state.event!!
                val today = LocalDate.now()
                val isOverdue = event.eventDate.isBefore(today)
                val priorityColor = when (event.priority) {
                    "HIGH" -> Color(0xFFE53935); "LOW" -> Color(0xFF43A047); else -> Color(0xFFFFA000)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer
                                                else MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CalendarToday, null,
                                    modifier = Modifier.size(40.dp),
                                    tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text(event.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold)
                                Text(event.eventDate.format(dateFmt),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                if (isOverdue) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("This event is past due",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (event.description.isNotBlank()) {
                                    DetailRow(Icons.Default.Description, "Description", event.description)
                                    HorizontalDivider()
                                }
                                DetailRow(Icons.Default.Label, "Category", event.category)
                                HorizontalDivider()
                                DetailRow(Icons.Default.Warning, "Priority", event.priority,
                                    valueColor = priorityColor)
                                HorizontalDivider()
                                DetailRow(Icons.Default.Repeat, "Repeat", event.repeatType)
                                if (event.reminderDate != null) {
                                    HorizontalDivider()
                                    DetailRow(Icons.Default.NotificationsActive, "Reminder",
                                        event.reminderDate!!.format(dateFmt),
                                        valueColor = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Event?") },
            text = { Text("This event and its reminder will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteEvent(context); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(icon, null,
            modifier = Modifier.size(20.dp).padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Simple Date Picker Dialog (no dependency on DatePicker library)
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val date = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    onDateSelected(date)
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = datePickerState)
    }
}
