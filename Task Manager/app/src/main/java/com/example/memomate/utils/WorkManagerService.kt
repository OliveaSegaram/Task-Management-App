package com.example.memomate.utils

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class WorkManagerService(val context: Context) {
    fun schedule(name: String, delay: Long) {
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .addTag(name)  // Use the task title or ID as a unique tag
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)  // Delay in milliseconds
            .setInputData(workDataOf("name" to name))
            .build()

        // This will cancel any existing work with the same tag and replace it
        WorkManager.getInstance(context).enqueueUniqueWork(
            name,  // Unique name/tag for the task (could be the task ID)
            ExistingWorkPolicy.REPLACE,  // Replace if there's already a job with the same name
            request
        )
        Log.d("WorkManagerService", "Work scheduled for $name with delay $delay ms")
    }

    fun cancel(name: String) {
        // This will cancel the unique work with the provided name
        WorkManager.getInstance(context).cancelUniqueWork(name)
        Log.d("WorkManagerService", "Work canceled for $name")
    }
}
