package com.example.simplecleanarchitecture.users.usecase.user

import com.example.simplecleanarchitecture.core.lib.TestException
import com.example.simplecleanarchitecture.core.lib.schedulers.MainCoroutineRule
import com.example.simplecleanarchitecture.core.lib.utils.mockkTest
import com.example.simplecleanarchitecture.core.model.User
import com.example.simplecleanarchitecture.core.repository.AssetsRepository
import com.example.simplecleanarchitecture.core.repository.UsersRepository
import com.example.simplecleanarchitecture.core.repository.model.UserDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UserShowDetailsUseCaseDefaultTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var userDetailsUseCase: UserShowDetailsUseCaseDefault

    private lateinit var usersRepository: UsersRepository
    private lateinit var assetsRepository: AssetsRepository

    @Test
    fun `invoke() merges user details and assets when user exists`() = runTest {
        val expected = User(
            DEFAULT_USER.id,
            DEFAULT_USER.nickname,
            DEFAULT_USER.email,
            DEFAULT_USER.description,
            byteArrayOf(),
            byteArrayOf()
        )
        coEvery { usersRepository.get(any()) } returns flowOf(DEFAULT_USER)
        coEvery { assetsRepository.getImage(AssetsRepository.AVATAR_ID_PATTERN.format(expected.id)) } returns flowOf(
            expected.photo!!
        )
        coEvery { assetsRepository.getImage(AssetsRepository.ID_SCAN_ID_PATTERN.format(expected.id)) } returns flowOf(
            expected.idScan!!
        )

        val result = userDetailsUseCase(DEFAULT_USER.id!!).last()

        assert(result == expected)
        coVerify(exactly = 1) {
            usersRepository.get(any())
            assetsRepository.getImage(AssetsRepository.AVATAR_ID_PATTERN.format(expected.id))
            assetsRepository.getImage(AssetsRepository.ID_SCAN_ID_PATTERN.format(expected.id))
        }
    }

    @Test
    fun `invoke() don't load assets when user don't exists`() = runTest {
        val error = TestException()
        coEvery { usersRepository.get(any()) } returns flow { throw error }
        coEvery { assetsRepository.getImage(any()) } returns flowOf(byteArrayOf())

        val observer = userDetailsUseCase.invoke(DEFAULT_USER.id!!).mockkTest()
        coVerifyAll { usersRepository.get(any()) }
        coVerify(exactly = 0) { assetsRepository.getImage(any()) }
    }

    @Test
    fun `invoke() doesn't result with error when user exists and photo doesn't`() = runTest {
        val expected = User(
            DEFAULT_USER.id,
            DEFAULT_USER.nickname,
            DEFAULT_USER.email,
            DEFAULT_USER.description,
            null
        )
        coEvery { usersRepository.get(any()) } returns flowOf(DEFAULT_USER)
        coEvery { assetsRepository.getImage(any()) } returns flow { throw TestException() }

        val observer = userDetailsUseCase.invoke(DEFAULT_USER.id!!).mockkTest()

        verify {
            observer.onEach(expected)
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
        userDetailsUseCase = UserShowDetailsUseCaseDefault(usersRepository, assetsRepository)
    }

    private fun cleanupUseCase() {
        // TODO:
    }

    companion object {
        private val DEFAULT_USER = UserDetails(
            "a312b3ee-84c2-11eb-8dcd-0242ac130003",
            "Testnick",
            "test@test.com",
            "Test description"
        )
    }
}