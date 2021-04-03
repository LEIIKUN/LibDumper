package com.libdumper

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.kmrite.Tools.Companion.dumpFile
import com.libdumper.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var bind: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        reqStorageLD()
        bind.dumpUE4.setOnClickListener {
            runNative(if (bind.libName.text.isNullOrBlank()) {
                "libUE4.so"
            } else {
                bind.libName.text.toString()
            })
        }
        bind.dumpil2cpp.setOnClickListener {
            if (bind.metadata.isChecked){
                runNative("global-metadata.dat")
            }
            runNative(if (bind.libName.text.isNullOrBlank()) {
                "libil2cpp.so"
            } else {
                bind.libName.text.toString()
            })
        }
        bind.github.setOnClickListener {
            startActivity(Intent(ACTION_VIEW, Uri.parse("https://github.com/MrPictYT-art/LibDumper")))
        }
    }

    private fun reqStorageLD() {
        val permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 10)
        }
    }

    private fun runNative(file: String) {
        bind.console.dumpFile(bind.pkg.text.toString(), file)
    }
}