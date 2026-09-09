package pt.vcc.vccmusic.scanner

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import pt.vcc.vccmusic.VccMusicApplication
import pt.vcc.vccmusic.data.saf.RootAccess

private const val REINDEX_WORK_NAME = "active-library-reindex"

class ReindexWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    /** Reindexa a raiz ativa em segundo plano quando o acesso ainda é válido. */
    override suspend fun doWork(): Result {
        val container = (applicationContext as VccMusicApplication).container
        val rootUri = container.safRootRepository.loadActiveRoot() ?: return Result.success()
        if (container.safRootRepository.access(rootUri) != RootAccess.Available) {
            return Result.failure()
        }
        val root = container.musicRepository.activeRoot() ?: return Result.success()
        return runCatching {
            container.musicScanner.scan(rootUri, root.id)
        }.fold(
            onSuccess = { result ->
                if (result.completed) Result.success() else Result.retry()
            },
            onFailure = { Result.retry() },
        )
    }
}

object ReindexScheduler {
    /** Agenda uma única reindexação com restrições de bateria e rede. */
    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReindexWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            REINDEX_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
