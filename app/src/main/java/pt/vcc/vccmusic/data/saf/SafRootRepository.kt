package pt.vcc.vccmusic.data.saf

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.FileNotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.rootPreferences by preferencesDataStore(name = "root_preferences")
private val activeRootKey = stringPreferencesKey("active_root_uri")

sealed interface RootAccess {
    data object Available : RootAccess
    data object Missing : RootAccess
    data object Revoked : RootAccess
    data object Invalid : RootAccess
}

interface SafRootRepository {
    val activeRootUri: Flow<String?>
    suspend fun accept(uri: Uri): Result<Uri>
    suspend fun access(uri: Uri): RootAccess
    suspend fun clear()
}

class AndroidSafRootRepository(
    context: Context,
) : SafRootRepository {
    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver

    override val activeRootUri: Flow<String?> =
        appContext.rootPreferences.data.map { preferences -> preferences[activeRootKey] }

    override suspend fun accept(uri: Uri): Result<Uri> {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT ||
            !DocumentsContract.isTreeUri(uri)
        ) {
            return Result.failure(IllegalArgumentException("A raiz selecionada não é uma URI SAF válida."))
        }

        return runCatching {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            check(access(uri) == RootAccess.Available) {
                "A raiz selecionada não está acessível."
            }
            appContext.rootPreferences.edit { preferences ->
                preferences[activeRootKey] = uri.toString()
            }
            uri
        }
    }

    override suspend fun access(uri: Uri): RootAccess {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT ||
            !DocumentsContract.isTreeUri(uri)
        ) {
            return RootAccess.Invalid
        }

        val hasPermission = resolver.persistedUriPermissions.any { permission ->
            permission.uri == uri &&
                permission.isReadPermission
        }
        if (!hasPermission) return RootAccess.Revoked

        return try {
            resolver.query(
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    uri,
                    DocumentsContract.getTreeDocumentId(uri),
                ),
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null,
            )?.use { RootAccess.Available } ?: RootAccess.Missing
        } catch (_: SecurityException) {
            RootAccess.Revoked
        } catch (_: FileNotFoundException) {
            RootAccess.Missing
        } catch (_: UnsupportedOperationException) {
            RootAccess.Missing
        }
    }

    override suspend fun clear() {
        appContext.rootPreferences.edit { preferences -> preferences.remove(activeRootKey) }
    }

    suspend fun loadActiveRoot(): Uri? =
        activeRootUri.first()?.let(Uri::parse)
}
