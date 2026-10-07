package com.samupdater.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.samupdater.app.MainActivity
import com.samupdater.app.R
import com.samupdater.app.domain.Channel
import com.samupdater.app.domain.Observation

object Notifier {

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(Channel.STABLE.id, context.getString(R.string.channel_stable), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(Channel.BETA.id, context.getString(R.string.channel_beta), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(Channel.TEST.id, context.getString(R.string.channel_test), NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannels(channels)
    }

    fun notify(context: Context, change: Observation) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val title = when (change.channel) {
            Channel.STABLE -> context.getString(R.string.notif_stable_title, change.device.label)
            Channel.BETA -> context.getString(R.string.notif_beta_title, change.device.label)
            Channel.TEST -> context.getString(R.string.notif_test_title, change.device.label)
        }
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, change.channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("${change.value} (${change.device.model} ${change.device.csc})")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(change.key.hashCode(), notification)
    }

    private val Channel.id: String get() = "firmware_${name.lowercase()}"
}
