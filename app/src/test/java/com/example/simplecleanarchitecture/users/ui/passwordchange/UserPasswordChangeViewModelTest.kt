package com.example.simplecleanarchitecture.users.ui.passwordchange

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.example.simplecleanarchitecture.R
import com.example.simplecleanarchitecture.core.lib.DefaultTestHelper
import com.example.simplecleanarchitecture.core.lib.TestException
import com.example.simplecleanarchitecture.core.lib.TestHelper
import com.example.simplecleanarchitecture.core.lib.resources.AppResources
import com.example.simplecleanarchitecture.core.lib.schedulers.MainCoroutineRule
import com.example.simplecleanarchitecture.core.lib.utils.mockkObserver
import com.example.simplecleanarchitecture.users.ui.passwordchange.UserPasswordChangeViewModel.UiEffect.Routing
import com.example.simplecleanarchitecture.users.usecase.user.UserPasswordUpdateUseCase
import com.github.terrakok.cicerone.Back
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UserPasswordChangeViewModelTest : TestHelper by DefaultTestHelper() {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: UserPasswordChangeViewModel
    private lateinit var appResources: AppResources
    private lateinit var passwordUpdateUseCase: UserPasswordUpdateUseCase

    @Before
    fun setUp() {
        prepareLifecycle()
        setupViewModel()
    }


    @After
    fun tearDown() {
        cleanUpLifecycle()
        invokeViewModelOnCleared(viewModel)
    }


    @Test
    fun `password update hides validation message when password is valid`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this, true)

            viewModel.setPassword(VALID_PASSWORD)

            verify {
                stateObserver.onEach(match { it.password == VALID_PASSWORD && it.passwordValidation == "" })
            }

            stateObserver.cancel()
        }


    @Test
    fun `password update shows validation message when password is invalid`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this, true)

            viewModel.setPassword(INVALID_PASSWORD)

            verifySequence {
                stateObserver.onEach(match { it.password == INVALID_PASSWORD && !it.passwordValidation.isNullOrEmpty() })
            }
            stateObserver.cancel()
        }


    @Test
    fun `password update shows validation message when confirmPassword differs`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            viewModel.setPasswordConfirmed(VALID_PASSWORD)
            clearMocks(stateObserver)
            viewModel.setPassword(VALID_PASSWORD + "test")

            verify { stateObserver.onEach(match { !it.passwordConfirmedValidation.isNullOrEmpty() }) }

            stateObserver.cancel()
        }


    @Test
    fun `password update hides validation message when confirmPassword equals`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            viewModel.setPasswordConfirmed(VALID_PASSWORD)
            clearMocks(stateObserver)

            viewModel.setPassword(VALID_PASSWORD)

            verify { stateObserver.onEach(match { it.passwordConfirmedValidation.isNullOrEmpty() }) }

            stateObserver.cancel()
        }


    @Test
    fun `passwordConfirmed update shows validation message when password differs`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            viewModel.setPassword(VALID_PASSWORD)
            clearMocks(stateObserver)

            viewModel.setPasswordConfirmed(VALID_PASSWORD + "test")

            verify {
                stateObserver.onEach(match { !it.passwordConfirmedValidation.isNullOrEmpty() })
            }

            stateObserver.cancel()
        }


    @Test
    fun `passwordConfirmed update hides validation message when confirmPassword equals`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            viewModel.setPassword(VALID_PASSWORD)
            clearMocks(stateObserver)

            viewModel.setPasswordConfirmed(VALID_PASSWORD)

            verify { stateObserver.onEach(match { it.passwordConfirmedValidation.isNullOrEmpty() }) }
            stateObserver.cancel()
        }


    @Test
    fun `password update enables submit button when both password are correct`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            viewModel.setPasswordConfirmed(VALID_PASSWORD)
            clearMocks(stateObserver)

            viewModel.setPassword(VALID_PASSWORD)

            verify {
                stateObserver.onEach(match {
                    it.isSubmitEnabled
                })
            }

            stateObserver.cancel()
        }


    @Test
    fun `password update disables submit button when password or password confirmed is not correct`() =
        runTest(UnconfinedTestDispatcher()) {
            val uiStateObserver = viewModel.uiState.mockkObserver(this)
            viewModel.setPassword(VALID_PASSWORD)
            viewModel.setPasswordConfirmed(VALID_PASSWORD)
            clearMocks(uiStateObserver)

            viewModel.setPassword(INVALID_PASSWORD)

            verifyAll {
                uiStateObserver.onEach(match { !it.isSubmitEnabled })
            }
            uiStateObserver.cancel()
        }


    @Test
    fun `passwordConfirm update enables submit button when both passwords are correct`() =
        runTest(UnconfinedTestDispatcher()) {
            val uiStateObserver = viewModel.uiState.mockkObserver(this)
            viewModel.setPassword(VALID_PASSWORD)
            clearMocks(uiStateObserver)

            viewModel.setPasswordConfirmed(VALID_PASSWORD)

            verifyAll {
                uiStateObserver.onEach(match { it.isSubmitEnabled })
            }
            uiStateObserver.cancel()
        }


    @Test
    fun `passwordConfirm update disables submit button when password or password confirm is invalid`() =
        runTest(UnconfinedTestDispatcher()) {
            val uiStateObserver = viewModel.uiState.mockkObserver(this)
            viewModel.setPassword(VALID_PASSWORD)
            viewModel.setPasswordConfirmed(VALID_PASSWORD)
            clearMocks(uiStateObserver)

            viewModel.setPasswordConfirmed(INVALID_PASSWORD)

            verifyAll {
                uiStateObserver.onEach(match { !it.isSubmitEnabled })
            }
            uiStateObserver.cancel()
        }


    @Test
    fun `submit() shows and then doesn't hide the preloader when password successfully changed`() =
        runTest(UnconfinedTestDispatcher()) {
            val uiStateObserver = viewModel.uiState.mockkObserver(this)
            coEvery { passwordUpdateUseCase.invoke(any(), any()) } returns flowOf(Unit)

            viewModel.submit()

            verifyAll { uiStateObserver.onEach(match { it.preloader }) }
            uiStateObserver.cancel()
        }


    @Test
    fun `submit() shows and then hides the preloader when there were errors during password change`() =
        // Using runBlockingTest due to this issue: https://github.com/Kotlin/kotlinx.coroutines/issues/3367
        // TODO: when fixed, change to runTest(UnconfinedTestDispatcher()) {
        runTest(UnconfinedTestDispatcher()) {  //runTest(UnconfinedTestDispatcher()) {
            val uiStateObserver = viewModel.uiState.mockkObserver(this, true)
            coEvery { passwordUpdateUseCase.invoke(any(), any()) } returns flow {
                yield()
                throw TestException()
            }

            viewModel.submit()

            verifyOrder {
                uiStateObserver.onEach(match { it.preloader })
                uiStateObserver.onEach(match { !it.preloader })
            }

            uiStateObserver.cancel()
        }


    @Test
    fun `submit() executes password update when password is valid`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.setPassword(VALID_PASSWORD)
            coEvery {
                passwordUpdateUseCase.invoke(DEFAULT_USER_ID, VALID_PASSWORD)
            } returns flow { emit(Unit) }

            viewModel.submit()

            coVerifyAll {
                passwordUpdateUseCase.invoke(DEFAULT_USER_ID, VALID_PASSWORD)
            }
        }


    @Test
    fun `submit() shows error message when there were some errors`() =
        runTest(UnconfinedTestDispatcher()) {
            val uiEffectObserver = viewModel.uiEffect.mockkObserver(this)
            viewModel.setPassword(INVALID_PASSWORD)
            coEvery {
                passwordUpdateUseCase.invoke(any(), any())
            } returns flow { throw TestException() }

            viewModel.submit()

            verifyAll {
                uiEffectObserver.onEach(match {
                    it is UserPasswordChangeViewModel.UiEffect.Message && !it.text.isNullOrEmpty()
                })
            }

            uiEffectObserver.cancel()
        }


    @Test
    fun `submit() closes the screen when there were no errors`() =
        runTest(UnconfinedTestDispatcher()) {
            val uiEffectObserver = viewModel.uiEffect.mockkObserver(this)
            viewModel.setPassword(INVALID_PASSWORD)
            coEvery { passwordUpdateUseCase.invoke(any(), any()) } returns flow { emit(Unit) }

            viewModel.submit()

            verify {
                uiEffectObserver.onEach(match {
                    it is Routing && it.command is Back
                })
            }

            uiEffectObserver.cancel()
        }


    private fun setupViewModel() {
        appResources = mockk()
        passwordUpdateUseCase = mockk()
        viewModel = UserPasswordChangeViewModel(
            DEFAULT_USER_ID,
            SavedStateHandle(),
            passwordUpdateUseCase,
            appResources
        )
        every { appResources.getStringResource(R.string.password_validation_message) } returns "Invalid password"
        every { appResources.getStringResource(R.string.password_confirmation_validation_message) } returns "Password confirmation differs"
        every { appResources.getStringResource(R.string.common_communication_error) } returns "Common communication error"
    }


    companion object {

        private const val VALID_PASSWORD = "V@lid001"
        private const val INVALID_PASSWORD = "short"
        private const val DEFAULT_USER_ID = "a312b3ee-84c2-11eb-8dcd-0242ac130003"

    }
}