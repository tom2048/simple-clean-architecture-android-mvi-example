package com.example.simplecleanarchitecture.users.ui.userlist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.simplecleanarchitecture.R
import com.example.simplecleanarchitecture.RouterScreen
import com.example.simplecleanarchitecture.core.lib.DefaultTestHelper
import com.example.simplecleanarchitecture.core.lib.TestException
import com.example.simplecleanarchitecture.core.lib.TestHelper
import com.example.simplecleanarchitecture.core.lib.resources.AppResources
import com.example.simplecleanarchitecture.core.lib.schedulers.MainCoroutineRule
import com.example.simplecleanarchitecture.core.lib.utils.mockkObserver
import com.example.simplecleanarchitecture.core.repository.model.UserDetails
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiEffect.Message
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiEffect.Routing
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel.UiEffect.UserActionConfirmation
import com.example.simplecleanarchitecture.users.usecase.user.UserDeleteUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserShowListUseCase
import com.github.terrakok.cicerone.Forward
import io.mockk.called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UserListViewModelTest : TestHelper by DefaultTestHelper() {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: UserListViewModel

    private lateinit var userShowListUseCase: UserShowListUseCase
    private lateinit var userDeleteUseCase: UserDeleteUseCase

    private lateinit var appResources: AppResources


    @Test
    fun `loadUsers() provides the list when the proper data is loaded`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            coEvery { userShowListUseCase.invoke() } returns flowOf(DEFAULT_USER_LIST)

            viewModel.loadUsers()

            verify {
                stateObserver.onEach(match { it.userList.map { it.user } == DEFAULT_USER_LIST })
            }
            stateObserver.cancel()
        }


    @Test
    fun `loadUsers() shows the error dialog when there is an error while loading the data`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            val effectObserver = viewModel.uiEffect.mockkObserver(this)
            val expectedMessage =
                appResources.getStringResource(R.string.common_communication_error)
            coEvery { userShowListUseCase.invoke() } returns flow {
                throw TestException()
            }

            viewModel.loadUsers()

            verify {
                stateObserver wasNot called
                effectObserver.onEach(match { it is Message && it.text == expectedMessage })
            }
            stateObserver.cancel()
            effectObserver.cancel()
        }


    @Test
    fun `loadUsers() shows and then hides the preloader when the proper data is being loaded`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            coEvery { userShowListUseCase.invoke() } returns flow {
                yield()
                emit(DEFAULT_USER_LIST)
            }

            viewModel.loadUsers()

            verifyOrder {
                stateObserver.onEach(match { it.preloader })
                stateObserver.onEach(match { !it.preloader })
            }
            stateObserver.cancel()
        }


    @Test
    fun `loadUsers() shows and then hides the preloader when there is an error while loading the data`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            coEvery { userShowListUseCase.invoke() } returns flow {
                yield()
                throw TestException()
            }

            viewModel.loadUsers()

            verifyOrder {
                stateObserver.onEach(match { it.preloader })
                stateObserver.onEach(match { !it.preloader })
            }
            stateObserver.cancel()
        }


    @Test
    fun `addNewUser() opens the user edit form`() =
        runTest(UnconfinedTestDispatcher()) {
            val effectObserver = viewModel.uiEffect.mockkObserver(this)

            viewModel.addNewUser()

            verify(exactly = 1) {
                effectObserver.onEach(match { it is Routing && it.command is Forward && (it.command as Forward).screen is RouterScreen.UserEditScreen })
            }
            effectObserver.cancel()
        }


    @Test
    fun `Given user id, when editUser(), then edit user form is opened`() =
        runTest(UnconfinedTestDispatcher()) {
            val effectObserver = viewModel.uiEffect.mockkObserver(this)

            viewModel.editUser(DEFAULT_USER_LIST.first().id!!)

            verify {
                effectObserver.onEach(match {
                    it is Routing && it.command is Forward &&
                            (it.command as Forward).let { command ->
                                command is Forward &&
                                        command.screen is RouterScreen.UserEditScreen &&
                                        (command.screen as RouterScreen.UserEditScreen).id == DEFAULT_USER_LIST.first().id
                            }
                })
            }
            effectObserver.cancel()
        }


    @Test
    fun `deleteUser() shows the confirmation dialog`() =
        runTest(UnconfinedTestDispatcher()) {
            val effectObserver = viewModel.uiEffect.mockkObserver(this)

            viewModel.deleteUser(DEFAULT_USER_LIST.first().id!!)

            verify(exactly = 1) {
                effectObserver.onEach(match { it is UserActionConfirmation && it.text == DEFAULT_USER_LIST.first().id })
            }
            effectObserver.cancel()
        }


    @Test
    fun `deleteUserConfirmed() shows the confirmation message when the user is deleted`() =
        runTest(UnconfinedTestDispatcher()) {
            val effectObserver = viewModel.uiEffect.mockkObserver(this)
            coEvery { userDeleteUseCase.invoke(any()) } returns flow {
                yield()
                emit(Unit)
            }
            coEvery { userShowListUseCase.invoke() } returns flow {
                yield()
                emit(DEFAULT_USER_LIST)
            }
            val expectedMessage =
                appResources.getStringResource(R.string.user_delete_success_message)

            viewModel.deleteUserConfirmed(DEFAULT_USER_LIST.first().id!!)

            verify(exactly = 1) {
                effectObserver.onEach(match { it is Message && it.text == expectedMessage })
            }
            effectObserver.cancel()
        }


    @Test
    fun `deleteUserConfirmed() updates the list when user is deleted`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            coEvery { userDeleteUseCase.invoke(any()) } returns flow { emit(Unit) }
            coEvery { userShowListUseCase.invoke() } returns flow {
                emit(
                    DEFAULT_USER_LIST.subList(
                        1,
                        2
                    )
                )
            }

            viewModel.deleteUserConfirmed(DEFAULT_USER_LIST.first().id!!)

            verify(exactly = 1) {
                stateObserver.onEach(match { it.userList == DEFAULT_USER_LIST.subList(1, 2) })
            }
            stateObserver.cancel()
        }


    @Test
    fun `deleteUserConfirmed() shows and then hides the preloader when user is properly deleted`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            coEvery { userDeleteUseCase.invoke(any()) } returns flow { emit(Unit) }
            coEvery { userShowListUseCase.invoke() } returns flow { emit(DEFAULT_USER_LIST) }

            viewModel.deleteUserConfirmed(DEFAULT_USER_LIST.first().id!!)

            verifyOrder {
                stateObserver.onEach(match { it.preloader })
                stateObserver.onEach(match { !it.preloader })
            }
            stateObserver.cancel()
        }


    @Test
    fun `deleteUserConfirmed() shows and then hides the preloader when there was an error while deleting the user`() =
        runTest(UnconfinedTestDispatcher()) {
            val stateObserver = viewModel.uiState.mockkObserver(this)
            coEvery { userDeleteUseCase.invoke(any()) } returns flow { throw TestException() }
            coEvery { userShowListUseCase.invoke() } returns flow { emit(DEFAULT_USER_LIST) }

            viewModel.deleteUserConfirmed(DEFAULT_USER_LIST.first().id!!)

            verifyOrder {
                stateObserver.onEach(match { it.preloader })
                stateObserver.onEach(match { !it.preloader })
            }
            stateObserver.cancel()
        }


    @Test
    fun `deleteUserConfirmed() displays an error message when there was an error while deleting the user`() =
        runTest(UnconfinedTestDispatcher()) {
            val effectObserver = viewModel.uiEffect.mockkObserver(this)
            coEvery { userDeleteUseCase.invoke(any()) } returns flow { throw TestException() }
            coEvery { userShowListUseCase.invoke() } returns flow { emit(DEFAULT_USER_LIST) }

            viewModel.deleteUserConfirmed(DEFAULT_USER_LIST.first().id!!)

            verify(exactly = 1) {
                effectObserver.onEach(match { it is Message })
            }
            effectObserver.cancel()
        }


    @Test
    fun `changeUserPassword() opens change password screen`() =
        runTest(UnconfinedTestDispatcher()) {
            val effectObserver = viewModel.uiEffect.mockkObserver(this)
            viewModel.changeUserPassword(DEFAULT_USER_LIST.first().id!!)

            verify(exactly = 1) {
                effectObserver.onEach(match {
                    it is Routing && it.command == Forward(
                        RouterScreen.UserPasswordChangeScreen(
                            DEFAULT_USER_LIST.first().id!!
                        ), true
                    )
                })
            }
            effectObserver.cancel()
        }


    @Before
    fun setUp() {
        prepareLifecycle()
        userShowListUseCase = mockk()
        userDeleteUseCase = mockk()
        appResources = mockk()

        viewModel = UserListViewModel(
            userShowListUseCase,
            userDeleteUseCase,
            appResources
        )

        every { appResources.getStringResource(R.string.common_communication_error) } returns "Test error message."
        every { appResources.getStringResource(R.string.user_delete_success_message) } returns "User deleted."
    }

    @After
    fun tearDown() {
        cleanUpLifecycle()
        invokeViewModelOnCleared(viewModel)
    }

    companion object {
        private val DEFAULT_USER_LIST = listOf(
            UserDetails(
                "a312b3ee-84c2-11eb-8dcd-0242ac130003",
                "Nickname1",
                "nickname1@test.com",
                "Test description 1"
            ),
            UserDetails(
                "3b04aacf-4320-48bb-8171-af512aae0894",
                "Nickname2",
                "nickname2@test.com",
                "Test description 1"
            ),
            UserDetails(
                "52408bc4-4cdf-49ef-ac54-364bfde3fbf0",
                "Nickname3",
                "nickname3@test.com",
                "Test description 1"
            )
        )
    }
}