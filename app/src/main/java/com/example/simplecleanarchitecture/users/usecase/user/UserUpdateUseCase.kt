package com.example.simplecleanarchitecture.users.usecase.user

import com.example.simplecleanarchitecture.core.lib.exception.ValidationException
import com.example.simplecleanarchitecture.core.lib.resources.AppResources
import com.example.simplecleanarchitecture.core.model.User
import com.example.simplecleanarchitecture.core.repository.AssetsRepository
import com.example.simplecleanarchitecture.core.repository.UsersRepository
import com.example.simplecleanarchitecture.core.repository.model.UserDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow

interface UserUpdateUseCase : suspend (User) -> Flow<Unit>

class UserUpdateUseCaseDefault(
    private val usersRepository: UsersRepository,
    private val assetsRepository: AssetsRepository,
    private val appResources: AppResources
) : UserUpdateUseCase {

    override suspend fun invoke(user: User): Flow<Unit> {
        // TODO: It would be possible to create validation in this place instead of in the viewModel, which could be more convenient when reusing use case or
        //  simply when performing client - server consistent validation. TBD
        // Regarding validation - there is some useful discussion about:
        // https://stackoverflow.com/questions/57603422/clean-architecture-where-to-put-input-validation-logic
        // https://groups.google.com/g/clean-code-discussion/c/latn4x6Zo7w/m/bFwtDI1XSA8J
        //val validationErrors = mutableListOf<Pair<String, String>>()
        //if (!Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
        //    validationErrors.add(Pair(Validation.EMAIL_VALIDATION_KEY, appResources.getStringResource(R.string.email_validation_message)))
        //}
        //val isValid = validationErrors.isEmpty()
        val isValid = true
        return if (isValid) {
            val userDetails = UserDetails(user.id, user.nickname, user.email, user.description)
            if (user.id.isNullOrEmpty()) {
                usersRepository.insert(userDetails)
                    .flatMapConcat { id ->
                        user.photo?.let {
                            assetsRepository.saveImage(
                                AssetsRepository.AVATAR_ID_PATTERN.format(id),
                                it
                            ).flatMapConcat {
                                flow { emit(id) }
                            }
                        } ?: run {
                            flow { emit(id) }
                        }
                    }
                    .flatMapConcat { id ->
                        user.idScan?.let {
                            assetsRepository.saveImage(
                                AssetsRepository.ID_SCAN_ID_PATTERN.format(id),
                                it
                            )
                        } ?: run {
                            flow { emit(Unit) }
                        }
                    }
            } else {
                usersRepository.update(userDetails)
                    .flatMapConcat {
                        user.photo?.let {
                            assetsRepository.saveImage(
                                AssetsRepository.AVATAR_ID_PATTERN.format(user.id),
                                it
                            )
                        } ?: run {
                            flow { emit(Unit) }
                        }
                    }
                    .flatMapConcat {
                        user.idScan?.let {
                            assetsRepository.saveImage(
                                AssetsRepository.ID_SCAN_ID_PATTERN.format(user.id),
                                it
                            )
                        } ?: run {
                            flow { emit(Unit) }
                        }
                    }
            }
        } else {
            // TODO: Possible validation
            flow { throw ValidationException(listOf()) }
        }
    }

}