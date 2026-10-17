package com.habittracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.habittracker.HabitApp
import com.habittracker.data.Habit

private val habitIdKey = ActionParameters.Key<String>("habitId")
private val onColor = ColorProvider(Color(0xFF1B1B2B))
private val dimColor = ColorProvider(Color(0xFF6B6F85))
private val okColor = ColorProvider(Color(0xFF00A88E))

class HabitWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val habits = HabitApp.repo.todayHabitsOnce()
        provideContent { Content(habits) }
    }
}

@Composable
private fun Content(habits: List<Pair<Habit, Boolean>>) {
    Column(GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFFF4F5FC))).padding(12.dp)) {
        Text("Hari Ini", style = TextStyle(color = onColor, fontWeight = FontWeight.Bold))
        if (habits.isEmpty()) {
            Text("Belum ada habit", style = TextStyle(color = dimColor))
        }
        habits.take(7).forEach { (habit, done) ->
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)
                    .clickable(actionRunCallback<CheckInAction>(actionParametersOf(habitIdKey to habit.id))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (done) "✓ " else "○ ", style = TextStyle(color = if (done) okColor else dimColor))
                Text(habit.name, style = TextStyle(color = onColor))
            }
        }
    }
}

class CheckInAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[habitIdKey] ?: return
        HabitApp.repo.checkInNow(id)
        HabitWidget().update(context, glanceId)
    }
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}
