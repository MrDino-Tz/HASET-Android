package com.haset.hasetapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;

public class NetworkUtils {

    public interface NetworkCallback {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    private static ConnectivityManager.NetworkCallback systemNetworkCallback;
    private static final java.util.List<java.lang.ref.WeakReference<NetworkCallback>> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * Checks if the device is currently connected to a network (Wi-Fi or mobile data).
     *
     * @param context The application context.
     * @return True if network is available, false otherwise.
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android M and above
            android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // For older Android versions (deprecated in M, but still works)
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    /**
     * Adds a network callback to monitor network changes.
     *
     * @param context The application context.
     * @param callback The callback to notify on network changes.
     */
    public static synchronized void addNetworkCallback(Context context, NetworkCallback callback) {
        if (context == null || callback == null) return;

        // Add to listeners list using WeakReference to prevent fragment leaks
        listeners.add(new java.lang.ref.WeakReference<>(callback));
        
        if (systemNetworkCallback == null) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager == null) return;

            systemNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(android.net.Network network) {
                    notifyListeners(true);
                }

                @Override
                public void onLost(android.net.Network network) {
                    notifyListeners(false);
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(systemNetworkCallback);
            }
        }
    }

    private static void notifyListeners(boolean available) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        for (java.lang.ref.WeakReference<NetworkCallback> ref : listeners) {
            NetworkCallback cb = ref.get();
            if (cb != null) {
                mainHandler.post(() -> {
                    if (available) cb.onNetworkAvailable();
                    else cb.onNetworkLost();
                });
            } else {
                listeners.remove(ref); // Clean up dead references
            }
        }
    }

    /**
     * Removes the network callback.
     *
     * @param context The application context.
     * @param callback The callback to remove.
     */
    public static synchronized void removeNetworkCallback(Context context, NetworkCallback callback) {
        if (context == null || callback == null) return;

        // Remove the specific callback
        for (java.lang.ref.WeakReference<NetworkCallback> ref : listeners) {
            if (ref.get() == callback) {
                listeners.remove(ref);
                break;
            }
        }
        
        // If no more listeners, unregister from system
        if (listeners.isEmpty() && systemNetworkCallback != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(systemNetworkCallback);
                } catch (Exception e) {
                    // Already unregistered or other issue
                }
            }
            systemNetworkCallback = null;
        }
    }
}
