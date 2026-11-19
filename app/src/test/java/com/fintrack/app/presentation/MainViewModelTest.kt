package com.fintrack.app.presentation

import com.fintrack.app.core.data.local.LocalAuthManager
import com.fintrack.app.core.domain.repository.NetworkRepository
import com.fintrack.app.core.profile.AccountMode
import com.fintrack.app.core.profile.ProfileManager
import com.fintrack.app.core.profile.ProfileState
import kotlinx.coroutines.flow.MutableStateFlow
import com.fintrack.app.presentation.home.CurrentUserObserver
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val auth: FirebaseAuth = mock()
    private val localAuthManager: LocalAuthManager = mock()
    private val profileManager: ProfileManager = mock()
    private lateinit var viewModel: MainViewModel

    private fun withMode(mode: AccountMode) {
        whenever(profileManager.state).thenReturn(MutableStateFlow(ProfileState("p", mode, "p", true)))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        withMode(AccountMode.ACCOUNT)
        val network: NetworkRepository = mock()
        whenever(network.observeNetworkConnectivity()).thenReturn(flowOf(true))
        viewModel = MainViewModel(
            networkRepository = network,
            auth = auth,
            currentUserObserver = mock<CurrentUserObserver>(),
            localAuthManager = localAuthManager,
            profileManager = profileManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `forgot PIN signs out and clears local auth before unlocking`() = runTest {
        viewModel.setAppLocked(true)

        viewModel.onForgotPin()

        inOrder(auth, profileManager, localAuthManager) {
            verify(auth).signOut()
            verify(profileManager).signOutToFreshState()
            verify(localAuthManager).clearLocalAuth()
        }
        assertFalse(viewModel.isAppLocked.value)
    }

    @Test
    fun `forgot PIN for a local-only user never unlocks or signs out`() = runTest {
        withMode(AccountMode.LOCAL_ONLY)
        viewModel.setAppLocked(true)

        viewModel.onForgotPin()

        assertTrue(viewModel.isAppLocked.value)
        assertTrue(viewModel.showLocalForgotPin.value)
        verify(auth, never()).signOut()
        verify(localAuthManager, never()).clearLocalAuth()
    }

    @Test
    fun `setAppLocked alone does not sign out`() = runTest {
        viewModel.setAppLocked(true)
        assertTrue(viewModel.isAppLocked.value)

        viewModel.setAppLocked(false)

        verify(auth, never()).signOut()
    }
}
