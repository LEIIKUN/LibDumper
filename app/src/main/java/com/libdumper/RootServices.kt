package com.libdumper

import android.content.Intent
import android.os.*
import android.util.Log
import com.kmrite.Tools
import com.topjohnwu.superuser.ipc.RootService
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException

class RootServices : RootService(), Handler.Callback {

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "RootServices: onBind")
        val h = Handler(Looper.getMainLooper(), this)
        val m = Messenger(h)
        return m.binder
    }

    override fun handleMessage(msg: Message): Boolean {
        if (msg.what != MSG_GETINFO) return false
        val reply = Message.obtain()
        val data = Bundle()
        val pkg = msg.data.getString("pkg")
        val file = msg.data.getString("file_dump")
        data.putString("result", Tools.dumpFile(pkg!!, file!!))
        reply.data = data
        try {
            msg.replyTo.send(reply)
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote error", e)
        }
        return false
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "RootServices: onUnbind, client process unbound")
        // Default returns false, which means NOT daemon mode
        return super.onUnbind(intent)
    }

    companion object {
        const val TAG = "IPC"
        const val MSG_GETINFO = 1
    }
}