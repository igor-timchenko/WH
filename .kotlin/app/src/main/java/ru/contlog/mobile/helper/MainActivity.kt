package ru.contlog.mobile.helper

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.contlog.mobile.helper.databinding.ActivityMainBinding
import ru.contlog.mobile.helper.repo.Api
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.vm.AppViewModel
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this))
    }

    private val connectivityManager: ConnectivityManager by lazy {
        getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val networkQueryCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            super.onLost(network)
            binding.root.post {
                viewModel.setInternetAvailableState(available = false)
            }
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            binding.root.post {
                viewModel.setInternetAvailableState(available = true)
            }
        }

        override fun onUnavailable() {
            super.onUnavailable()
            binding.root.post {
                viewModel.setInternetAvailableState(available = false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
                val navigationBarInsets = insets.getInsets(WindowInsets.Type.navigationBars())
                view.setBackgroundColor(ContextCompat.getColor(this, R.color.statusBarColor))
                view.setPadding(0, statusBarInsets.top, 0, navigationBarInsets.bottom)
                insets
            }
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.statusBarColor)
            binding.root.fitsSystemWindows = true
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.fragmentContainerView.id) as NavHostFragment
        val navController = navHostFragment.navController

        if (viewModel.apiAuthData != null) {
            navController.navigate(R.id.action_loginFragment_to_workSitesFragment)
        }

        binding.bottomNavigation.setOnItemSelectedListener { mi ->
            when (mi.itemId) {
                R.id.mHome -> {
                    if ((navController.currentDestination?.id ?: false) != R.id.workSitesFragment) {
                        navController.navigate(R.id.action_profileFragment_to_workSitesFragment)
                    }
                }
                R.id.mProfile -> {
                    if ((navController.currentDestination?.id ?: false) != R.id.profileFragment) {
                        navController.navigate(R.id.action_workSitesFragment_to_profileFragment)
                    }
                }
            }
            true
        }

        lifecycleScope.launch(Dispatchers.IO) {
            navController.currentBackStackEntryFlow.collect { entry ->
                val navbarVisibility = when (entry.destination.id) {
                    R.id.workSitesFragment, R.id.profileFragment -> View.VISIBLE
                    else -> View.GONE
                }
                launch(Dispatchers.Main) {
                    binding.bottomNavigation.visibility = navbarVisibility
                    when (entry.destination.id) {
                        R.id.workSitesFragment -> binding.bottomNavigation.selectedItemId = R.id.mHome
                        R.id.profileFragment -> binding.bottomNavigation.selectedItemId = R.id.mProfile
                    }
                }
            }
        }

        val networkQueryRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(networkQueryRequest, networkQueryCallback)

        viewModel.internetAvailable.observe(this) { internetAvailable ->
            binding.noInternetOverlay.visibility = if (internetAvailable) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroy() {
        connectivityManager.unregisterNetworkCallback(networkQueryCallback)
        super.onDestroy()
    }

    companion object {
        const val TAG = "Contlog.MainActivity"
    }
}