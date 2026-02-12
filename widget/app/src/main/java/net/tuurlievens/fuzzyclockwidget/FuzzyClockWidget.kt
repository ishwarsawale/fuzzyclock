package net.tuurlievens.fuzzyclockwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import java.util.Calendar

class FuzzyClockWidget : AppWidgetProvider() {

    companion object {
        const val ConfigTag = "CONFIG"

        private const val ACTION_ALARM_TICK =
            "net.tuurlievens.fuzzyclockwidget.ACTION_ALARM_TICK"

        private const val ALARM_REQUEST_CODE = 1001
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            UpdateWidgetService.updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextMinute(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelAlarm(context)
        context.stopService(Intent(context, UpdateWidgetService::class.java))
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context == null || intent == null) return

        // 1) Our self-rescheduling minute tick
        if (intent.action == ACTION_ALARM_TICK) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FuzzyClockWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)

            onUpdate(context, appWidgetManager, ids)

            // IMPORTANT: schedule the next minute again (otherwise you update only once)
            scheduleNextMinute(context)
            return
        }

        // 2) TIME_TICK (may not arrive from manifest, but harmless if it does)
        if (intent.action == Intent.ACTION_TIME_TICK) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FuzzyClockWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, ids)
            return
        }

        val extras = intent.extras

        // 3) Open config panel again
        if (intent.action?.contains(ConfigTag) == true) {
            val appWidgetId = extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val i = Intent(context, FuzzyClockWidgetConfigureActivity::class.java)
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(i)
            Log.i("ALARM", "open $appWidgetId config")
            return
        }

        // 4) Rerender widget for explicit update intents (tap, launcher, etc.)
        if (extras != null) {
            val appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val manager = AppWidgetManager.getInstance(context)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                UpdateWidgetService.updateWidget(context, manager, appWidgetId)
            }

            // Highlight config button when interacting with widget
            if (extras.getBoolean("manual", false)) {
                val view = RemoteViews(context.packageName, R.layout.fuzzy_clock_widget)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, FuzzyClockWidget::class.java)
                )

                view.setInt(R.id.configbtn, "setBackgroundColor", 0x44888888.toInt())
                appWidgetManager.updateAppWidget(appWidgetIds, view)

                Handler(Looper.getMainLooper()).postDelayed({
                    view.setInt(R.id.configbtn, "setBackgroundColor", Color.TRANSPARENT)
                    appWidgetManager.updateAppWidget(appWidgetIds, view)
                }, 300)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            FuzzyClockWidgetConfigureActivity.deletePrefs(context, appWidgetId)
        }
    }

    private fun scheduleNextMinute(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, 1)
        }

        val intent = Intent(context, FuzzyClockWidget::class.java).apply {
            action = ACTION_ALARM_TICK
        }

        val flags = PendingIntent.FLAG_CANCEL_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val pi = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            flags
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else {
            manager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }

        Log.i("ALARM", "Next minute scheduled: ${cal.timeInMillis}")
    }

    private fun cancelAlarm(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, FuzzyClockWidget::class.java).apply {
            action = ACTION_ALARM_TICK
        }

        val flags = PendingIntent.FLAG_NO_CREATE or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val pi = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags)
        if (pi != null) manager.cancel(pi)
    }
}
