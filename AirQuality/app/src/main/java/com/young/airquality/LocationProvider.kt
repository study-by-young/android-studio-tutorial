package com.young.airquality

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat

class LocationProvider(val context : Context) {
    // GPS 나 Network 의 위치 정보를 사용하여 위도나 경도를 가져오려는 목적

    private var location : Location? = null
    private var locationManager : LocationManager? = null // LocationManager : 시스템 위치 서비스 접근을 제공하는 클래스

    init {
        getLocation()
    }

    private fun getLocation() : Location? { // GPS 와 Network 두 위치 모두 사용 불가할 경우 null 반환 -> nullable
        try {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            var gpsLocation : Location? = null
            var networkLocation : Location? = null

            // GPS or Network 가 활성화 되었는지 확인
            val isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) // LocationManager 가 null 이면 catch 로 빠짐
            val isNetworkEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if(!isGPSEnabled && !isNetworkEnabled) {
                return null
            } else {
                // 권한 재확인
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return null
                }
                if(isNetworkEnabled) {
                    networkLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
                if(isGPSEnabled) {
                    gpsLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }
                // GPS 와 Network 둘 다 존재할 시 -> 더 정확한 값 리턴
                if(gpsLocation != null && networkLocation != null) {
                    if(gpsLocation.accuracy > networkLocation.accuracy) {
                        location = gpsLocation
                    } else {
                        location = networkLocation
                    }
                } else {
                    if(gpsLocation != null) {
                        location = gpsLocation
                    }
                    if(networkLocation != null) {
                        location = networkLocation
                    }
                }
            }
        } catch(e : Exception) {
            e.printStackTrace()
        }
        return location
    }

    // 위도
    fun getLocationLatitude() : Double? {
        return location?.latitude
    }

    // 경도
    fun getLocationLongitude() : Double? {
        return location?.longitude
    }

}