package com.young.airquality.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitConnection {
    companion object { // 레트로핏 객체 생성
        private const val BASE_URL = "https://api.airvisual.com/v2/"
        private var INSTANCE : Retrofit? = null

        fun getInstance() : Retrofit {
            if(INSTANCE == null) {
                INSTANCE = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()) // 서버에서 받은 JSON 응답을 데이터 클래스 객체로 변환
                    .build()
            }
            return INSTANCE!!
        }
    }
}