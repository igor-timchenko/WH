package ru.contlog.mobile.helper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager.findFragmentById(binding.fragmentContainerView.id) as NavHostFragment
        val navController = navHostFragment.navController

        if (viewModel.apiAuthData != null) {
            navController.navigate(R.id.action_loginFragment_to_workSitesFragment)
        }

        binding.versionLabel.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
    }
}