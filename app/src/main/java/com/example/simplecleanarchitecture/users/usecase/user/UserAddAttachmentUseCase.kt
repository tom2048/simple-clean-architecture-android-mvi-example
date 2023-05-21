package com.example.simplecleanarchitecture.users.usecase.user

import com.example.simplecleanarchitecture.core.repository.AssetsRepository
import com.example.simplecleanarchitecture.core.repository.StorageRepository
import com.example.simplecleanarchitecture.users.usecase.user.UserAddAttachmentUseCaseDefault.Type
import com.example.simplecleanarchitecture.users.usecase.user.UserAddAttachmentUseCaseDefault.Type.Avatar
import com.example.simplecleanarchitecture.users.usecase.user.UserAddAttachmentUseCaseDefault.Type.IdScan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

interface UserAddAttachmentUseCase : (String, String, Type) -> Flow<String>

class UserAddAttachmentUseCaseDefault(
    private val storageRepository: StorageRepository,
    private val assetsRepository: AssetsRepository
) : UserAddAttachmentUseCase {

    override fun invoke(userId: String, url: String, type: Type): Flow<String> =
        storageRepository.load(url)
            .flatMapConcat { data ->
                when (type) {
                    Avatar -> AssetsRepository.AVATAR_ID_PATTERN.format(userId)
                    IdScan -> AssetsRepository.ID_SCAN_ID_PATTERN.format(userId)
                }.let { key ->
                    assetsRepository.saveImage(key, data).map { key }
                }
            }

    enum class Type {
        Avatar, IdScan
    }
}