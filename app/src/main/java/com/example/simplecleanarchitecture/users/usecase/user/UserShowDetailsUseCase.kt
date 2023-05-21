package com.example.simplecleanarchitecture.users.usecase.user

import com.example.simplecleanarchitecture.core.model.User
import com.example.simplecleanarchitecture.core.repository.AssetsRepository
import com.example.simplecleanarchitecture.core.repository.UsersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

interface UserShowDetailsUseCase : suspend (String) -> Flow<User>

class UserShowDetailsUseCaseDefault(
    private val usersRepository: UsersRepository,
    private val assetsRepository: AssetsRepository
) : UserShowDetailsUseCase {

    override suspend fun invoke(id: String): Flow<User> = usersRepository
        .get(id)
        .map { user ->
            User(user.id, user.nickname, user.email, user.description)
        }
        .flatMapConcat { user ->
            assetsRepository.getImage(AssetsRepository.AVATAR_ID_PATTERN.format(user.id!!))
                .map { image ->
                    user.copy(photo = image)
                }
                .catch {
                    emit(user)
                }
        }
        .flatMapConcat { user ->
            assetsRepository.getImage(AssetsRepository.ID_SCAN_ID_PATTERN.format(user.id!!))
                .map { image ->
                    user.copy(idScan = image)
                }
                .catch {
                    emit(user)
                }
        }

}