package io.github.openapitest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.openapitest.ui.ChatScreen
import io.github.openapitest.ui.theme.OpenAPITestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenAPITestTheme {
                val chatViewModel: ChatViewModel = viewModel()
                val uiState by chatViewModel.uiState.collectAsState()

                ChatScreen(
                    uiState = uiState,
                    onSendMessage = chatViewModel::sendMessage,
                    onRetry = chatViewModel::retryLastRequest,
                    onNewConversation = chatViewModel::clearConversation
                )
            }
        }
    }
}
