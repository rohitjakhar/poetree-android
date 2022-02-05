package com.mambo.poetree.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mambo.core.extensions.isNotNullOrEmpty
import com.mambo.core.utils.IntentExtras
import com.mambo.core.viewmodel.MainViewModel
import com.mambo.core.work.InteractReminderWork
import com.mambo.poetree.R
import com.mambo.poetree.databinding.ActivityMainBinding
import com.mambobryan.navigation.Destinations
import com.mambobryan.navigation.extensions.getDeeplink
import com.mambobryan.navigation.extensions.getNavOptionsPopUpTo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.util.*
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment

        navController = navHostFragment.navController

        binding.navBottomMain.setupWithNavController(navController)

//        viewModel.connection.observe(this) { isNetworkAvailable ->
//            binding.layoutConnection.constraintLayoutNetworkStatus.isVisible = !isNetworkAvailable
//        }

        lifecycleScope.launchWhenStarted {
            viewModel.events.collect { event ->
                when (event) {
                    MainViewModel.MainEvent.SetupDailyInteractionReminder -> setupDailyInteractionReminder()
                }
            }
        }

        setUpDestinationListener()

        initNavigation()

        handleNotificationData()

    }

    /**
     * method to handle the data content on clicking of notification if both notification and data payload are sent
     */
    private fun handleNotificationData() {

        val extras = intent.extras

        if (extras != null) {

            val type = extras.getString(IntentExtras.TYPE)
            val poem = extras.getString(IntentExtras.POEM)

            Log.i("SomeThing", "TYPE: $type ")
            Log.i("SomeThing", "POEM: $poem ")

            val uri: String? =
                if (type != null)
                    when (type) {
                        "comment" -> {
                            getString(Destinations.COMMENTS)
                        }
                        else -> {
                            getString(Destinations.POEM)
                        }
                    }
                else null

            if (viewModel.isValidPoem(poem) && uri.isNotNullOrEmpty())
                navController.navigate(Uri.parse(uri))

        }

    }

    private fun initNavigation() {

        if (!viewModel.isOnBoarded) {
            navigateToOnBoarding()
            return
        }

        if (!viewModel.isLoggedIn) {
            navigateToAuth()
            return
        }

        if (!viewModel.isUserSetup) {
            navigateToSetup()
            return
        }

        navigateToFeeds()

    }

    private fun navigateToOnBoarding() {
        navController.navigate(getDeeplink(Destinations.ON_BOARDING), getLoadingNavOptions())
    }

    private fun navigateToFeeds() {
        navController.navigate(getDeeplink(Destinations.FEED), getLoadingNavOptions())
    }

    private fun navigateToSetup() {
        navController.navigate(getDeeplink(Destinations.SETUP), getLoadingNavOptions())
    }

    private fun navigateToAuth() {
        navController.navigate(getDeeplink(Destinations.LANDING), getLoadingNavOptions())
    }

    private fun getLoadingNavOptions() = getNavOptionsPopUpTo(R.id.flow_loading)

    override fun onBackPressed() {
        when (getDestinationId()) {

            R.id.feedFragment, R.id.landingFragment, R.id.setupFragment -> {
                finish()
            }

            else -> {
                super.onBackPressed()
            }

        }

    }

    private fun setUpDestinationListener() {
        val destinationChangedListener =
            NavController.OnDestinationChangedListener { _: NavController?, destination: NavDestination, _: Bundle? ->
                when (destination.id) {
                    R.id.feedFragment,
                    R.id.exploreFragment,
                    R.id.bookmarksFragment,
                    R.id.libraryFragment -> {
                        showBottomNavigation()
                    }

                    else -> {
                        hideBottomNavigation()
                    }
                }
            }
        navController.addOnDestinationChangedListener(destinationChangedListener)
    }

    private fun getDestinationId(): Int? {
        return navController.currentDestination?.id
    }

    private fun hideBottomNavigation() {
        binding.apply {
            navBottomMain.visibility = View.GONE
        }
    }

    private fun showBottomNavigation() {
        binding.apply {
            navBottomMain.visibility = View.VISIBLE
        }
    }

    private fun setupDailyInteractionReminder() {

        val then = Calendar.getInstance()
        val now = Calendar.getInstance()

        then.set(Calendar.HOUR_OF_DAY, 17)         // set hour
        then.set(Calendar.MINUTE, 15)             // set minute
        then.set(Calendar.SECOND, 0)             // set seconds

        val time = then.timeInMillis - now.timeInMillis

        val work =
            PeriodicWorkRequestBuilder<InteractReminderWork>(24, TimeUnit.HOURS)
                .setInitialDelay(time, TimeUnit.MILLISECONDS)
                .addTag(InteractReminderWork.TAG)
                .build()

        WorkManager.getInstance(this).enqueue(work)

    }
}