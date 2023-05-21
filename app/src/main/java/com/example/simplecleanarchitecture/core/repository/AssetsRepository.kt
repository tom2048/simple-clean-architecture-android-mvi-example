package com.example.simplecleanarchitecture.core.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * This is repository responsible for providing user assets like images, logos, documents, avatars, etc.
 */
interface AssetsRepository {
    suspend fun getImage(assetId: String): Flow<ByteArray>
    suspend fun saveImage(assetId: String, contents: ByteArray): Flow<Unit>
    suspend fun deleteImage(assetId: String): Flow<Unit>

    companion object {
        const val AVATAR_ID_PATTERN = "%s-avatar"
        const val ID_SCAN_ID_PATTERN = "%s-id-scan"
    }

}

/**
 * Implementation of asset repository. This is just an example, so for simplicity we have simple memory implementation, but in real life most likely
 * we will need file system based repository here.
 */
class AssetsRepositoryMemory() : AssetsRepository {

    private val assets = mutableMapOf<String, ByteArray>()

    override suspend fun getImage(assetId: String): Flow<ByteArray> {
        return flow {
            delay(TEST_DELAY_MILLIS)
            synchronized(assets) {
                assets[assetId]
            }?.let { image ->
                emit(image)
            } ?: run {
                throw Exception("User not found")
            }
        }
    }

    override suspend fun saveImage(assetId: String, contents: ByteArray): Flow<Unit> {
        synchronized(assets) {
            assets[assetId] = contents
        }
        return flow {
            delay(TEST_DELAY_MILLIS)
            emit(Unit)
        }
    }

    override suspend fun deleteImage(assetId: String): Flow<Unit> {
        delay(TEST_DELAY_MILLIS)
        val result = synchronized(assets) {
            assets.remove(assetId)
        }
        return if (result != null) {
            flow { emit(Unit) }
        } else {
            flow { throw Exception("Invalid user id") }
        }
    }

    companion object {
        private const val TEST_DELAY_MILLIS = 1L
    }
}