package com.packethunter.mobile.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provides device GPS location for map positioning
 */
class LocationProvider(private val context: Context) : LocationListener {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    private val _isLocationEnabled = MutableStateFlow(false)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled.asStateFlow()
    
    companion object {
        private const val TAG = "LocationProvider"
        private const val MIN_UPDATE_TIME = 5000L // 5 seconds
        private const val MIN_UPDATE_DISTANCE = 10f // 10 meters
    }
    
    init {
        checkLocationPermission()
    }
    
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!checkLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return
        }
        
        try {
            // Try GPS provider first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_UPDATE_TIME,
                    MIN_UPDATE_DISTANCE,
                    this
                )
                _isLocationEnabled.value = true
                Log.d(TAG, "GPS location updates started")
                
                // Get last known location immediately
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                    _currentLocation.value = it
                    Log.d(TAG, "Got last known GPS location: ${it.latitude}, ${it.longitude}")
                }
            }
            
            // Also try network provider as fallback
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_UPDATE_TIME,
                    MIN_UPDATE_DISTANCE,
                    this
                )
                
                // Get last known network location if GPS not available
                if (_currentLocation.value == null) {
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                        _currentLocation.value = it
                        Log.d(TAG, "Got last known network location: ${it.latitude}, ${it.longitude}")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates", e)
            _isLocationEnabled.value = false
        }
    }
    
    fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
            _isLocationEnabled.value = false
            Log.d(TAG, "Location updates stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }
    
    override fun onLocationChanged(location: Location) {
        _currentLocation.value = location
        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m)")
    }
    
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d(TAG, "Location provider status changed: $provider - $status")
    }
    
    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Location provider enabled: $provider")
        _isLocationEnabled.value = true
    }
    
    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Location provider disabled: $provider")
        _isLocationEnabled.value = false
    }
}
