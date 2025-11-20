package com.packethunter.mobile.capture

import android.content.Context
import android.util.Log

/**
 * JNI interface for native layer operations
 */
object NativeInterface {
    private const val TAG = "NativeInterface"
    
    init {
        Log.i(TAG, "Initializing NativeInterface object")
        try {
            System.loadLibrary("packethunter")
            Log.i(TAG, "✅ Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "❌ Failed to load native library", e)
        }
    }
    
    /**
     * Set application context for native layer
     * This is required for robust class loading in native threads
     * @param context Application context
     */
    external fun setAppContext(context: Context)
}