package com.ivangames.voxelgrit

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapView = findViewById<MapView>(R.id.mapView)
        val btnZoomIn = findViewById<Button>(R.id.btnZoomIn)
        val btnZoomOut = findViewById<Button>(R.id.btnZoomOut)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnZoomIn.setOnClickListener { mapView.zoomIn() }
        btnZoomOut.setOnClickListener { mapView.zoomOut() }
        btnBack.setOnClickListener { finish() } // Закрыть игру
    }
}
