package com.mycelium.wallet.activity.supportchat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.mycelium.supportchat.ui.SupportChatEvent
import com.mycelium.supportchat.ui.SupportChatScreen
import com.mycelium.supportchat.viewmodel.SupportChatUIStateManager
import com.mycelium.supportchat.viewmodel.SupportChatViewModel

class SupportChatActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = viewModelFactory {
            initializer {
                SupportChatViewModel(
                    uiStateM = SupportChatUIStateManager(),
                    chatRepository = SupportChatDependencies.chatRepository,
                    identityManager = SupportChatDependencies.identityManager,
                    imageUploadService = SupportChatDependencies.imageUploadService,
                    shareProvider = AndroidShareProvider(applicationContext),
                    navController = NavController(this@SupportChatActivity)
                )
            }
        }
        val viewModel = ViewModelProvider(this, factory)[SupportChatViewModel::class.java]

        setContent {
            MaterialTheme {
                val state by viewModel.uiStateM.uiState.collectAsState()
                SupportChatScreen(
                    state = state,
                    onEvent = { event ->
                        if (event is SupportChatEvent.OnBack) {
                            finish()
                        } else {
                            viewModel.handleEvent(event)
                        }
                    }
                )
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SupportChatActivity::class.java))
        }
    }
}
