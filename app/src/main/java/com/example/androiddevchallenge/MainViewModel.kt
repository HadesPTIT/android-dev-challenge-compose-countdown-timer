package com.example.androiddevchallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel : ViewModel() {

    private val eventChannel = Channel<TimerEvent>(Channel.BUFFERED)

    val dispatch = { event: TimerEvent ->
        if (!eventChannel.isClosedForSend) {
            eventChannel.offer(event)
        }
    }

    private var _uiState = MutableStateFlow(MainState.initial())
    val uiState: StateFlow<MainState> = _uiState

    init {
        val resume = TIMER_INITIAL_DURATION

        eventChannel.consumeAsFlow()
            .onEach { event ->
                when (event) {
                    TimerEvent.START -> Unit
                    TimerEvent.PAUSE -> _uiState.value.seconds
                    TimerEvent.RESET -> TIMER_INITIAL_DURATION
                }
            }
            .flatMapLatest { event ->
                when (event) {
                    TimerEvent.START -> {

                        // Sequence start -> next
                        generateSequence(resume - 1) { it - 1 }
                            .asFlow()
                            .onEach { delay(1000) }
                            .onStart { emit(resume) }
                            .takeWhile { it >= 0 }
                            .map {
                                MainState(
                                    seconds = it,
                                    state = TimerState.RUNNING
                                )
                            }
                            .onCompletion {
                                emit(MainState.initial())
                            }

                    }
                    TimerEvent.PAUSE -> flowOf(
                        MainState(
                            state = TimerState.PAUSED,
                            seconds = resume
                        )
                    )
                    TimerEvent.RESET -> flowOf(MainState.initial())
                }
            }
            .onEach {
                _uiState.value = it
            }
            .catch {

            }
            .launchIn(viewModelScope)
    }

}