package io.github.openapitest

import androidx.lifecycle.ViewModel
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

class ChatViewModel : ViewModel() {
    private val repository = OpenAiChatRepository(
        apiKey = BuildConfig.OPENAI_API_KEY,
        model = BuildConfig.OPENAI_MODEL,
        vectorStoreId = BuildConfig.OPENAI_VECTOR_STORE_ID
    )

    private val mutableUiState = MutableStateFlow(ChatUiState())
    val uiState = mutableUiState.asStateFlow()

    private var requestJob: Job? = null
    private var failedQuestion: String? = null

    fun sendMessage(question: String) {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isEmpty() || mutableUiState.value.isLoading) return

        failedQuestion = trimmedQuestion
        mutableUiState.value = mutableUiState.value.copy(
            messages = mutableUiState.value.messages + ChatMessage(
                role = ChatMessageRole.USER,
                text = trimmedQuestion
            ),
            isLoading = true,
            errorMessage = null
        )
        requestAnswer(trimmedQuestion)
    }

    fun retryLastRequest() {
        val question = failedQuestion ?: return
        if (!mutableUiState.value.isLoading) {
            requestAnswer(question)
        }
    }

    fun clearConversation() {
        requestJob?.cancel()
        requestJob = null
        failedQuestion = null
        mutableUiState.value = ChatUiState()
    }

    private fun requestAnswer(question: String) {
        requestJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        requestJob = viewModelScope.launch {
            try {
                val answer = repository.answer(question)
                failedQuestion = null
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
                failedQuestion = question
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
