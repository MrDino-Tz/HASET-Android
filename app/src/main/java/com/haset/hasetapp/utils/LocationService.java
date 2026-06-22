package com.haset.hasetapp.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;

public class LocationService {
    
    private static final String TAG = "LocationService";
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_INTERVAL = 5000; // 5 seconds
    
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationManager locationManager;
    private LocationCallback locationCallback;
    private LocationListener locationListener;
    
    public interface LocationCallbackListener {
        void onLocationReceived(Location location);
        void onLocationError(String error);
    }
    
    public LocationService(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    
    /**
     * Get current location using FusedLocationProviderClient (recommended)
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocation(LocationCallbackListener callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Location permission not granted");
            return;
        }
        
        if (!isLocationEnabled()) {
            callback.onLocationError("Location services are disabled");
            return;
        }
        
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "Location received: " + location.getLatitude() + ", " + location.getLongitude());
                            callback.onLocationReceived(location);
                        } else {
                            Log.w(TAG, "Location is null");
                            // Fallback to last known location
                            getLastKnownLocation(callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get location", e);
                        callback.onLocationError("Failed to get location: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception getting location", e);
            callback.onLocationError("Exception: " + e.getMessage());
        }
    }
    
    /**
     * Get last known location as fallback
     */
    @SuppressLint("MissingPermission")
    private void getLastKnownLocation(LocationCallbackListener callback) {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "Last known location: " + location.getLatitude() + ", " + location.getLongitude());
                            callback.onLocationReceived(location);
                        } else {
                            Log.w(TAG, "No last known location available");
                            callback.onLocationError("No location available");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get last known location", e);
                        callback.onLocationError("Failed to get last known location: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception getting last known location", e);
            callback.onLocationError("Exception: " + e.getMessage());
        }
    }
    
    /**
     * Start location updates for continuous tracking
     */
    @SuppressLint("MissingPermission")
    public void startLocationUpdates(LocationCallbackListener callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Location permission not granted");
            return;
        }
        
        if (!isLocationEnabled()) {
            callback.onLocationError("Location services are disabled");
            return;
        }
        
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                .build();
        
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        callback.onLocationReceived(location);
                    }
                }
            }
        };
        
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d(TAG, "Location updates started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start location updates", e);
            callback.onLocationError("Failed to start location updates: " + e.getMessage());
        }
    }
    
    /**
     * Stop location updates
     */
    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                Log.d(TAG, "Location updates stopped");
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop location updates", e);
            }
        }
    }
    
    /**
     * Calculate distance between two points in kilometers
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        Location locationA = new Location("point A");
        locationA.setLatitude(lat1);
        locationA.setLongitude(lon1);
        
        Location locationB = new Location("point B");
        locationB.setLatitude(lat2);
        locationB.setLongitude(lon2);
        
        float distanceInMeters = locationA.distanceTo(locationB);
        return distanceInMeters / 1000.0; // Convert to kilometers
    }
    
    /**
     * Get distance between two points in a readable format
     */
    public static String getDistanceText(double distanceInKm) {
        if (distanceInKm < 1) {
            int meters = (int) (distanceInKm * 1000);
            return meters + " m away";
        } else if (distanceInKm < 10) {
            return String.format("%.1f km away", distanceInKm);
        } else {
            return String.format("%.0f km away", distanceInKm);
        }
    }
    
    /**
     * Check if location permission is granted
     */
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if location services are enabled
     */
    private boolean isLocationEnabled() {
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gpsEnabled || networkEnabled;
    }
    
    /**
     * Get available location providers
     */
    public List<String> getAvailableProviders() {
        return locationManager.getProviders(true);
    }
    
    /**
     * Check if GPS is available
     */
    public boolean isGpsAvailable() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    
    /**
     * Check if Network location is available
     */
    public boolean isNetworkLocationAvailable() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    
    /**
     * Get location accuracy description
     */
    public static String getAccuracyDescription(float accuracy) {
        if (accuracy < 10) {
            return "Excellent (" + String.format("%.0f", accuracy) + "m)";
        } else if (accuracy < 50) {
            return "Good (" + String.format("%.0f", accuracy) + "m)";
        } else if (accuracy < 100) {
            return "Fair (" + String.format("%.0f", accuracy) + "m)";
        } else {
            return "Poor (" + String.format("%.0f", accuracy) + "m)";
        }
    }
    
    /**
     * Validate location coordinates
     */
    public static boolean isValidLocation(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }
    
    /**
     * Get location provider type
     */
    public static String getProviderType(Location location) {
        if (location == null) return "Unknown";
        
        switch (location.getProvider()) {
            case LocationManager.GPS_PROVIDER:
                return "GPS";
            case LocationManager.NETWORK_PROVIDER:
                return "Network";
            case LocationManager.PASSIVE_PROVIDER:
                return "Passive";
            default:
                return location.getProvider();
        }
    }
    
    /**
     * Format location as readable string
     */
    public static String formatLocation(Location location) {
        if (location == null) return "Location not available";
        
        return String.format("%.6f, %.6f (%s, %.0fm accuracy)", 
                location.getLatitude(), 
                location.getLongitude(),
                getProviderType(location),
                location.getAccuracy());
    }
}
