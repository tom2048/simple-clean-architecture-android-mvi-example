package com.example.simplecleanarchitecture.core.repository

import com.example.simplecleanarchitecture.core.repository.model.UserDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID

interface UsersRepository {
    suspend fun getList(): Flow<List<UserDetails>>
    suspend fun get(id: String): Flow<UserDetails>
    suspend fun insert(user: UserDetails): Flow<String>
    suspend fun update(user: UserDetails): Flow<Unit>
    suspend fun updatePassword(userId: String, password: String): Flow<Unit>
    suspend fun delete(id: String): Flow<Unit>

    data class Response<T>(val response: T)
}

/**
 * Implementation of UsersRepository
 * It's only a simple memory implementation for this example project, in real project it would be a REST or database repository.
 * Timers were added for real data source delay simulation.
 */
class UsersRepositoryMemory() : UsersRepository {

    private val users = mutableMapOf<String, UserWithCredentials>().apply {
        // Test users
        put(
            "a312b3ee-84c2-11eb-8dcd-0242ac130003",
            UserWithCredentials(
                UserDetails(
                    "a312b3ee-84c2-11eb-8dcd-0242ac130003",
                    "Nickname1",
                    "nickname1@test.com",
                    "Test description 1"
                )
            )
        )
        put(
            "3b04aacf-4320-48bb-8171-af512aae0894",
            UserWithCredentials(
                UserDetails(
                    "3b04aacf-4320-48bb-8171-af512aae0894",
                    "Nickname2",
                    "nickname2@test.com",
                    "Test description 2"
                )
            )
        )
        put(
            "52408bc4-4cdf-49ef-ac54-364bfde3fbf0",
            UserWithCredentials(
                UserDetails(
                    "52408bc4-4cdf-49ef-ac54-364bfde3fbf0",
                    "Nickname3",
                    "nickname3@test.com",
                    "Test description 3"
                )
            )
        )
    }

    override suspend fun getList(): Flow<List<UserDetails>> = flow {
        emit(getListInternal())
    }

    private suspend fun getListInternal() = withContext(Dispatchers.IO) {
        delay(TEST_DELAY_MILLIS)
        synchronized(users) {
            users.values.toList().map { it.user }
        }
    }

    override suspend fun get(id: String): Flow<UserDetails> {
        return synchronized(users) {
            users[id]
        }?.let {
            flow {
                delay(TEST_DELAY_MILLIS)
                emit(it.user)
            }
        } ?: run {
            flow {
                delay(TEST_DELAY_MILLIS)
                throw Exception("User not found")
            }
        }
    }

    override suspend fun insert(user: UserDetails): Flow<String> {
        return if (user.id.isNullOrEmpty()) {
            flow {
                delay(TEST_DELAY_MILLIS)
                emit(synchronized(users) {
                    val id = UUID.randomUUID().toString()
                    users[id] = UserWithCredentials(user.copy(id = id))
                    id
                })
            }
        } else {
            flow { throw Exception("Invalid user object") }
        }
    }

    override suspend fun update(user: UserDetails): Flow<Unit> {
        delay(TEST_DELAY_MILLIS)
        return user.id?.let { id ->
            synchronized(users) {
                users[id] = UserWithCredentials(user)
            }
            flow { emit(Unit) }
        } ?: run {
            flow { throw Exception("Invalid user object") }
        }
    }

    override suspend fun updatePassword(userId: String, password: String) = flow<Unit> {
        delay(TEST_DELAY_MILLIS)
        synchronized(users) {
            users[userId]?.let { user ->
                users[userId] = user.copy(password = password)
            } ?: run {
                throw Exception("Invalid user id")
            }
        }
        emit(Unit)
    }

    override suspend fun delete(id: String) = flow<Unit> {
        delay(TEST_DELAY_MILLIS)
        synchronized(users) {
            if (users.remove(id) == null) {
                throw Exception("Invalid user id")
            }
        }
        emit(Unit)
    }

    companion object {
        private const val TEST_DELAY_MILLIS = 300L
    }

    private data class UserWithCredentials(
        val user: UserDetails,
        val password: String? = null
    )
}

