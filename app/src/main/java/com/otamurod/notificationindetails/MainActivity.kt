package com.otamurod.notificationindetails

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.otamurod.notificationindetails.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var notificationManager: NotificationManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUri = intent.data
        imageUri?.apply {
            binding.notifyBtn.visibility = View.GONE
            binding.notificationImage.visibility = View.VISIBLE
            binding.notificationImage.setImageURI(imageUri)
        }

        binding.apply {
            val imageTitle = "Favourites"
            val imageDescription = "Tap to view the image"

            val imageUri: Uri =
                Uri.parse("android.resource://com.otamurod.notificationindetails/drawable/img")
            val picture = BitmapFactory.decodeResource(resources, R.drawable.img)

            // Create an explicit intent for an Activity in your app
            val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            intent.data = imageUri
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                this@MainActivity,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_fav_border)
                .setContentTitle(imageTitle)
                .setContentText(imageDescription)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setColor(resources.getColor(R.color.red))
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(picture)
                        .bigLargeIcon(null)
                )

            val notification = notificationBuilder.build()

            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            createNotificationChannel()

            notifyBtn.setOnClickListener {
                notificationManager.notify(100, notification)
            }
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "fav_channel"
        private const val CHANNEL_NAME = "Favourites"
        private const val CHANNEL_DESCRIPTION = "Favourites"
    }
}