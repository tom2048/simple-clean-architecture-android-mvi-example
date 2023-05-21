package com.example.simplecleanarchitecture.core.repository

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream

interface StorageRepository {
    fun load(url: String): Flow<ByteArray>
}

class FileStorageRepository(private val application: Application) : StorageRepository {

    override fun load(url: String): Flow<ByteArray> = flow {
        var inputStream: InputStream? = null
        try {
            inputStream = application.contentResolver.openInputStream(Uri.parse(url))
            emit(inputStream?.readBytes()!!)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
    }

}

