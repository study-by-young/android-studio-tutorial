package com.young.airquality

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.young.airquality.databinding.ActivityMainBinding
import com.young.airquality.retrofit.AirQualityResponse
import com.young.airquality.retrofit.AirQualityService
import com.young.airquality.retrofit.RetrofitConnection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.IllegalArgumentException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.jvm.java
import kotlin.let

class MainActivity : AppCompatActivity() {

    // API key 값
    val airApiKey = getString(R.string.air_api_key)

    var mInterstitialAd : InterstitialAd? = null

    lateinit var binding : ActivityMainBinding
    lateinit var locationProvider : LocationProvider

    private val PERMISSIONS_REQUEST_CODE = 100

    var latitude : Double? = 0.0
    var longitude : Double? = 0.0

    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    lateinit var getGPSPermissionLauncher : ActivityResultLauncher<Intent> // 설정 앱을 열고 다시 돌아 왔을 때 정보 가져 오는 역할

    val startMapActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
        object : ActivityResultCallback<ActivityResult> {
            override fun onActivityResult(result: ActivityResult) {
                if(result?.resultCode?: 0 == RESULT_OK) {
                    latitude = result?.data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                    longitude = result?.data?.getDoubleExtra("longitude", 0.0) ?: 0.0
                    updateUI()
                }
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAllPermissions()
        updateUI()
        setRefreshButton()
        setFab()
        setBannerAds()
    }

    override fun onResume() {
        super.onResume()
        setInterstitialAds()
    }

    private fun setInterstitialAds() {

        val adRequest = AdRequest.Builder().build()

        // 테스트용 광고 key
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(p0: InterstitialAd) {
                super.onAdLoaded(p0)
                Log.d("Ads Log", "전면 광고가 로드 되었습니다.")
                mInterstitialAd = p0
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                Log.d("Ads Log", "전면 광고가 로드 실패 되었습니다.")
            }
        })
    }

    private fun setBannerAds() {
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        binding.adsBanner.loadAd(adRequest)

        binding.adsBanner.adListener = object: AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                Log.d("Ads Log", "배너 광고가 로드 되었습니다.")
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                Log.d("Ads Log", "배너 광고가 로드 실패 되었습니다.")
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Log.d("Ads Log", "배너 광고가 클릭 되었습니다.")
            }
        }
    }

    private fun updateUI() {
        locationProvider = LocationProvider(this@MainActivity)

        if(latitude == 0.0 && longitude == 0.0) { // 위도와 경도가 없을 때 -> 값을 가져옴
            latitude = locationProvider.getLocationLatitude()
            longitude = locationProvider.getLocationLongitude()
        }

        if(latitude != null && longitude != null) { // 여기서 null 이 아니라고 판별됨 -> !! 붙여줌
            // 1. 현재 위치 가져 오고 UI 업데이트
            val address = getCurrentAddress(latitude!!, longitude!!)

            address?.let { // address 가 null 이 아닌 경우에 { } 코드 실행
                binding.tvLocationTitle.text = "${it.thoroughfare}" // thoroughfare : 지명
                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}"
            }

            // 2. 미세먼지 농도 가져 오고 UI 업데이트
            getAirQualityData(latitude!!, longitude!!)

        } else {
            Toast.makeText(this, "위도, 경도 정보를 가져올 수 없습니다.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAirQualityData(latitude: Double, longitude: Double) {
        var retrofitAPI = RetrofitConnection.getInstance().create( // AirQualityService 인터페이스의 구현체를 생성해 줌
            AirQualityService::class.java
        )

        retrofitAPI.getAirQualityData(
            latitude.toString(),
            longitude.toString(),
            airApiKey
        ).enqueue( object : Callback<AirQualityResponse> {
            override fun onResponse(
                call: Call<AirQualityResponse>,
                response: Response<AirQualityResponse>
            ) {
                if(response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "최신 데이터 업데이트 완료!", Toast.LENGTH_LONG).show()
                    response.body()?.let { updateAirUI(it) } // response.body() 가 null 이 아니면 -> 메서드 실행
                } else {
                    Toast.makeText(this@MainActivity, "데이터를 가져오는 데 실패했습니다.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@MainActivity, "데이터를 가져오는 데 실패했습니다.", Toast.LENGTH_LONG).show()
            }

        }
        )
        // execute() : 동기 실행 (=메인 스레드에서 실행)
        // enqueue() : 비동기 실행 (=백그라운드 스레드에서 실행)
    }

    private fun updateAirUI(airQualityData: AirQualityResponse) {
        val pollutionData = airQualityData.data.current.pollution

        // 수치 지정
        binding.tvCount.text = pollutionData.aqius.toString()
        // 측정 날짜 지정
        val dateTime = ZonedDateTime.parse(pollutionData.ts).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        binding.tvCheckTime.text = dateTime.format(dateFormatter).toString()

        // 미세먼지 농도 이미지 지정
        when(pollutionData.aqius) {
            in 0 .. 50 -> {
                binding.tvTitle.text = "좋음"
                binding.imgBg.setImageResource(R.drawable.bg_good)
            }
            in 51 .. 150 -> {
                binding.tvTitle.text = "보통"
                binding.imgBg.setImageResource(R.drawable.bg_soso)
            }
            in 151 .. 200 -> {
                binding.tvTitle.text = "나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_bad)
            }
            else -> {
                binding.tvTitle.text = "매우 나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_worst)
            }
        }
    }

    private fun setRefreshButton() {
        binding.btnRefresh.setOnClickListener {
            updateUI()
        }
    }

    private fun setFab() {
        binding.fab.setOnClickListener {

            if(mInterstitialAd != null) {
                mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()
                        Log.d("Ads Log", "전면 광고 닫혔습니다.")
                        var intent = Intent(this@MainActivity, MapActivity::class.java)
                        intent.putExtra("currentLat", latitude) // putExtra 를 사용하면 값을 보내줄 수 있음
                        intent.putExtra("currentLng", longitude)
                        startMapActivityResult.launch(intent)
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        super.onAdFailedToShowFullScreenContent(p0)
                        Log.d("Ads Log", "전면 광고 열기 실패했습니다.")
                    }

                    override fun onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent()
                        Log.d("Ads Log", "전면 광고 열기 성공했습니다.")
                        mInterstitialAd = null
                    }
                }

                mInterstitialAd!!.show(this@MainActivity)

            } else {
                Log.d("Ads Log", "전면 광고가 로딩이 되지 않았습니다.")
                Toast.makeText(this, "잠시 후 시도해주세요", Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun getCurrentAddress (latitude : Double, longitude : Double) : Address? {
        // 지오코딩 : 위도, 경도 <-> 지명, 주소

        val geoCoder = Geocoder(this, Locale.KOREA)
        val addresses : List<Address>?

        addresses = try {
            geoCoder.getFromLocation(latitude, longitude, 7)
        } catch (ioException : IOException) {
            Toast.makeText(this, "지오코더 서비스를 이용불가 합니다.", Toast.LENGTH_LONG).show()
            return null
        } catch (illegalArgumentException : IllegalArgumentException) {
            Toast.makeText(this, "잘못된 위도, 경도 입니다.", Toast.LENGTH_LONG).show()
            return null
        }

        if(addresses == null || addresses.size == 0) {
            Toast.makeText(this, "주소가 발견되지 않았습니다.", Toast.LENGTH_LONG).show()
            return null
        }

        return addresses[0]
    }

    private fun checkAllPermissions() {
        if(!isLocationServicesAvailable()) { // GPS ON/OFF 여부 확인
            showDialogForLocationServiceSetting() // GPS OFF -> GPS Setting
        } else {
            isRunTimePermissionsGranted() // 런타임 권한 확인
        }
    }

    private fun isLocationServicesAvailable() : Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        // GPS 를 통해 위치 정보 받아 오기 or WI-FI 등을 통해 위치 정보 받아 오기
    }

    private fun isRunTimePermissionsGranted() {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) // 자세한 위치 정보
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) // 대략적인 위치 정보

        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE) // 액세스 허용 요청 Dialog
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size) {
            var checkResult = true

            for(result in grantResults) {
                if(result != PackageManager.PERMISSION_GRANTED) {
                    checkResult = false
                    break;
                }
            }

            if(checkResult) {
                // 위치 값을 가져올 수 있음
                updateUI()
            } else {
                Toast.makeText(this@MainActivity, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해 주세요.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun showDialogForLocationServiceSetting() {
        // A -> B 넘어간 후에 다시 B -> A 로 데이터 보내야 하는 경우에 ActivityResultLauncher 사용
        getGPSPermissionLauncher = registerForActivityResult( // registerForActivityResult -> ActivityResultLauncher 객체 생성
            ActivityResultContracts.StartActivityForResult()
        ) {
            result ->
            if(result.resultCode == RESULT_OK) { // 위치 서비스 설정 끝내고 잘 돌아 왔는지 확인
                if(isLocationServicesAvailable()) {
                    isRunTimePermissionsGranted()
                } else {
                    Toast.makeText(this@MainActivity, "위치 서비스를 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        val builder : AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("위치 서비스가 꺼져 있습니다. 설정해야 앱을 사용할 수 있습니다.") // 추후 strings.xml 활용
        builder.setCancelable(true) // Dialog 바깥을 터치 시 창이 닫힘
        builder.setPositiveButton("설정", DialogInterface.OnClickListener { dialogInterface, i ->  
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) // 안드로이드 설정 페이지로 이동
            getGPSPermissionLauncher.launch(callGPSSettingIntent)
        })
        builder.setNegativeButton("취소", DialogInterface.OnClickListener { dialogInterface, i ->
            dialogInterface.cancel()
            Toast.makeText(this@MainActivity, "위치 서비스를 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
            finish()
        })
        builder.create().show()
    }

}