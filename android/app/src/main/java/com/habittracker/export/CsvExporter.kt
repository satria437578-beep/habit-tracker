package com.habittracker.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.habittracker.data.Habit
import com.habittracker.ui.UiState
import java.io.File

object CsvExporter {
    fun export(context: Context, state: UiState) {
        val habits: Map<String, Habit> = (state.today.map { it.habit } + state.archived).associateBy { it.id }
        val sb = StringBuilder("habit,category,date,value,duration_seconds,note,done\n")
        state.checkinsByHabit.values.flatten()
            .sortedWith(compareBy({ it.habitId }, { it.date }))
            .forEach { c ->
                val h = habits[c.habitId]
                sb.append(csv(h?.name ?: c.habitId)).append(',')
                    .append(csv(h?.category.orEmpty())).append(',')
                    .append(c.date).append(',')
                    .append(c.value).append(',')
                    .append(c.duration).append(',')
                    .append(csv(c.note)).append(',')
                    .append(if (c.value >= 1.0 || c.duration > 0) 1 else 0).append('\n')
            }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "habit_export.csv")
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Export Habit Tracker")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(share, "Bagikan CSV").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value
}
