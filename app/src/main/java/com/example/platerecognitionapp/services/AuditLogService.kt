package com.example.platerecognitionapp.services

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.platerecognitionapp.data.LocalDateTimeConverter
import com.example.platerecognitionapp.utils.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventType: String,
    val eventSource: String,
    val plateNumber: String? = null,
    val details: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val severity: AuditSeverity = AuditSeverity.INFO
)

enum class AuditSeverity {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

enum class AuditEventType {
    APP_START,
    APP_CLOSE,
    LOGIN,
    LOGOUT,
    PLATE_RECOGNITION,
    PLATE_SAVED,
    PLATE_DELETED,
    SETTINGS_CHANGED,
    BACKUP_CREATED,
    BACKUP_RESTORED,
    SECURITY_ALERT,
    ANOMALY_DETECTED
}

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insertLog(auditLog: AuditLog)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 100): List<AuditLog>

    @Query("SELECT * FROM audit_logs WHERE eventType = :eventType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLogsByType(eventType: String, limit: Int = 100): List<AuditLog>

    @Query("SELECT * FROM audit_logs WHERE severity >= :minSeverity ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLogsBySeverity(minSeverity: String, limit: Int = 100): List<AuditLog>

    @Query("DELETE FROM audit_logs WHERE timestamp < :retentionDate")
    suspend fun deleteOldLogs(retentionDate: LocalDateTime)
}

@Database(entities = [AuditLog::class], version = 1)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AuditLogDatabase : RoomDatabase() {
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AuditLogDatabase? = null

        fun getDatabase(context: Context): AuditLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AuditLogDatabase::class.java,
                    "audit_log_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class AuditLogService(private val context: Context) {
    private val auditLogDatabase = AuditLogDatabase.getDatabase(context)
    private val errorLogger = ErrorLogger.getInstance(context)

    suspend fun logEvent(
        eventType: AuditEventType,
        eventSource: String,
        plateNumber: String? = null,
        details: String = "",
        severity: AuditSeverity = AuditSeverity.INFO
    ) = withContext(Dispatchers.IO) {
        try {
            val auditLog = AuditLog(
                eventType = eventType.name,
                eventSource = eventSource,
                plateNumber = plateNumber,
                details = details,
                severity = severity
            )

            auditLogDatabase.auditLogDao().insertLog(auditLog)
        } catch (e: Exception) {
            errorLogger.logError("Erro ao registrar log de auditoria", e)
        }
    }

    suspend fun getRecentLogs(limit: Int = 100): List<AuditLog> = withContext(Dispatchers.IO) {
        try {
            auditLogDatabase.auditLogDao().getRecentLogs(limit)
        } catch (e: Exception) {
            errorLogger.logError("Erro ao buscar logs recentes", e)
            emptyList()
        }
    }

    suspend fun getLogsByType(eventType: AuditEventType, limit: Int = 100): List<AuditLog> = withContext(Dispatchers.IO) {
        try {
            auditLogDatabase.auditLogDao().getLogsByType(eventType.name, limit)
        } catch (e: Exception) {
            errorLogger.logError("Erro ao buscar logs por tipo", e)
            emptyList()
        }
    }

    suspend fun cleanupOldLogs(retentionDays: Long = 30) = withContext(Dispatchers.IO) {
        try {
            val retentionDate = LocalDateTime.now().minusDays(retentionDays)
            auditLogDatabase.auditLogDao().deleteOldLogs(retentionDate)
        } catch (e: Exception) {
            errorLogger.logError("Erro ao limpar logs antigos", e)
        }
    }

    companion object {
        fun getInstance(context: Context): AuditLogService {
            return AuditLogService(context.applicationContext)
        }
    }
}
