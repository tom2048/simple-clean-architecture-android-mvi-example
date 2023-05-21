package com.example.simplecleanarchitecture.users.usecase.user

import com.example.simplecleanarchitecture.core.repository.UsersRepository
import kotlinx.coroutines.flow.Flow

interface UserPasswordUpdateUseCase : suspend (String, String) -> Flow<Unit>

class UserPasswordUpdateUseCaseDefault(private val usersRepository: UsersRepository) :
    UserPasswordUpdateUseCase {

    // No unit tests for simple getters
    override suspend fun invoke(userId: String, password: String): Flow<Unit> =
        usersRepository.updatePassword(userId, password)

}