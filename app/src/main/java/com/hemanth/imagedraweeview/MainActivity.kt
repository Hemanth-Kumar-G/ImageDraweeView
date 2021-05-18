package com.hemanth.imagedraweeview

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var draweeView: ImageDraweeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        draweeView = findViewById(R.id.photo_drawee_view)
        draweeView.setOnDoubleTapListener(null);
        draweeView.setPhotoUri(Uri.parse("res:///" + R.drawable.panda),this)

        draweeView.setOnPhotoTapListener(object : OnPhotoTapListener {
            override fun onPhotoTap(view: View, x: Float, y: Float) {
                Toast.makeText(
                    view.context, "onPhotoTap :  x =  $x; y = $y",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        draweeView.setOnViewTapListener(object : OnViewTapListener {
            override fun onViewTap(view: View, x: Float, y: Float) {
                Toast.makeText(view.context, "onViewTap", Toast.LENGTH_SHORT).show()
            }
        })

        draweeView.setOnLongClickListener { v ->
            Toast.makeText(v.context, "onLongClick", Toast.LENGTH_SHORT).show()
            true
        }
    }
}
