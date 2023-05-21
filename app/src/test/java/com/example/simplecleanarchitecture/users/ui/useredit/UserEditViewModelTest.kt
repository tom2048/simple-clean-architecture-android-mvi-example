package com.example.simplecleanarchitecture.users.ui.useredit

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.example.simplecleanarchitecture.R
import com.example.simplecleanarchitecture.core.lib.DefaultTestHelper
import com.example.simplecleanarchitecture.core.lib.TestException
import com.example.simplecleanarchitecture.core.lib.TestHelper
import com.example.simplecleanarchitecture.core.lib.exception.ValidationException
import com.example.simplecleanarchitecture.core.lib.resources.AppResources
import com.example.simplecleanarchitecture.core.lib.schedulers.MainCoroutineRule
import com.example.simplecleanarchitecture.core.lib.utils.mockkObserver
import com.example.simplecleanarchitecture.core.lib.utils.mockkTest
import com.example.simplecleanarchitecture.core.model.User
import com.example.simplecleanarchitecture.users.ui.useredit.UserEditViewModel.UiEffect.Message
import com.example.simplecleanarchitecture.users.ui.useredit.UserEditViewModel.UiEffect.Routing
import com.example.simplecleanarchitecture.users.usecase.user.UserAddAttachmentUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserGetAttachmentUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserShowDetailsUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserUpdateUseCase
import com.github.terrakok.cicerone.Back
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import java.nio.charset.Charset

class UserEditViewModelTest : TestHelper by DefaultTestHelper() {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: UserEditViewModel

    private lateinit var userShowDetailsUseCase: UserShowDetailsUseCase
    private lateinit var userAddAttachmentUseCase: UserAddAttachmentUseCase
    private lateinit var userGetAttachmentUseCase: UserGetAttachmentUseCase
    private lateinit var userUpdateUseCase: UserUpdateUseCase
    private lateinit var appResources: AppResources

    @Test
    fun `submit() closes the form when there are no errors during save`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiEffect.mockkObserver(this) { observer ->
                coEvery { userUpdateUseCase.invoke(any()) } returns flow { emit(Unit) }

                viewModel.submit()

                verify {
                    observer.onEach(match {
                        it is Routing && it.command is Back
                    })
                }
            }
        }

    @Test
    fun `submit() doesn't close the form when there is an error during save`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiEffect.mockkObserver(this) { uiEffectObserver ->

                coEvery { userUpdateUseCase.invoke(any()) } returns flow {
                    throw ValidationException(listOf(Pair("test", "test")))
                }

                viewModel.submit()

                verify(exactly = 0) {
                    uiEffectObserver.onEach(match { it is Routing })
                }
            }
        }

    @Test
    fun `submit() displays an error message when there is an error during save`() =
        runTest(UnconfinedTestDispatcher()) {
            val uiEffectObserver = viewModel.uiEffect.mockkObserver(this)
            coEvery { userUpdateUseCase.invoke(any()) } returns flow {
                throw ValidationException(listOf(Pair("test", "test")))
            }

            viewModel.submit()

            verify {
                uiEffectObserver.onEach(match { it is Message && !it.text.isNullOrEmpty() })
            }
            uiEffectObserver.cancel()
        }

    @Test
    fun `submit() invokes update use case when there are correct user details set`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { userUpdateUseCase.invoke(any()) } returns flow { emit(Unit) }
            val testObserver = userUpdateUseCase(DEFAULT_USER).mockkTest()
            viewModel.setEmail(DEFAULT_USER.email)
            viewModel.setNickname(DEFAULT_USER.nickname)

            viewModel.submit()

            verify { testObserver.onCompletion() }
            coVerify { userUpdateUseCase.invoke(any()) }
        }

    @Test
    fun `setting nickname shows validation message when the nickname is invalid`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->
                viewModel.setNickname(INVALID_NICKNAME)

                verify {
                    uiStateObserver.onEach(match { !it.nicknameValidationError.isNullOrEmpty() })
                }
            }
        }

    @Test
    fun `setting email shows validation message when the email is invalid`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->
                viewModel.setEmail(INVALID_EMAIL)

                verify {
                    uiStateObserver.onEach(match { !it.emailValidationError.isNullOrEmpty() })
                }
            }
        }

    @Test
    fun `setting description shows validation error message when the description is invalid`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->
                viewModel.setDescription(INVALID_DESCRIPTION)

                verify {
                    uiStateObserver.onEach(match { !it.descriptionValidationError.isNullOrEmpty() })
                }
            }
        }

    @Test
    fun `setting all values enables the submit button when values are valid`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->
                viewModel.setNickname(VALID_NICKNAME)
                viewModel.setEmail(VALID_EMAIL)
                viewModel.setDescription(VALID_DESCRIPTION)

                verifySequence {
                    uiStateObserver.onEach(match { !it.isSubmitEnabled })
                    uiStateObserver.onEach(match { !it.isSubmitEnabled })
                    uiStateObserver.onEach(match { it.isSubmitEnabled })
                }
            }
        }

    @Test
    fun `loadDetails() loads user data when the data is properly loaded`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->

                coEvery { userShowDetailsUseCase.invoke(any()) } returns flowOf(DEFAULT_USER)

                viewModel.loadDetails()

                verify {
                    uiStateObserver.onEach(match {
                        it.nickname == DEFAULT_USER.nickname &&
                                it.email == DEFAULT_USER.email &&
                                it.description == DEFAULT_USER.description
                    })
                }
            }
        }

    @Test
    fun `loadDetails() shows and then hides the preloader when the data is properly loaded`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->

                coEvery { userShowDetailsUseCase.invoke(any()) } returns flow {
                    yield()
                    emit(DEFAULT_USER)
                }

                viewModel.loadDetails()

                verifySequence {
                    uiStateObserver.onEach(match { it.preloader })
                    uiStateObserver.onEach(match { !it.preloader })
                }
            }
        }

    @Test
    fun `loadDetails() shows and then hides the preloader when there is an error while loading data`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->

                coEvery { userShowDetailsUseCase.invoke(any()) } returns flow {
                    yield()
                    throw TestException()
                }

                viewModel.loadDetails()

                verifySequence {
                    uiStateObserver.onEach(match { it.preloader })
                    uiStateObserver.onEach(match { !it.preloader })
                }
            }
        }

    @Test
    fun `addAvatar() adds the image to the assets when correct data provided`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->

                val imageBytes = "data".toByteArray(Charset.defaultCharset())
                coEvery {
                    userAddAttachmentUseCase.invoke(any(), any(), any())
                } returns flowOf("KEY")
                coEvery { userGetAttachmentUseCase.invoke(any()) } returns flowOf(imageBytes)

                viewModel.addAvatar("test")

                verify {
                    uiStateObserver.onEach(match { it.avatar!!.bytes.contentEquals(imageBytes) })
                }
            }
        }

    @Test
    fun `addIdScan() adds the image to the assets when correct data provided`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->
                val imageBytes = "data".toByteArray(Charset.defaultCharset())
                coEvery {
                    userAddAttachmentUseCase.invoke(any(), any(), any())
                } returns flowOf("KEY")
                coEvery { userGetAttachmentUseCase.invoke(any()) } returns flowOf(imageBytes)

                viewModel.addIdScan("test")

                verify {
                    uiStateObserver.onEach(match { it.idScan!!.bytes.contentEquals(imageBytes) })
                }
            }
        }

    @Test
    fun `submit() shows the preloader and doesn't hide it when the data is correctly saved`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->

                coEvery { userUpdateUseCase.invoke(any()) } returns flowOf(Unit)

                viewModel.submit()

                verify {
                    uiStateObserver.onEach(match { it.preloader })
                }
            }
        }

    @Test
    fun `submit() shows and then hides preloader when there is an error while loading the data`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiState.mockkObserver(this) { uiStateObserver ->
                coEvery { userUpdateUseCase.invoke(any()) } returns flow {
                    yield()
                    throw TestException()
                }

                viewModel.submit()

                verifySequence {
                    uiStateObserver.onEach(match { it.preloader })
                    uiStateObserver.onEach(match { !it.preloader })
                }
            }
        }

    @Test
    fun `cancel() closes screen`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel.uiEffect.mockkObserver(this) { uiEffectObserver ->

                viewModel.cancel()

                verify {
                    uiEffectObserver.onEach(match { it is Routing && it.command is Back })
                }
            }
        }

    @Before
    fun setUp() {
        prepareLifecycle()

        userShowDetailsUseCase = mockk()
        userAddAttachmentUseCase = mockk()
        userGetAttachmentUseCase = mockk()
        userUpdateUseCase = mockk()
        appResources = mockk()

        every { appResources.getStringResource(R.string.nickname_validation_message) } returns "Validation error"
        every { appResources.getStringResource(R.string.email_validation_message) } returns "Validation error"
        every { appResources.getStringResource(R.string.description_validation_message) } returns "Validation error"
        every { appResources.getStringResource(R.string.common_communication_error) } returns "Edit user"
        every { appResources.getStringResource(R.string.user_edit_header) } returns "Edit user"
        every { appResources.getStringResource(R.string.user_add_header) } returns "Add user"

        viewModel = UserEditViewModel(
            DEFAULT_USER.id!!,
            SavedStateHandle(),
            userShowDetailsUseCase,
            userAddAttachmentUseCase,
            userGetAttachmentUseCase,
            userUpdateUseCase,
            appResources
        )
    }

    @After
    fun tearDown() {
        cleanUpLifecycle()

        invokeViewModelOnCleared(viewModel)
    }

    companion object {
        private val DEFAULT_USER =
            User("a312b3ee-84c2-11eb-8dcd-0242ac130003", "Testnick", "test@test.com", "")
        private const val VALID_NICKNAME = "Nick1"
        private const val INVALID_NICKNAME = "TooLongNicknameOfTheUser"
        private const val VALID_EMAIL = "test@test.com"
        private const val INVALID_EMAIL = "test@test"
        private const val VALID_DESCRIPTION = "Test description"
        private const val INVALID_DESCRIPTION = "@test@"
    }
}