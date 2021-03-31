package com.libdumper

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.libdumper.databinding.ActivityMainBinding
import java.io.*
import java.util.*

/*
    Credit :
    Lib By kp7742 : https://github.com/kp7742
*/
class MainActivity : AppCompatActivity() {
    private var mBit = 0
    private var isRoot: Boolean = false
    private lateinit var bind: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.getRoot())
        reqStorageLD()
        copyFolder("armeabi-v7a")
        copyFolder("arm64-v8a")
        bind.Bit.setOnCheckedChangeListener { _, checkedId ->
            run {
                when (checkedId) {
                    R.id.b32 -> mBit = 32
                    R.id.b64 -> mBit = 64
                    else -> Toast.makeText(this, "failed by id", Toast.LENGTH_SHORT).show()
                }
            }
        }
        bind.isRoot.setOnCheckedChangeListener { _, checkedId ->
            run {
                when (checkedId) {
                    R.id.root -> isRoot = true
                    R.id.nonroot -> isRoot = false
                    else -> Toast.makeText(this, "failed by id", Toast.LENGTH_SHORT).show()
                }
            }
        }
        bind.dumpUE4.setOnClickListener {
            if (mBit == 32 || mBit == 64) {
                bind.Progress.visibility = View.VISIBLE
                runNative("ue4dumper", mBit)
            } else {
                Toast.makeText(this@MainActivity, "Please Select Arch", Toast.LENGTH_SHORT).show()
            }
        }
        bind.dumpil2cpp.setOnClickListener {
            when (mBit) {
                32 -> {
                    bind.Progress.visibility = View.VISIBLE
                    runNative("il2cppdumper", mBit)
                }
                64 -> {
                    Toast.makeText(
                        this@MainActivity,
                        "Il2cppdump is not Support For arm64",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> Toast.makeText(this@MainActivity, "Please Select Arch", Toast.LENGTH_SHORT).show()
            }
        }
        bind.floatingActionButton.setOnClickListener {
            startActivity(Intent(ACTION_VIEW, Uri.parse("https://github.com/MrPictYT-art/LibDumper")))
        }
    }

    private fun reqStorageLD() {
        try {
            Runtime.getRuntime().exec("su")
        }catch (e: Exception){
            e.printStackTrace()
        }
        val permission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                10
            )
        }
    }

    private fun runNative(str: String, megaBit: Int) {
        var path = "${filesDir.path}/"
        if (megaBit == 32) {
            path += "armeabi-v7a/$str"
        } else if (megaBit == 64) {
            path += "arm64-v8a/$str"
        }
        Thread {
            val process = if (isRoot)
                Runtime.getRuntime().exec(
                    arrayOf(
                        "su",
                        "-c",
                        path,
                        "--package",
                        bind.pkg.text.toString(),
                        "--lib"
                    )
                )
            else
                Runtime.getRuntime().exec(
                    arrayOf(
                        path,
                        "--package",
                        bind.pkg.text.toString(),
                        "--lib"
                    )
                )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            runOnUiThread {
                bind.log.text = ""
                reader.useLines {
                    it.forEach { str->
                        bind.log.append("\n$str")
                    }
                }
                reader.close()
                bind.Progress.visibility = View.GONE
            }
        }.start()
    }

    private fun copyFolder(name: String) {
        val files: Array<String>? = assets.list(name)
        for (filename in files!!) {
            var `in`: InputStream?
            var out: OutputStream?
            val folder = File("${filesDir.path}/$name")
            var success = true
            if (!folder.exists()) {
                success = folder.mkdir()
            }
            if (success) {
                try {
                    `in` = assets.open("$name/$filename")
                    val file = File("${filesDir.path}/$name/$filename")
                    out = FileOutputStream(file)
                    `in`.copyTo(out)
                    `in`.close()
                    out.flush()
                    out.close()
                    Runtime.getRuntime().exec(arrayOf("chmod", "777", file.path))
                } catch (e: IOException) {
                    Log.e("ERROR", "Failed to copy asset file: $filename", e)
                }
            }
        }
    }
}