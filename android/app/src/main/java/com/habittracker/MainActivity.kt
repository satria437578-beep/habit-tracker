package com.habittracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.glance.appwidget.updateAll
import com.habittracker.ui.AppRoot
import com.habittracker.ui.AppViewModel
import com.habittracker.ui.HabitTheme
import com.habittracker.widget.HabitWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            HabitTheme {
                val vm: AppViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        HabitApp.repo.pull()
        lifecycleScope.launch { HabitWidget().updateAll(this@MainActivity) }
    }
}
