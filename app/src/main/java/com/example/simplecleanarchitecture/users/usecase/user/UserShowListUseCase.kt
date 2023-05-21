package com.example.simplecleanarchitecture.users.usecase.user

import com.example.simplecleanarchitecture.core.repository.UsersRepository
import com.example.simplecleanarchitecture.core.repository.model.UserDetails
import kotlinx.coroutines.flow.Flow

fun interface UserShowListUseCase : suspend () -> Flow<List<UserDetails>>

class UserShowListUseCaseDefault(private val usersRepository: UsersRepository) :
    UserShowListUseCase {

    // No unit tests for simple getters
    override suspend fun invoke(): Flow<List<UserDetails>> = usersRepository
        .getList()

}