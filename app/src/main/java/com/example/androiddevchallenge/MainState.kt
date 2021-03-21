package com.example.androiddevchallenge

data class MainState(
    val state: TimerState = TimerState.IDLE,
    val seconds: Long = 0
) {

    companion object {
        fun initial(): MainState {
            return MainState(
                seconds = TIMER_INITIAL_DURATION
            )
        }
    }


}

enum class TimerState {
    RUNNING, PAUSED, IDLE
}

enum class TimerEvent {
    START, PAUSE, RESET
}