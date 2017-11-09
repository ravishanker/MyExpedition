package com.raywenderlich.android.myexpedition

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.ar.core.Config
import com.google.ar.core.Session

class MainActivity : AppCompatActivity() {

    private lateinit var session: Session
    private lateinit var defaultConfig: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session = Session(this)

        // Create default config, check is supported, create session from that config.
        defaultConfig = Config.createDefaultConfig()
        if (!session.isSupported(defaultConfig)) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show()
            finish()
            return
        }

    }
}
