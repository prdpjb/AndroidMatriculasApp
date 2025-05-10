package com.example.platerecognitionapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.platerecognitionapp.MainActivity
import com.example.platerecognitionapp.R
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.services.TrafficIntegrationService.VehicleStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class SmartNotificationService(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val trafficIntegrationService = TrafficIntegrationService.getInstance(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_VEHICLE_ALERTS,
                    "Alertas de Veículos",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_UNUSUAL_ACTIVITY,
                    "Atividades Incomuns",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    suspend fun notifyVehicleStatus(plate: Plate) = withContext(Dispatchers.Default) {
        val vehicleStatus = trafficIntegrationService.checkVehicleStatus(plate)
        
        when (vehicleStatus) {
            VehicleStatus.STOLEN -> sendHighPriorityAlert(
                "Veículo Roubado",
                "Matrícula ${plate.plateNumber} marcada como roubada"
            )
            VehicleStatus.WANTED -> sendMediumPriorityAlert(
                "Veículo em Busca",
                "Matrícula ${plate.plateNumber} está em lista de procurados"
            )
            VehicleStatus.EXPIRED_INSURANCE -> sendLowPriorityAlert(
                "Seguro Expirado",
                "Matrícula ${plate.plateNumber} com seguro vencido"
            )
            VehicleStatus.EXPIRED_INSPECTION -> sendLowPriorityAlert(
                "Inspeção Vencida",
                "Matrícula ${plate.plateNumber} necessita de inspeção"
            )
            else -> {} // Sem notificação
        }
    }

    suspend fun notifyUnusualActivity(plate: Plate, location: String) = withContext(Dispatchers.Default) {
        sendUnusualActivityAlert(
            "Atividade Incomum Detectada",
            "Matrícula ${plate.plateNumber} em local incomum: $location"
        )
    }

    private fun sendHighPriorityAlert(title: String, content: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_VEHICLE_ALERTS)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_VEHICLE_ALERTS, notification)
    }

    private fun sendMediumPriorityAlert(title: String, content: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_VEHICLE_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_VEHICLE_ALERTS, notification)
    }

    private fun sendLowPriorityAlert(title: String, content: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_VEHICLE_ALERTS)
            .setSmallIcon(R.drawable.ic_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_VEHICLE_ALERTS, notification)
    }

    private fun sendUnusualActivityAlert(title: String, content: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_UNUSUAL_ACTIVITY)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UNUSUAL_ACTIVITY, notification)
    }

    companion object {
        private const val CHANNEL_VEHICLE_ALERTS = "vehicle_alerts_channel"
        private const val CHANNEL_UNUSUAL_ACTIVITY = "unusual_activity_channel"
        private const val NOTIFICATION_ID_VEHICLE_ALERTS = 1001
        private const val NOTIFICATION_ID_UNUSUAL_ACTIVITY = 1002

        fun getInstance(context: Context): SmartNotificationService {
            return SmartNotificationService(context.applicationContext)
        }
    }
}
