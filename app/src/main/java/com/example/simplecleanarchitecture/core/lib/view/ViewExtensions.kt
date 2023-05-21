package com.example.simplecleanarchitecture.core.lib.view

import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

fun TextInputLayout.updateError(errorText: CharSequence) {
    if (!this.error.contentEquals(errorText)) {
        this.error = errorText
    }
}

fun TextView.updateText(text: CharSequence) {
    if (!this.text.contentEquals(text)) {
        this.text = text
    }
}

fun EditText.addTextUpdatedListener(afterTextChanged: (text: Editable?) -> Unit = {}) {
    this.addTextChangedListener { text ->
        if (this.text != text) {
            afterTextChanged.invoke(text)
        }
    }
}

fun Button.updateEnabled(isEnabled: Boolean) {
    if (this.isEnabled != isEnabled) {
        this.isEnabled = isEnabled
    }
}

fun View.updateVisible(isVisible: Boolean) {
    if (this.isVisible != isVisible) {
        this.isVisible = isVisible
    }
}

fun View.updateGone(isGone: Boolean) {
    if (this.isGone != isGone) {
        this.isGone = isGone
    }
}

fun View.updateInvisible(isInvisible: Boolean) {
    if (this.isInvisible != isInvisible) {
        this.isInvisible = isInvisible
    }
}

fun <T : IFlexible<*>> FlexibleAdapter<T>.updateDataSetIfChanged(
    data: List<T>,
    matcher: (T, T) -> Boolean
) {
    if (
        this.currentItems.size != data.size ||
        !this.currentItems.zip(data).all { matcher(it.first, it.second) }
    ) {
        this.updateDataSet(data)
    }
}