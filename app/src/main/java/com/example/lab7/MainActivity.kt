package com.example.lab7


import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.example.lab7.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {
    private val serviceConnection = MServiceConnection()

    private lateinit var messenger: Messenger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        messenger = Messenger(MActivityHandler(mainLooper, binding))

        bindService(Intent(this, ImageDownloader::class.java),
                serviceConnection, Context.BIND_AUTO_CREATE)

        binding.buttonStart.setOnClickListener {
            val intent = Intent(this, ImageDownloader::class.java).apply {
                putExtra("url", "https://wallbox.ru/wallpapers/main2" +
                        "/202046/16050262595faac1d3d9aec1.70131582.jpg")
            }

            startService(intent)
        }

        binding.buttonBind.setOnClickListener {
            val msg = Message.obtain().apply {
                obj = "https://wallbox.ru/wallpapers/main2" +
                        "/202046/16050262595faac1d3d9aec1.70131582.jpg"
                what = 123
                replyTo = messenger
            }
            serviceConnection.serviceMessenger?.send(msg)
        }
    }
}

class ImageDownloader : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url")
        var path: String?

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
                    picture?.compress(Bitmap.CompressFormat.PNG, 100, it)
                }

                path = File(filesDir, file).absolutePath

                sendBroadcast(Intent("Download complete").apply {
                    putExtra("path", path)
                })

                Log.d("ImageDownloader",
                    "Successfully download in $path")
                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = Messenger(MServiceHandler(mainLooper, this)).binder
}

class MServiceConnection: ServiceConnection {
    var serviceMessenger: Messenger? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        serviceMessenger = Messenger(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        serviceMessenger = null
    }

}

class MServiceHandler(looper: Looper, private val downloader: ImageDownloader) : Handler(looper) {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun handleMessage(msg: Message) {
        if (msg.what != 123) {
            super.handleMessage(msg)
            return
        }

        val url = msg.obj as String
        var path: String?

        val replyTo = msg.replyTo

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

            var i = 1
            while (true) {
                val f = File(downloader.filesDir, "picture${i}.png")
                if (!f.exists()) {
                    break
                }
                i++
            }

            val file = "picture${i}.png"

            Log.d("ImageDownloader", "Starting to download picture${i}.png")

            downloader.openFileOutput(file, Service.MODE_PRIVATE).use {
                picture?.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            path = File(downloader.filesDir, file).absolutePath

            Log.d("ImageDownloader",
                    "Successfully download in $path")

            replyTo.send(Message.obtain().apply {
                obj = path
                what = 321
            })
        }
    }
}

class MActivityHandler(looper: Looper, private val binding: ActivityMainBinding) : Handler(looper) {
    override fun handleMessage(msg: Message) {
        if (msg.what != 321) {
            super.handleMessage(msg)
            return
        }

        binding.pathFromBoundService.text = msg.obj as String
    }
}