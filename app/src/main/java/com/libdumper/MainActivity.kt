package com.libdumper

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.kmrite.Tools
import com.libdumper.databinding.ActivityMainBinding
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService


class MainActivity : AppCompatActivity(), Handler.Callback {
    lateinit var bind: ActivityMainBinding
    private var consoleList: AppendCallbackList = AppendCallbackList()
    private val myMessenger = Messenger(Handler(Looper.getMainLooper(), this))
    var remoteMessenger: Messenger? = null
    private var serviceTestQueued = false
    private var conn: MSGConnection? = null

    companion object {
        const val TAG = "LibDumper"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shell.rootAccess()
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        reqStorageLD()
        bind.dumpUE4.setOnClickListener {
            runNative(
                if (bind.libName.text.isNullOrBlank()) {
                    "libUE4.so"
                } else {
                    bind.libName.text.toString()
                }
            )
        }
        bind.dumpil2cpp.setOnClickListener {
            if (bind.metadata.isChecked) {
                runNative("global-metadata.dat")
            }

            runNative(
                if (bind.libName.text.isNullOrBlank()) {
                    "libil2cpp.so"
                } else {
                    bind.libName.text.toString()
                }
            )
        }
        bind.github.setOnClickListener {
            startActivity(
                Intent(
                    ACTION_VIEW,
                    Uri.parse("https://github.com/MrPictYT-art/LibDumper")
                )
            )
        }
    }

    private fun reqStorageLD() {
        val permission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                10
            )
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        val dump = msg.data.getString("result")
        consoleList.add(dump)
        return false
    }

    private fun runNative(file: String) {
        if (bind.pkg.text != null) {
            if (Shell.rootAccess()) {
                tryBindService(file)
            } else {
                bind.console.text = Tools.dumpFile(bind.pkg.text.toString(), file)
            }
        } else {
            Toast.makeText(this, "put pkg name please", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryBindService(file: String) {
        if (remoteMessenger == null) {
            serviceTestQueued = true
            val intent = Intent(this, RootServices::class.java)
            conn = MSGConnection(bind.pkg.text.toString(), file)
            RootService.bind(intent, conn!!)
            return
        }
        testService(bind.pkg.text.toString(), file)
    }

    private fun testService(pkg: String, file: String) {
        val message: Message = Message.obtain(null, RootServices.MSG_GETINFO)
        message.data.putString("pkg", pkg)
        message.data.putString("file_dump", file)
        message.replyTo = myMessenger
        try {
            remoteMessenger?.send(message)
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote error", e)
        }
    }

    inner class MSGConnection(private var pkg: String, var lib: String) : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "service onServiceConnected")
            remoteMessenger = Messenger(service)
            if (serviceTestQueued) {
                serviceTestQueued = false
                testService(pkg, lib)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "service onServiceDisconnected")
            remoteMessenger = null
        }
    }

    inner class AppendCallbackList : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            bind.console.append(s)
            bind.console.append("\n")
            bind.sv.postDelayed({ bind.sv.fullScroll(ScrollView.FOCUS_DOWN) }, 10)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (conn != null) {
            RootService.unbind(conn!!)
        }
    }
}