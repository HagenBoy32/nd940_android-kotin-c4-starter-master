package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
//import com.firebase.ui.auth.viewmodel.RequestCodes
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig.Prompt.SIGN_IN
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import kotlinx.android.synthetic.main.activity_authentication.*
import com.udacity.project4.utils.RequestCodes
/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser != null) {
            navigateToRemindersActivity()
            return
        }

        setContentView((R.layout.activity_authentication))
        findViewById<View>(R.id.auth_button).setOnClickListener { onAuthButtonClicked() }

    }

    private fun navigateToRemindersActivity() {
        startActivity(Intent(this, RemindersActivity::class.java))
        finish()
    }

    private fun onAuthButtonClicked() {
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(
                    listOf(
                        AuthUI.IdpConfig.GoogleBuilder().build(),
                        AuthUI.IdpConfig.EmailBuilder().build()
                    )
                )
                .setAuthMethodPickerLayout(
                    AuthMethodPickerLayout
                        .Builder(R.layout.login_screen)
                        .setGoogleButtonId(R.id.google_button)
                        .setEmailButtonId(R.id.email_button)
                        .build()
                )
                .setTheme(R.style.AppTheme)
                .build(), RequestCodes.SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        val response = IdpResponse.fromResultIntent(data)
        if (resultCode == Activity.RESULT_OK) {
            Log.d("<<AuthActivity>>",
                "onActivityResult: " + "${FirebaseAuth.getInstance().currentUser?.displayName}!"
            )
            val intent = Intent(this, RemindersActivity::class.java)
            startActivity(intent)
        } else {
            Log.d("<<AuthActivity>>", "Successful sign in $response?.error.errorCode}" )
        }

        if (requestCode != RequestCodes.SIGN_IN) {
            return
        }

        if (resultCode == RESULT_OK) {
            navigateToRemindersActivity()
            return
        }


    }

}
