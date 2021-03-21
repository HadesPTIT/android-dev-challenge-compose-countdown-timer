/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.takeWhile

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
        var remain = TIMER_INITIAL_DURATION
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
                        generateSequence(remain - 1) { it - 1 }
                            .asFlow()
                            .onEach {
                                delay(1000)
                                remain -= 1
                            }
                            .onStart { emit(remain) }
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
                    TimerEvent.PAUSE -> {
                        flowOf(
                            MainState(
                                state = TimerState.PAUSED,
                                seconds = _uiState.value.seconds
                            )
                        )
                    }
                    TimerEvent.RESET -> {
                        remain = TIMER_INITIAL_DURATION
                        flowOf(MainState.initial())
                    }
                }
            }
            .onEach {
                _uiState.value = it
                Log.d("TAG", "State: $it --- SECOND $remain")
            }
            .launchIn(viewModelScope)
    }
}
