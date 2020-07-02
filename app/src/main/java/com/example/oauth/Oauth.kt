package com.example.oauth

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException


class Oauth(val clientId: String, val clientSecret: String, context: Context){

    var mAccessToken: String? = null
    var mTokenType: String? = null
    private var mRefreshToken: String? = null

    val preferences: SharedPreferences =
        context.getSharedPreferences("oauth_prefs", MODE_PRIVATE)

    fun getAccessToken(authCode: String?) {
        val client = OkHttpClient()
        val requestBody: RequestBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", authCode)
            .add("access_type","offline")
            .build()
        Log.e("TAG","requestBody = ${requestBody}")
        val request: Request = Request.Builder()
            .url("https://www.googleapis.com/oauth2/v4/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(requestBody)
            .build()
        Log.e("TAG","requst = $request")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Fail", "Token was not received")
            }
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonObject = JSONObject(response.body()!!.string())
                     mAccessToken = jsonObject.get("access_token").toString()
                    mAccessToken = AES.encrypt(mAccessToken!!, preferences)
                    preferences.edit().putString("access_token", mAccessToken).apply()
                     mTokenType = jsonObject.get("token_type").toString()
                    if (jsonObject.has("refresh_token")) {
                        mRefreshToken = jsonObject.get("refresh_token").toString()
                        mRefreshToken = AES.encrypt(mRefreshToken!!, preferences)
                        preferences.edit().putString("refresh_token", mRefreshToken).apply()
                        Log.e("Token", "refresh = ${preferences.getString("refresh_token","")}")
                        Log.e("Token","decrypt refresh = ${AES.decrypt(mRefreshToken, preferences.getString("aes_key",""))}")
                    }
                    Log.e("Token","acces = $mAccessToken")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getNewAccessToken() {
        val client = OkHttpClient()
        val requestBody: RequestBody = FormBody.Builder()
            .add("refresh_token", AES.decrypt(preferences.getString("refresh_token",""), preferences.getString("aes_key","")))
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("grant_type", "refresh_token")
            .build()
        val request: Request = Request.Builder()
            .url("https://www.googleapis.com/oauth2/v4/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonObject = JSONObject(response.body()!!.string())
                    preferences.edit().putString("access_token", AES.encrypt(jsonObject["access_token"].toString(), preferences)).apply()
                    Log.e("Tag", "new token = " + preferences.getString("access_token",""))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        })
    }

   @RequiresApi(Build.VERSION_CODES.O)
   fun getSubscriptions(){
       mAccessToken = AES.decrypt(preferences.getString("access_token",""), preferences.getString("aes_key",""))
        val client = OkHttpClient()
        val request: Request = Request.Builder()
            .url("https://www.googleapis.com/youtube/v3/subscriptions?access_token=$mAccessToken&part=snippet&mine=true")
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                if (e is HttpException) {
                    if (e.code() == 401) {
                        getNewAccessToken()
                    }
                }
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonObject = JSONObject(response.body()!!.string())
                    Log.e("Subscriptions","$jsonObject")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        })

    }




}