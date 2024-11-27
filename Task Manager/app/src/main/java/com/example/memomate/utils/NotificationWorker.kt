package com.example.memomate.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.memomate.R

class NotificationWorker(val context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    override fun doWork(): Result {
        val input = inputData.getString("name").toString()
        Log.d("NotificationWorker", "Worker executed with input: $input") // Log for debugging
        sendNotification(context, input)
        return Result.success()
    }

    private fun sendNotification(context: Context, input: String) {
        val channelId = "my_channel"
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.baseline_notifications)
            .setContentTitle("Note Alert!")
            .setContentText("Time to do $input")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val manager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Note Reminder", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "This channel sends note scheduling notifications"
            manager.createNotificationChannel(channel)
        }

        // Using a dynamic notification ID
        manager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }
}
