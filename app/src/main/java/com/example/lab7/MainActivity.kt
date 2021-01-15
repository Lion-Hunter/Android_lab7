package com.example.lab7


import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.example.lab7.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            val intent = Intent(this, ImageDownloader::class.java).apply {
                putExtra("url", "https://wallbox.ru/wallpapers/main2" +
                        "/202046/16050262595faac1d3d9aec1.70131582.jpg")
            }

            startService(intent)
        }
    }
}

class ImageDownloader : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url")
        var path: String? = null

        if (url != null) {
            scope.launch(Dispatchers.IO) {
                Log.d("ImageDownloader",
                    "ImageDownloader started in ${Thread.currentThread().name}")
                var picture: Bitmap? = null
                try {
                    val stream = URL(url).openStream()
                    picture = BitmapFactory.decodeStream(stream)
                } catch (e: Exception) {
                    e.message?.let { Log.e("Error", it) }
                    e.printStackTrace()
                }

                path = picture?.save(filesDir)
                sendBroadcast(Intent("Download complete"). apply {
                    putExtra("path", path) })

                Log.d("ImageDownloader",
                    "Successfully download in $path")
                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private fun Bitmap.save(folder: File): String {
        var i = 1
        while (true) {
            val f = File(filesDir, "picture${i}.png")
            if (!f.exists()) {
                break
            }
            i++
        }

        val file = "picture${i}.png"

        Log.d("ImageDownloader", "Starting to download picture${i}.png")

        openFileOutput(file, MODE_PRIVATE).use {
            compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        return File(filesDir, file).absolutePath
    }

    override fun onDestroy() {
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}