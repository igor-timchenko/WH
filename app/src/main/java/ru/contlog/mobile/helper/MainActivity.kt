package ru.contlog.mobile.helper

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.contlog.mobile.helper.databinding.ActivityMainBinding
import ru.contlog.mobile.helper.repo.AppPreferencesRepository
import ru.contlog.mobile.helper.vm.AppViewModel
import ru.contlog.mobile.helper.vm.factories.AppViewModelFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(AppPreferencesRepository(this))
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
                view.setBackgroundColor(getColor(R.color.statusBarColor))

                view.setPadding(0, statusBarInsets.top, 0, navigationBarInsets.bottom)
                insets
            }
        } else {
            window.statusBarColor = getColor(R.color.statusBarColor)
            binding.root.fitsSystemWindows = true
        }

        val navHostFragment = supportFragmentManager.findFragmentById(binding.fragmentContainerView.id) as NavHostFragment
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

//        binding.versionLabel.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
    }

    companion object {
        const val TAG = "Contlog.MainActivity"
    }
}