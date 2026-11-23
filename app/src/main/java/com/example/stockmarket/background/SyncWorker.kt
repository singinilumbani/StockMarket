package com.example.stockmarket.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stockmarket.data.local.AccountCache
import com.example.stockmarket.data.local.SettingsRepository
import com.example.stockmarket.data.remote.Api
import com.example.stockmarket.data.repository.SessionManager
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

// Runs in the background, even when the app is closed.
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1) When the app is open, its own refresh loop already does this job
        if (AppVisibility.isVisible) return Result.success()

        val context = applicationContext

        // 2) Is somebody logged in? (The worker may run in a process that was just started for it.)
        val saved = SessionManager.tokenForBackground(context) ?: return Result.success()
        val userId = saved.user.id
        AccountCache.init(context)

        return try {
            // 3) What did we know last time, and what does the server say now?
            val beforeAccount = AccountCache.readAccount(userId)?.account
            val beforeRegistration = AccountCache.readRegistration(userId)?.status
            val account = Api.account.account()
            val kyc = Api.kyc.get()

            // 4) Tell the user what changed, if they allowed it
            val changes = findChanges(beforeAccount, account, beforeRegistration, kyc.status, kyc.rejectionReason)
            val allowed = SettingsRepository(context).settings.first().orderAlerts
            if (allowed && changes.isNotEmpty()) {
                Notifier.createChannel(context)
                changes.forEach { Notifier.show(context, it.id, it.title, it.text) }
            }

            // 5) Remember the new state, so the same news is never announced twice
            AccountCache.saveAccount(userId, System.currentTimeMillis(), account)
            AccountCache.saveRegistration(userId, kyc.status)

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.retry()                      // no connection: try again later, with a growing delay
        } catch (e: HttpException) {
            if (e.code() == 401) BackgroundSync.cancel(context)    // the login is no longer valid
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

