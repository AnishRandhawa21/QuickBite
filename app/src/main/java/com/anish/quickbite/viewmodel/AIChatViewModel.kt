package com.anish.quickbite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.quickbite.data.model.MenuItem
import com.anish.quickbite.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val suggestedItems: List<MenuItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ASSISTANT
}

data class AIChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "Hey! I'm your QuickBite Campus Food Assistant.\n\nAsk me about meals within your budget, what's currently available, or recommendations for a quick break!"
        )
    ),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val quickPrompts: List<String> = listOf(
        "What can I get under ₹60?",
        "Suggest something filling",
        "What is available right now?",
        "Quick snacks for break"
    )
)

class AIChatViewModel(
    private val repository: StudentRepository = StudentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    private var cachedItems: List<MenuItem> = emptyList()

    init {
        loadMenuContext()
    }

    private fun loadMenuContext() {
        viewModelScope.launch {
            repository.getCanteens().onSuccess { canteens ->
                val openCanteen = canteens.firstOrNull { it.isOpen } ?: canteens.firstOrNull()
                if (openCanteen != null) {
                    repository.getMenuItems(openCanteen.id).onSuccess { items ->
                        cachedItems = items
                    }
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(userText: String = _uiState.value.inputText) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isThinking = true
            )
        }

        viewModelScope.launch {
            // Intelligent local matching based on real menu data
            val lower = trimmed.lowercase()
            val response: ChatMessage = when {
                lower.contains("under") || lower.contains("budget") || lower.contains("60") || lower.contains("cheap") -> {
                    val budget = Regex("""\d+""").find(lower)?.value?.toDoubleOrNull() ?: 60.0
                    val affordable = cachedItems.filter { it.price <= budget && it.isAvailable }
                    if (affordable.isNotEmpty()) {
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "Here are the best items available under ₹${budget.toInt()}:",
                            suggestedItems = affordable.take(4)
                        )
                    } else {
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "I couldn't find items under ₹${budget.toInt()} right now. Check the main menu for other great options!"
                        )
                    }
                }
                lower.contains("filling") || lower.contains("meal") || lower.contains("heavy") || lower.contains("lunch") -> {
                    val filling = cachedItems.filter {
                        it.isAvailable && (it.category.contains("Meal", true) || it.category.contains("Lunch", true) || it.price >= 50)
                    }
                    if (filling.isNotEmpty()) {
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "For a solid, filling meal, I recommend:",
                            suggestedItems = filling.take(3)
                        )
                    } else {
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "Here are the top available meals in the canteen:",
                            suggestedItems = cachedItems.filter { it.isAvailable }.take(3)
                        )
                    }
                }
                lower.contains("available") || lower.contains("now") || lower.contains("open") -> {
                    val available = cachedItems.filter { it.isAvailable }
                    ChatMessage(
                        sender = MessageSender.ASSISTANT,
                        text = if (available.isNotEmpty())
                            "These items are currently available and ready for quick pickup:"
                        else
                            "No food items are currently marked available in the open canteen.",
                        suggestedItems = available.take(5)
                    )
                }
                lower.contains("snack") || lower.contains("quick") || lower.contains("break") -> {
                    val snacks = cachedItems.filter {
                        it.isAvailable && (it.category.contains("Snack", true) || it.price <= 40)
                    }
                    ChatMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "Here are some quick snacks you can pick up during a short break:",
                        suggestedItems = snacks.take(4)
                    )
                }
                else -> {
                    // Search items matching query
                    val matched = cachedItems.filter {
                        it.name.contains(trimmed, ignoreCase = true) ||
                                it.category.contains(trimmed, ignoreCase = true)
                    }
                    if (matched.isNotEmpty()) {
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "Here is what matches \"$trimmed\":",
                            suggestedItems = matched
                        )
                    } else {
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "I found these recommendations from today's menu:",
                            suggestedItems = cachedItems.filter { it.isAvailable }.take(3)
                        )
                    }
                }
            }

            _uiState.update {
                it.copy(
                    messages = it.messages + response,
                    isThinking = false
                )
            }
        }
    }
}
