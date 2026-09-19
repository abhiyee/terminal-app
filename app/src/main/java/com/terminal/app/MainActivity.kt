package com.terminal.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.terminal.app.databinding.ActivityMainBinding
import com.terminal.app.ui.SettingsFragment
import com.terminal.app.ui.TerminalFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val terminalFragment = TerminalFragment()
    private val settingsFragment = SettingsFragment()
    private var activeFragment: Fragment = terminalFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            setupFragments()
        } else {
            restoreFragments(savedInstanceState)
        }

        setupBottomNavigation()
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, settingsFragment, "settings")
            .hide(settingsFragment)
            .add(R.id.fragment_container, terminalFragment, "terminal")
            .commit()
    }

    private fun restoreFragments(savedInstanceState: Bundle) {
        val terminal = supportFragmentManager.findFragmentByTag("terminal") as TerminalFragment
        val settings = supportFragmentManager.findFragmentByTag("settings") as SettingsFragment

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, settings, "settings")
            .add(R.id.fragment_container, terminal, "terminal")
            .hide(settings)
            .commit()

        activeFragment = terminal
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_terminal -> {
                    switchFragment(terminalFragment)
                    true
                }
                R.id.nav_settings -> {
                    switchFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (targetFragment == activeFragment) return

        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(targetFragment)
            .commit()

        activeFragment = targetFragment
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        supportFragmentManager.putFragment(outState, "terminal", terminalFragment)
        supportFragmentManager.putFragment(outState, "settings", settingsFragment)
    }
}
