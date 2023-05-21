package com.example.simplecleanarchitecture.core.lib.extensions

import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.internal.createInstance

inline fun <reified T : Any> anyNotNull(): T = any()

inline fun <reified T : String> nullOrEmpty(): T {
    return Mockito.argThat { arg: T? -> arg.isNullOrEmpty() } ?: createInstance(
        T::class
    )
}

inline fun <reified T : String> notNullOrEmpty(): T {
    return Mockito.argThat { arg: T? -> !arg.isNullOrEmpty() } ?: createInstance(
        T::class
    )
}

