package com.example.phoneinfo.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        Handler().postDelayed({
            getDeviceINFO()
        }, 5000)

        binding.button.setOnClickListener {
            getDeviceINFO()
        }

        uploadData()
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

                        val myResponse = response.body()
                        Toast.makeText(
                            this@MainActivity,
                            myResponse?.msg.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        Handler().postDelayed({
                            Toast.makeText(
                                this@MainActivity,
                                "Uploaded Data",
                                Toast.LENGTH_LONG
                            ).show()
                        }, 3000)
                    }

                    override fun onFailure(call: Call<RequestResponseModel>, t: Throwable) {
                        Toast.makeText(
                            this@MainActivity,
                            t.message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getDeviceINFO() {
        fetchLocation()
        getDeviceIMEI()
        getBatteryStatus()
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

    @SuppressLint("HardwareIds")
    private fun getDeviceIMEI() {
        binding.deviceID.text = UUID.randomUUID().toString()
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
                binding.location.text = "Latitude = ${it.latitude} Longitude = ${it.longitude}"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStop() {
        super.onStop()
        getDeviceINFO()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPause() {
        super.onPause()
        getDeviceINFO()
    }

}