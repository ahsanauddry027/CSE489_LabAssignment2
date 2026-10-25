package com.example.labassignment2_cse489

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewGroupCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.labassignment2_cse489.databinding.ActivityMainBinding
import com.example.labassignment2_cse489.util.applyWindowInsetsPadding

/**
 * The single host activity for the four drawer screens.
 *
 * Layout: a [DrawerLayout] containing the top app bar + a NavHostFragment, and a NavigationView
 * (the drawer). Navigation's NavigationUI wires them together: choosing a drawer item navigates
 * to the fragment with the same ID, the toolbar title follows the current destination and the
 * current item stays highlighted.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * Back closes an open drawer before doing anything else. It is enabled only while the drawer
     * is open, so in every other case Back keeps its normal behaviour (and its predictive-back
     * animation). With targetSdk 36+ on Android 16+ the old onBackPressed() is never called, so
     * this OnBackPressedDispatcher callback is the supported way to customise Back.
     */
    private val closeDrawerOnBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setUpNavigation()
        setUpBackHandling()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // After rotation DrawerLayout re-opens a drawer that was open, without notifying its
        // listeners, so sync the Back callback with the restored state here.
        closeDrawerOnBack.isEnabled = binding.drawerLayout.isDrawerOpen(GravityCompat.START)
    }

    private fun setUpNavigation() {
        // With FragmentContainerView the NavController must be read from the NavHostFragment.
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Every drawer item is a top-level destination: all four show the hamburger icon (which
        // opens the drawer) instead of an Up arrow. Building the configuration from the menu
        // keeps this set in sync with drawer_menu.xml.
        val appBarConfiguration = AppBarConfiguration(binding.navView.menu, binding.drawerLayout)
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }

    private fun setUpBackHandling() {
        onBackPressedDispatcher.addCallback(this, closeDrawerOnBack)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                closeDrawerOnBack.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: View) {
                closeDrawerOnBack.isEnabled = false
            }
        })
    }

    private fun applyWindowInsets() {
        // Deliver insets to every child the same way on old and new Android versions.
        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        // The app bar's background extends behind the status bar; its content sits below it.
        binding.appBar.applyWindowInsetsPadding(left = true, top = true, right = true)
        // Screens stay clear of the navigation bar (bottom in portrait, a side in landscape).
        binding.navHostFragment.applyWindowInsetsPadding(left = true, right = true, bottom = true)
        // Keep the drawer header text below the status bar while the drawer is open.
        binding.navView.getHeaderView(0).applyWindowInsetsPadding(top = true)
    }
}
