package com.otamurod.notificationindetails

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.otamurod.notificationindetails.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = "https://cdn.wallpapersafari.com/28/98/9MbE1H.png"
        val imageUri = intent.data
        imageUri?.apply {
            binding.notifyBtn.visibility = View.GONE
            binding.imageCard.visibility = View.VISIBLE
            binding.notificationImage.setImageURI(imageUri)
        }

        binding.apply {
            notifyBtn.setOnClickListener {
                onDownload(url)
                notifyBtn.isClickable = false
            }
        }
    }

    private fun requestPermissions(url: String) {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                @RequiresApi(Build.VERSION_CODES.M)
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.permission_granted),
                        Toast.LENGTH_SHORT
                    ).show()
                    downloadFile(getString(R.string.file_name), getString(R.string.fav_image), url)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) { /* ... */
                }
            }).check()
    }

    private fun onDownload(url: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadFile(getString(R.string.file_name), getString(R.string.fav_image), url)
        } else {
            requestPermissions(url)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("Range")
    private fun downloadFile(fileName: String, desc: String, url: String) {
        val imagePath =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName"
        val imageFile = File(imagePath)

        if (!imageFile.exists()) {
            // fileName -> fileName with extension
            val request = DownloadManager.Request(Uri.parse(url))
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadID = downloadManager.enqueue(request)

            var totalBytes = 0L
            var lastProgress = 0

            buildNotification()
            createNotificationChannel()
            // Initial Alert
            notificationManager.notify(100, notificationBuilder.build())

            Thread {
                var downloading = true

                while (downloading) {
                    val cursor =
                        downloadManager.query(DownloadManager.Query().setFilterById(downloadID))

                    if (cursor.moveToFirst()) {
                        if (totalBytes <= 0) {
                            totalBytes =
                                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        }

                        val downloadedBytes =
                            cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val progress = (downloadedBytes * 100L / totalBytes).toInt()

                        if (downloadedBytes == totalBytes && totalBytes > 0) {
                            downloading = false
                        }

                        if (progress > lastProgress) {
                            lastProgress = progress
                            updateNotificationProgress(progress, totalBytes)
                        }
                    }

                    cursor.close()
                    SystemClock.sleep(100)
                }
                // Listen to the completion of the download using a BroadcastReceiver
                val onComplete = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadID) {
                            // Dismiss the Download Manager's notification
                            downloadManager.remove(id)
                        }
                    }
                }

                registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
                updateNotification(fileName)
            }.start()

        } else {
            buildNotification()
            createNotificationChannel()
            updateNotification(fileName)
        }
    }

    private fun updateNotificationProgress(progress: Int, totalBytes: Long) {
        val contentText = String.format(
            "%.2f/%.2f MB",
            (progress * totalBytes / 100.0) / (1024 * 1024),
            totalBytes.toDouble() / (1024 * 1024)
        )
        notificationBuilder.setContentText(contentText)
            .setProgress(100, progress, false)

        notificationManager.notify(100, notificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateNotification(fileName: String) {
        // Update the notification when the download is completed
        val imagePath =
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName"
        val imageFile = File(imagePath)

        if (imageFile.exists()) {

            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

            val bigPictureStyle = NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null)

            val imageUri: Uri =
                Uri.parse(imagePath)

            // Create an explicit intent for an Activity in your app
            val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            intent.data = imageUri
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                this@MainActivity, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder.setContentTitle(getString(R.string.download_completed))
                .setContentText(getString(R.string.tap_to_view))
                .setProgress(0, 0, false)
                .setAutoCancel(true)
                .setStyle(bigPictureStyle)
                .setContentIntent(pendingIntent)

            notificationManager.notify(100, notificationBuilder.build())
        }
    }

    private fun buildNotification() {
        val imageTitle = getString(R.string.download_image)
        val imageDescription = getString(R.string.tap_to_view)

        notificationBuilder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fav_border)
            .setContentTitle(imageTitle)
            .setContentText(imageDescription)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(resources.getColor(R.color.red))
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, true)
            // Set the intent that will fire when the user taps the notification
            .setAutoCancel(true)
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

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
        private const val CHANNEL_NAME = "Favourite Channel"
        private const val CHANNEL_DESCRIPTION = "Favourites"
    }
}