package com.example.simplecleanarchitecture

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.example.simplecleanarchitecture.core.lib.resources.AppResources
import com.example.simplecleanarchitecture.core.lib.resources.AppResourcesDefault
import com.example.simplecleanarchitecture.core.repository.AssetsRepository
import com.example.simplecleanarchitecture.core.repository.AssetsRepositoryMemory
import com.example.simplecleanarchitecture.core.repository.FileStorageRepository
import com.example.simplecleanarchitecture.core.repository.StorageRepository
import com.example.simplecleanarchitecture.core.repository.UsersRepository
import com.example.simplecleanarchitecture.core.repository.UsersRepositoryMemory
import com.example.simplecleanarchitecture.users.ui.passwordchange.UserPasswordChangeViewModel
import com.example.simplecleanarchitecture.users.ui.useredit.UserEditViewModel
import com.example.simplecleanarchitecture.users.ui.userlist.UserListViewModel
import com.example.simplecleanarchitecture.users.usecase.user.UserAddAttachmentUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserAddAttachmentUseCaseDefault
import com.example.simplecleanarchitecture.users.usecase.user.UserDeleteUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserDeleteUseCaseDefault
import com.example.simplecleanarchitecture.users.usecase.user.UserGetAttachmentUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserGetAttachmentUseCaseDefault
import com.example.simplecleanarchitecture.users.usecase.user.UserPasswordUpdateUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserPasswordUpdateUseCaseDefault
import com.example.simplecleanarchitecture.users.usecase.user.UserShowDetailsUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserShowDetailsUseCaseDefault
import com.example.simplecleanarchitecture.users.usecase.user.UserShowListUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserShowListUseCaseDefault
import com.example.simplecleanarchitecture.users.usecase.user.UserUpdateUseCase
import com.example.simplecleanarchitecture.users.usecase.user.UserUpdateUseCaseDefault
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.NavigatorHolder
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MainApplication)
            modules(
                listOf(
                    viewModelModule(),
                    repositoryModule(),
                    useCaseModule(),
                    navigationModule(),
                    otherModule()
                )
            )
        }
    }

    private fun viewModelModule() = module {
        viewModel { UserListViewModel(get(), get(), get()) }
        viewModel { (userId: String, state: SavedStateHandle) ->
            UserEditViewModel(
                userId,
                state,
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        viewModel { (userId: String, state: SavedStateHandle) ->
            UserPasswordChangeViewModel(
                userId,
                state,
                get(),
                get()
            )
        }
    }

    private fun repositoryModule() = module {
        single<UsersRepository> { UsersRepositoryMemory() }
        single<AssetsRepository> { AssetsRepositoryMemory() }
        single<StorageRepository> { FileStorageRepository(get()) }
    }

    private fun useCaseModule() = module {
        single<UserShowListUseCase> { UserShowListUseCaseDefault(get()) }
        single<UserShowDetailsUseCase> { UserShowDetailsUseCaseDefault(get(), get()) }
        single<UserUpdateUseCase> { UserUpdateUseCaseDefault(get(), get(), get()) }
        single<UserAddAttachmentUseCase> { UserAddAttachmentUseCaseDefault(get(), get()) }
        single<UserGetAttachmentUseCase> { UserGetAttachmentUseCaseDefault(get()) }
        single<UserDeleteUseCase> { UserDeleteUseCaseDefault(get()) }
        single<UserPasswordUpdateUseCase> { UserPasswordUpdateUseCaseDefault(get()) }
    }

    private fun navigationModule() = module {
        single<Cicerone<MainRouter>> { Cicerone.create(MainRouter()) }
        factory<MainRouter> { get<Cicerone<MainRouter>>().router }
        factory<NavigatorHolder> { get<Cicerone<MainRouter>>().getNavigatorHolder() }
    }

    private fun otherModule() = module {
        single<AppResources> { AppResourcesDefault(get()) }
    }

}