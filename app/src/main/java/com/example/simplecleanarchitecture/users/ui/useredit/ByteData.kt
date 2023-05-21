package com.example.simplecleanarchitecture.users.ui.useredit

/**
 * Data class wrapper for ByteArray due to the issue related to using arrays in data classes:
 * Property with 'Array' type in a 'data' class: it is recommended to override 'equals()' and 'hashCode()'
 */
data class ByteData(val bytes: ByteArray?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteData

        if (bytes != null) {
            if (other.bytes == null) return false
            if (!bytes.contentEquals(other.bytes)) return false
        } else if (other.bytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes?.contentHashCode() ?: 0
    }
}

fun byteDataOf(byteArray: ByteArray?) = ByteData(byteArray)