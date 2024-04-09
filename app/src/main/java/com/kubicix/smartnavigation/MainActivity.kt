package com.kubicix.smartnavigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity" // Tag for logging messages

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Log a message when the activity is created
        Log.d(TAG, "onCreate called")

        // Example of logging a toast message
        val toastMessage = "Activity started!"
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        Log.i(TAG, toastMessage)
    }
}