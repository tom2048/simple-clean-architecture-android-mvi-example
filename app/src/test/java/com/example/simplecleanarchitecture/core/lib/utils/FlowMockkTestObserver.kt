package com.example.simplecleanarchitecture.core.lib.utils

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface FlowMockkTestObserver<T> {

    fun onError(throwable: Throwable)

    fun onEach(item: T)

    fun onCompletion()

}

suspend fun <T> Flow<T>.mockkTest(): FlowMockkTestObserver<T> {
    val observer: FlowMockkTestObserver<T> = mockk(relaxed = true)
    this
        .catch {
            observer.onError(it)
        }
        .onEach {
            observer.onEach(it)
        }
        .onCompletion {
            observer.onCompletion()
        }
        .collect()
    return observer
}

class FlowObserver<T>() {

    private var job: Job? = null

    fun onError(throwable: Throwable) {}

    fun onEach(item: T) {}

    fun onCompletion() {}

    fun setup(job: Job) {
        this.job = job
    }

    fun cancel() {
        job!!.cancel()
    }
}


suspend fun <T> Flow<T>.mockkObserver(
    coroutineScope: CoroutineScope,
    clearMocks: Boolean = false
): FlowObserver<T> {
    val observerMock: FlowObserver<T> = spyk(FlowObserver<T>())
    val job = coroutineScope.launch {
        this@mockkObserver
            .catch { observerMock.onError(it) }
            .onEach { observerMock.onEach(it) }
            .onCompletion { observerMock.onCompletion() }
            .collect()
    }
    observerMock.setup(job)
    if (clearMocks) {
        clearMocks(observerMock)
    }
    return observerMock
}

suspend fun <T> Flow<T>.mockkObserver(
    coroutineScope: CoroutineScope,
    clearMocks: Boolean = false,
    action: ((FlowObserver<T>) -> Unit)? = null
): FlowObserver<T> {
    val observerMock: FlowObserver<T> = this.mockkObserver(coroutineScope, clearMocks)
    action?.let { it.invoke(observerMock) }
    observerMock.cancel()
    return observerMock
}