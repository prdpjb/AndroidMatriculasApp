package com.example.platerecognitionapp.services

import android.content.Context
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.utils.ErrorLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class CommunityPlateReport {
    val plateNumber: String
    val reportType: ReportType
    val description: String
    val location: String
    val reportedBy: String
    val timestamp: LocalDateTime = LocalDateTime.now()
    val upvotes: Int = 0
    val downvotes: Int = 0
}

enum class ReportType {
    SUSPICIOUS_ACTIVITY,
    POTENTIAL_THEFT,
    TRAFFIC_VIOLATION,
    ABANDONED_VEHICLE,
    OTHER
}

data class CommunityAlert {
    val plateNumber: String
    val alertType: AlertType
    val severity: AlertSeverity
    val communityConfidence: Float
    val totalReports: Int
}

enum class AlertType {
    HIGH_RISK,
    MODERATE_RISK,
    LOW_RISK,
    CLEARED
}

enum class AlertSeverity {
    INFO,
    WARNING,
    URGENT,
    CRITICAL
}

class CommunityCollaborationService(private val context: Context) {
    private val firestore: FirebaseFirestore by lazy { Firebase.firestore }
    private val errorLogger = ErrorLogger.getInstance(context)

    suspend fun reportPlate(
        plate: Plate,
        reportType: ReportType,
        description: String,
        location: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val report = hashMapOf(
                "plateNumber" to plate.plateNumber,
                "reportType" to reportType.name,
                "description" to description,
                "location" to location,
                "reportedBy" to getUserIdentifier(),
                "timestamp" to System.currentTimeMillis(),
                "upvotes" to 0,
                "downvotes" to 0
            )

            firestore.collection("community_reports")
                .add(report)
                .await()

            true
        } catch (e: Exception) {
            errorLogger.logError("Erro ao reportar matrícula na comunidade", e)
            false
        }
    }

    suspend fun getCommunityAlertForPlate(plateNumber: String): CommunityAlert? = withContext(Dispatchers.IO) {
        try {
            val reportsSnapshot = firestore.collection("community_reports")
                .whereEqualTo("plateNumber", plateNumber)
                .get()
                .await()

            if (reportsSnapshot.isEmpty) return@withContext null

            val reports = reportsSnapshot.documents
            val totalReports = reports.size
            val upvotes = reports.sumOf { it.getLong("upvotes") ?: 0 }
            val downvotes = reports.sumOf { it.getLong("downvotes") ?: 0 }

            val communityConfidence = calculateCommunityConfidence(upvotes, downvotes)
            val alertType = determineAlertType(communityConfidence)
            val alertSeverity = determineAlertSeverity(reports)

            CommunityAlert(
                plateNumber = plateNumber,
                alertType = alertType,
                severity = alertSeverity,
                communityConfidence = communityConfidence,
                totalReports = totalReports
            )
        } catch (e: Exception) {
            errorLogger.logError("Erro ao buscar alerta comunitário", e)
            null
        }
    }

    suspend fun voteCommunityReport(
        reportId: String,
        isUpvote: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val reportRef = firestore.collection("community_reports").document(reportId)
            val voteField = if (isUpvote) "upvotes" else "downvotes"

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(reportRef)
                val currentVotes = snapshot.getLong(voteField) ?: 0
                transaction.update(reportRef, voteField, currentVotes + 1)
            }.await()

            true
        } catch (e: Exception) {
            errorLogger.logError("Erro ao votar em relatório comunitário", e)
            false
        }
    }

    private fun getUserIdentifier(): String {
        // Implementar lógica de identificação de usuário
        // Pode ser um ID único gerado ou baseado em autenticação
        return "anonymous_user"
    }

    private fun calculateCommunityConfidence(upvotes: Long, downvotes: Long): Float {
        val totalVotes = upvotes + downvotes
        return if (totalVotes > 0) upvotes.toFloat() / totalVotes else 0f
    }

    private fun determineAlertType(communityConfidence: Float): AlertType {
        return when {
            communityConfidence > 0.8 -> AlertType.HIGH_RISK
            communityConfidence > 0.5 -> AlertType.MODERATE_RISK
            communityConfidence > 0.2 -> AlertType.LOW_RISK
            else -> AlertType.CLEARED
        }
    }

    private fun determineAlertSeverity(reports: List<com.google.firebase.firestore.QueryDocumentSnapshot>): AlertSeverity {
        val criticalReports = reports.count { 
            it.getString("reportType") in listOf(
                ReportType.POTENTIAL_THEFT.name, 
                ReportType.SUSPICIOUS_ACTIVITY.name
            )
        }

        return when {
            criticalReports > reports.size * 0.5 -> AlertSeverity.CRITICAL
            criticalReports > reports.size * 0.3 -> AlertSeverity.URGENT
            criticalReports > reports.size * 0.1 -> AlertSeverity.WARNING
            else -> AlertSeverity.INFO
        }
    }

    companion object {
        fun getInstance(context: Context): CommunityCollaborationService {
            return CommunityCollaborationService(context.applicationContext)
        }
    }
}
