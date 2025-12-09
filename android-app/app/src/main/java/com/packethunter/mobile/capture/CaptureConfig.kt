package com.packethunter.mobile.capture

import android.content.Context
import android.content.SharedPreferences

object CaptureConfig {
    private const val KEY_ROUTE_ALL = "route_all_traffic"
    private const val KEY_USE_SYSTEM_DNS = "use_system_dns"
    private const val PREFS_NAME = "afterpackets_capture_config"

    fun routeAllTraffic(context: Context): Boolean {
        val prefs = prefs(context)
        return prefs.getBoolean(KEY_ROUTE_ALL, false)
    }

    fun setRouteAllTraffic(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ROUTE_ALL, value).apply()
    }

    fun useSystemDns(context: Context): Boolean {
        val prefs = prefs(context)
        return prefs.getBoolean(KEY_USE_SYSTEM_DNS, true)
    }

    fun setUseSystemDns(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_SYSTEM_DNS, value).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
