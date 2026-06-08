package com.example.debloater

import android.os.Bundle
import android.widget.Toast
import android.app.Activity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "Universal Debloater Initialized!", Toast.LENGTH_LONG).show()
    }
}

