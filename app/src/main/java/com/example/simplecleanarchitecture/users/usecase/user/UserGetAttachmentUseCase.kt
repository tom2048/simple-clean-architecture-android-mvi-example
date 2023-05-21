package com.example.simplecleanarchitecture.users.usecase.user

import com.example.simplecleanarchitecture.core.repository.AssetsRepository
import kotlinx.coroutines.flow.Flow

interface UserGetAttachmentUseCase : suspend (String) -> Flow<ByteArray>

class UserGetAttachmentUseCaseDefault(
    private val assetsRepository: AssetsRepository
) : UserGetAttachmentUseCase {

    override suspend fun invoke(key: String): Flow<ByteArray> =
        assetsRepository.getImage(key)

}