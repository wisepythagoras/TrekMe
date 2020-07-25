package com.peterlaurence.trekme.util.gpx

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.peterlaurence.trekme.MainActivity


class GpxReceiveActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data
        println("GpxReceiveActivity URI : ")
        println(data)

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = "import_gpx"
        intent.data = data
        startActivity(intent)
        finish()
    }
}