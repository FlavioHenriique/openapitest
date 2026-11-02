package io.github.openapitest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class ChatMessageRole {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatMessageRole,
    val text: String
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val instructions = application.resources.openRawResource(
        R.raw.imune_360_assistant_prompt
    ).bufferedReader().use { it.readText() }

    private val repository = OpenAiChatRepository(
        apiKey = BuildConfig.OPENAI_API_KEY,
        model = BuildConfig.OPENAI_MODEL,
        vectorStoreId = BuildConfig.OPENAI_VECTOR_STORE_ID,
        instructions = instructions
    )

    private val mutableUiState = MutableStateFlow(ChatUiState())
    val uiState = mutableUiState.asStateFlow()

    private var requestJob: Job? = null
    private var failedConversation: List<ChatMessage>? = null
    private var conversationGeneration: Long = 0

    fun sendMessage(question: String) {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isEmpty() || mutableUiState.value.isLoading) return

        val conversation = mutableUiState.value.messages + ChatMessage(
            role = ChatMessageRole.USER,
            text = trimmedQuestion
        )
        failedConversation = conversation
        mutableUiState.value = mutableUiState.value.copy(
            messages = conversation,
            isLoading = true,
            errorMessage = null
        )
        requestAnswer(conversation)
    }

    fun retryLastRequest() {
        val conversation = failedConversation ?: return
        if (!mutableUiState.value.isLoading) {
            requestAnswer(conversation)
        }
    }

    fun clearConversation() {
        conversationGeneration += 1
        requestJob?.cancel()
        requestJob = null
        failedConversation = null
        mutableUiState.value = ChatUiState()
    }

    private fun requestAnswer(conversation: List<ChatMessage>) {
        val conversationSnapshot = conversation.toList()
        if (conversationSnapshot.lastOrNull()?.role != ChatMessageRole.USER) return

        val requestGeneration = conversationGeneration
        requestJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        requestJob = viewModelScope.launch {
            try {
                val answer = repository.answer(conversationSnapshot)
                if (requestGeneration != conversationGeneration) return@launch

                failedConversation = null
                mutableUiState.value = mutableUiState.value.copy(
                    messages = mutableUiState.value.messages + ChatMessage(
                        role = ChatMessageRole.ASSISTANT,
                        text = answer
                    ),
                    isLoading = false,
                    errorMessage = null
                )
                requestJob = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (requestGeneration != conversationGeneration) return@launch

                failedConversation = conversationSnapshot
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message
                        ?: "Não foi possível obter uma resposta. Tente novamente."
                )
                requestJob = null
            }
        }
    }
}
