package com.example.trackyourhealth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.trackyourhealth.databinding.ActivityMainBinding
import com.example.trackyourhealth.viewModel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setupWithNavController(getNavController())

        getNavController().addOnDestinationChangedListener {_, destination, _ ->
            when (destination.id) {
                R.id.destination_auth -> binding.toolbar.navigationIcon = null
                R.id.destination_list -> binding.toolbar.navigationIcon = null
            }
        }
    }

    private fun getNavHost() =
        supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment

    private fun getNavController() = getNavHost().navController
}