package com.example.simplecleanarchitecture.core.lib

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.reflect.jvm.isAccessible

interface TestHelper {

    val lifecycleOwner: LifecycleOwner
    val lifecycle: LifecycleRegistry

    fun prepareLifecycle()

    fun cleanUpLifecycle()

    fun invokeViewModelOnCleared(viewModel: ViewModel)
}

class DefaultTestHelper : TestHelper {

    override lateinit var lifecycleOwner: LifecycleOwner
        private set
    override lateinit var lifecycle: LifecycleRegistry
        private set

    override fun prepareLifecycle() {
        lifecycleOwner = mock()
        lifecycle = LifecycleRegistry(lifecycleOwner).apply {
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            whenever(lifecycleOwner.lifecycle).thenReturn(this)
        }
    }

    override fun cleanUpLifecycle() {
        lifecycle.run {
            handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    override fun invokeViewModelOnCleared(viewModel: ViewModel) {
        // Please read for further information: https://stackoverflow.com/questions/54115627/how-to-ensure-viewmodeloncleared-is-called-in-an-android-unit-test
        ViewModel::class.members
            .single { it.name == "onCleared" }
            .apply { isAccessible = true }
            .call(viewModel)
    }
}