package com.example.oauth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    var mApiClient: GoogleApiClient? = null
    val RC_AUTH_CODE = 1
    var gso: GoogleSignInOptions? = null
    var oauth: Oauth? = null
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


     gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestServerAuthCode(getString(R.string.server_client_id))
        .requestEmail()
         .requestScopes(Scope("https://www.googleapis.com/auth/youtube.readonly"))
        .build()

    sign_in_button.setOnClickListener {
        sighIn()
        sign_in_button.visibility = View.INVISIBLE
        get_data_button.visibility = View.VISIBLE
    }
        get_data_button.setOnClickListener {
            oauth!!.getSubscriptions()
        }

    mApiClient = GoogleApiClient.Builder(this)
        .enableAutoManage(this) { this }
        .addApi(Auth.GOOGLE_SIGN_IN_API, gso!!)
        .build()
    }

    fun sighIn(){
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mApiClient)
        startActivityForResult(signInIntent, RC_AUTH_CODE)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_AUTH_CODE) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val acct = result.signInAccount
                val authCode = acct!!.serverAuthCode
                oauth = Oauth(getString(R.string.server_client_id), getString(R.string.client_secret), this)
                oauth!!.getAccessToken(authCode)
            } else Log.e("TAG", "Result is wrong")
        }
    }








}
