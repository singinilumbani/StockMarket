package com.example.stockmarket.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Starts and stops the background checking.
object BackgroundSync {

    private const val PERIODIC = "account_sync"
    private const val TEST_RUN = "account_sync_test"

    // Only run when the phone has a network
    private val needsNetwork = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    // Called at login. Safe to call many times.
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)   // 15 minutes is the shortest allowed
            .setConstraints(needsNetwork)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,

            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    // FOR TESTING: one run, 20 seconds from now, so you do not have to wait 15 minutes
    fun runSoon(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(20, TimeUnit.SECONDS)
            .setConstraints(needsNetwork)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(TEST_RUN, ExistingWorkPolicy.REPLACE, request)
    }

    // Called at logout
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        WorkManager.getInstance(context).cancelUniqueWork(TEST_RUN)
    }
}
