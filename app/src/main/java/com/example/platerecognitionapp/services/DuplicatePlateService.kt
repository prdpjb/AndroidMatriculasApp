package com.example.platerecognitionapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.platerecognitionapp.R
import com.example.platerecognitionapp.data.Plate
import com.example.platerecognitionapp.data.PlateDatabase
import kotlinx.coroutines.flow.first

class DuplicatePlateService(private val context: Context) {
    private val plateDatabase = PlateDatabase.getDatabase(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "PLATE_NOTIFICATION_CHANNEL",
                "Plate Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun checkAndNotifyDuplicatePlate(newPlate: Plate) {
        val existingPlates = plateDatabase.plateDao().getAllPlates().first()
        
        val duplicatePlate = existingPlates.find { it.plateNumber == newPlate.plateNumber }
        
        if (duplicatePlate != null) {
            sendDuplicatePlateNotification(newPlate.plateNumber)
        }
    }

    private fun sendDuplicatePlateNotification(plateNumber: String) {
        val builder = NotificationCompat.Builder(context, "PLATE_NOTIFICATION_CHANNEL")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Matrícula Duplicada")
            .setContentText("A matrícula $plateNumber já foi registrada anteriormente")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
