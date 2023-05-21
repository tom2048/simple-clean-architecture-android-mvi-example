package com.example.simplecleanarchitecture.users.usecase.user

import com.example.simplecleanarchitecture.core.repository.UsersRepository
import kotlinx.coroutines.flow.Flow

interface UserDeleteUseCase : suspend (String) -> Flow<Unit>

class UserDeleteUseCaseDefault(private val usersRepository: UsersRepository) : UserDeleteUseCase {

    // No unit tests for simple getters
    override suspend fun invoke(id: String): Flow<Unit> = usersRepository.delete(id)

}