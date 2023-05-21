package com.example.simplecleanarchitecture.users.usecase.user

import com.example.simplecleanarchitecture.core.lib.resources.AppResources
import com.example.simplecleanarchitecture.core.lib.schedulers.MainCoroutineRule
import com.example.simplecleanarchitecture.core.lib.utils.mockkTest
import com.example.simplecleanarchitecture.core.model.User
import com.example.simplecleanarchitecture.core.repository.AssetsRepository
import com.example.simplecleanarchitecture.core.repository.UsersRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UserUpdateUseCaseDefaultTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var userUpdateUseCase: UserUpdateUseCase

    private lateinit var usersRepository: UsersRepository
    private lateinit var assetsRepository: AssetsRepository
    private lateinit var appResources: AppResources

    @Test
    fun `invoke() executes insert when empty user id`() = runTest {
        coEvery { usersRepository.insert(any()) } returns flowOf(DEFAULT_USER.id!!)
        coEvery { usersRepository.update(any()) } returns flowOf(Unit)
        val newUser = DEFAULT_USER.copy(id = null)

        val testObserver = userUpdateUseCase(newUser).mockkTest()

        verify() { testObserver.onCompletion() }
        coVerify(exactly = 1) { usersRepository.insert(any()) }
        coVerify(exactly = 0) { usersRepository.update(any()) }
    }

    @Test
    fun `invoke() executes update when empty user id`() = runTest {
        val existingUser = DEFAULT_USER

        val testObserver = userUpdateUseCase(existingUser).mockkTest()

        verify { testObserver.onCompletion() }
        coVerify(exactly = 0) { usersRepository.insert(any()) }
        coVerify(exactly = 1) { usersRepository.update(any()) }
    }

    @Test
    fun `invoke() saves photo when avatar not null and new user added`() = runTest {
        val newUser = DEFAULT_USER.copy(id = null, photo = byteArrayOf())

        val testObserver = userUpdateUseCase(newUser).mockkTest()

        verify { testObserver.onCompletion() }
        coVerify {
            assetsRepository.saveImage(
                AssetsRepository.AVATAR_ID_PATTERN.format(DEFAULT_USER.id),
                any()
            )
        }
    }

    @Test
    fun `invoke() saves photo when avatar not null and existing user edited`() = runTest {
        val existingUser = DEFAULT_USER.copy(photo = byteArrayOf())

        val testObserver = userUpdateUseCase(existingUser).mockkTest()

        verify { testObserver.onCompletion() }
        coVerify {
            assetsRepository.saveImage(
                AssetsRepository.AVATAR_ID_PATTERN.format(DEFAULT_USER.id),
                any()
            )
        }
    }

    @Test
    fun `invoke() saves scan photo when avatar not null and new user added`() = runTest {
        val newUser = DEFAULT_USER.copy(id = null, idScan = byteArrayOf())

        val testObserver = userUpdateUseCase(newUser).mockkTest()

        verify { testObserver.onCompletion() }
        coVerify {
            assetsRepository.saveImage(
                AssetsRepository.ID_SCAN_ID_PATTERN.format(
                    DEFAULT_USER.id
                ), any()
            )
        }
    }

    @Test
    fun `invoke() saves scan photo when avatar not null and existing user edited`() = runTest {
        val existingUser = DEFAULT_USER.copy(idScan = byteArrayOf())

        val testObserver = userUpdateUseCase(existingUser).mockkTest()

        verify { testObserver.onCompletion() }
        coVerify {
            assetsRepository.saveImage(
                AssetsRepository.ID_SCAN_ID_PATTERN.format(
                    DEFAULT_USER.id
                ), any()
            )
        }
    }


    @Before
    fun setUp() {
        setupUseCase()
    }

    @After
    fun tearDown() {
        cleanupUseCase()
    }

    private fun setupUseCase() {
        usersRepository = mockk()
        assetsRepository = mockk()
        appResources = mockk()
        userUpdateUseCase =
            UserUpdateUseCaseDefault(usersRepository, assetsRepository, appResources)

        coEvery { usersRepository.insert(any()) } returns flowOf(DEFAULT_USER.id!!)
        coEvery { usersRepository.update(any()) } returns flowOf(Unit)
        coEvery { assetsRepository.saveImage(any(), any()) } returns flowOf(Unit)
    }

    private fun cleanupUseCase() {
        // TODO:
    }

    companion object {
        private val DEFAULT_USER = User(
            "a312b3ee-84c2-11eb-8dcd-0242ac130003",
            "Testnick",
            "test@test.com",
            "Test description"
        )
    }
}