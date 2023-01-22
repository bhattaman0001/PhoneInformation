@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package com.example.phoneinfo.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings.Secure
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.phoneinfo.api.RetrofitClient
import com.example.phoneinfo.databinding.ActivityMainBinding
import com.example.phoneinfo.model.Model
import com.example.phoneinfo.model.RequestResponseModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var binding: ActivityMainBinding
    val handler = Handler()
    val delay = 900000L

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            alertDialog()
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

            handler.postDelayed(object : Runnable {
                override fun run() {
                    getDeviceINFO()
                    handler.postDelayed(this, delay)
                }
            }, delay)

            binding.button.setOnClickListener {
                getDeviceINFO()
                uploadData()
            }
        } else if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.INTERNET), 101
            )
            Toast.makeText(
                this@MainActivity,
                "Connect through internet first!",
                Toast.LENGTH_SHORT
            ).show()
            return

        }

        openCaptureImage()

    }

    private fun openCaptureImage() {
        binding.captureImage.setOnClickListener {
            startActivity(Intent(this, ImageCapture::class.java))
        }
    }

    private fun alertDialog() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Listen!")
        builder.setMessage(
            "The Device information will be \nchanged after every 15 minutes\n" +
                    "and in between if you want to see the \ninformation then click on provided button"
        )
        builder.setCancelable(true)
        builder.create().show()
    }

    private fun uploadData() {
        binding.pb.visibility = View.VISIBLE

        try {
            val model = Model(
                binding.deviceID.text.toString(),
                binding.internetStatus.text.toString(),
                binding.batteryStatus.text.toString(),
                binding.batteryPercentage.text.toString(),
                binding.location.text.toString()
            )

            val retrofit = RetrofitClient()
            retrofit.buildService().sendData(model)
                .enqueue(object : retrofit2.Callback<RequestResponseModel> {
                    override fun onResponse(
                        call: Call<RequestResponseModel>,
                        response: Response<RequestResponseModel>
                    ) {

                        // progress bar invisible
                        binding.pb.visibility = View.INVISIBLE

//                        Toast.makeText(
//                            this@MainActivity,
//                            response.message(),
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        Log.d("msg", response.message())
//                        Handler().postDelayed({
//                            Toast.makeText(
//                                this@MainActivity,
//                                "Your data has been uploaded!",
//                                Toast.LENGTH_LONG
//                            ).show()
//                        }, 3000)
                    }

                    override fun onFailure(call: Call<RequestResponseModel>, t: Throwable) {
                        if (t.message.toString() == "Failed to connect to /143.244.138.96:2110") {
                            Toast.makeText(
                                this@MainActivity,
                                "First Connect to Internet!!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
//                        Log.d("msg", t.message.toString())
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun getDeviceINFO() {
        fetchLocation()
        getDeviceIMEI()
        getBatteryStatus()
        getTimeStamp()
        if (checkForInternet(this)) {
            binding.internetStatus.text = "Device is connected through Internet"
        } else {
            binding.internetStatus.text = "Device is not connected through Internet"
        }
    }

    private fun checkForInternet(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    private fun getBatteryStatus() {
        val bm = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batLevel: Int = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        binding.batteryPercentage.text = "Currently the Battery level is : $batLevel%"

        // Intent to check the actions on battery
        val batteryStatus: Intent? =
            IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                applicationContext.registerReceiver(null, ifilter)
            }

        // usbCharge is true when connected to usb port and same with the ac wall charger
        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        if (usbCharge) {
            binding.batteryStatus.text = "Device is charging through USB!"
        } else if (acCharge) {
            binding.batteryStatus.text = "Device is charging through Adapter!"
        } else {
            binding.batteryStatus.text = "Device is not plugged in anywhere!"
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("HardwareIds")
    private fun getDeviceIMEI() {
//        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//        if (ActivityCompat.checkSelfPermission(
//                this@MainActivity,
//                Manifest.permission.READ_PHONE_STATE
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this@MainActivity,
//                arrayOf(Manifest.permission.READ_PHONE_STATE), 101
//            )
//            return
//        }
//        binding.deviceID.text = telephonyManager.imei.toString()

        binding.deviceID.text =
            Secure.getString(this@MainActivity.contentResolver, Secure.ANDROID_ID).toString()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getTimeStamp() {
        binding.timeStamp.text = SimpleDateFormat("HH:mm:ss").format(Calendar
            .getInstance().time
        );
    }

    @SuppressLint("SetTextI18n")
    private fun fetchLocation() {
        val task: Task<Location> = fusedLocationProviderClient.lastLocation

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101
            )
            return
        }
        task.addOnSuccessListener {
            if (it != null) {
                binding.location.text = "Location = ${it.latitude}, ${it.longitude}"
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation === Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(
                this@MainActivity,
                "Landscape Mode ON",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this@MainActivity,
                "Portrait Mode ON",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStop() {
        super.onStop()
        getDeviceINFO()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPause() {
        super.onPause()
        getDeviceINFO()
    }

}