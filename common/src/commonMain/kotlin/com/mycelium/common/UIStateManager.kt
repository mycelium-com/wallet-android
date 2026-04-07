package com.mycelium.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Abstract base class for navigation events in the application. Encapsulates navigation route information along with optional navigation parameters and extras.
 */
abstract class NavigationEvent(
    val route: String,
    val navOptions: NavOptions? = null,
    val navigatorExtras: Navigator.Extras? = null
)

object EmptyEvent : NavigationEvent("empty")

/**
 * Interface for components that manage UI state using UIStateManager. Provides a contract for accessing and manipulating UI state and navigation events.
 */
interface WithUIStateManger<UIState> {
    val uiStateM: UIStateManager<UIState>
}

fun <UIState> WithUIStateManger<UIState>.provideUIState() = uiStateM.uiState
val <UIState> WithUIStateManger<UIState>.uiState get() = uiStateM.uiState.value
fun <UIState> WithUIStateManger<UIState>.push(state: UIState) {
    uiStateM.push(state)
}

fun <UIState> WithUIStateManger<UIState>.provideNavEvent() = uiStateM.navigationEvent
fun <UIState> WithUIStateManger<UIState>.navigate(event: NavigationEvent) = uiStateM.navigate(event)

/**
 * Generic UI state manager that handles state updates and navigation events. Provides reactive state management using StateFlow and SharedFlow for UI components.
 */
open class UIStateManager<UIState>(defaultState: UIState) {
    private val _uiState = MutableStateFlow<UIState>(defaultState)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    fun push(state: UIState) = _uiState.tryEmit(state)

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun navigate(event: NavigationEvent) = _navigationEvent.tryEmit(event)
}

fun <T> ViewModel.observeSavedState(
    navController: NavController,
    key: String,
    initialValue: T? = null,
    block: suspend CoroutineScope.(T) -> Unit
) {
    viewModelScope.launch(Dispatchers.Main) {
        navController.currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
            savedStateHandle.getStateFlow<T?>(key, initialValue)
//                .filterNotNull()
                .collect { result ->
                    if(result != null) {
                        savedStateHandle[key] = null
//                        savedStateHandle.remove<T>(key) - work one time
                        block(result)
                    }
                }
        }
    }
}