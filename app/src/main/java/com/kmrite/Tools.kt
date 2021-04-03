package com.kmrite

import android.widget.TextView
import java.io.*
import java.nio.ByteBuffer

/*
    An Modified Tools.kt from "https://github.com/MrPictYT-art/KMrite"
*/
data class Memory(val pkg: String) {
    var pid: Int = 0
    var sAddress: Long = 0L
    var eAddress: Long = 0L
}

class Tools {
    companion object {
        private const val TAG = "TOOLS : "
        fun TextView.dumpFile(pkg: String, file: String) {
            this.append("Begin Dumping For $file\n")
            val mem = Memory(pkg)
            getProcessID(mem)
            this.append("PID : ${mem.pid}\n")
            if (mem.pid > 1 && mem.sAddress < 1L) {
                parseMap(mem, file)
                parseMapEnd(mem, file)
                this.append(
                    "Start Address : ${longToHex(mem.sAddress)}\nEnd Address : ${
                        longToHex(
                            mem.eAddress
                        )
                    }\n"
                )
                if (mem.sAddress > 1L) {
                    RandomAccessFile("/proc/${mem.pid}/mem", "r").use { mems ->
                        mems.channel.use {
                            this.append("Saving...\n")
                            val buff: ByteBuffer =
                                ByteBuffer.allocate((mem.eAddress - mem.sAddress).toInt())
                            it.read(buff, mem.sAddress)
                            FileOutputStream("/sdcard/$file").use { out ->
                                out.write(buff.array())
                                out.close()
                            }
                            this.append("Result : /sdcard/$file\n\n")
                        }
                    }
                }
            }
        }

        private fun longToHex(ling: Long): String {
            return Integer.toHexString(ling.toInt())
        }

        private fun parseMap(nmax: Memory, lib_name: String) {
            val fil = File("/proc/${nmax.pid}/maps")
            if (fil.exists()) {
                fil.useLines { liness ->
                    val start = liness.find { it.contains(lib_name) }
                    if (start?.isBlank() == false && nmax.sAddress == 0L) {
                        val regex = "\\p{XDigit}+".toRegex()
                        val result = regex.find(start)?.value!!
                        nmax.sAddress = result.toLong(16)
                    }
                }
            } else {
                throw FileNotFoundException("FAILED OPEN DIRECTORY : ${fil.path}")
            }
        }

        private fun parseMapEnd(nmax: Memory, lib_name: String) {
            val fil = File("/proc/${nmax.pid}/maps")
            if (fil.exists()) {
                fil.useLines { liness ->
                    val end = liness.findLast { it.contains(lib_name) }
                    if (end?.isBlank() == false && nmax.eAddress == 0L) {
                        val regex = "\\p{XDigit}+-\\p{XDigit}+".toRegex()
                        val result = regex.find(end)?.value!!.split("-")
                        nmax.eAddress = result[1].toLong(16)
                    }
                }
            } else {
                throw FileNotFoundException("FAILED OPEN DIRECTORY : ${fil.path}")
            }
        }

        private fun getProcessID(nmax: Memory) {
            val process = Runtime.getRuntime().exec(arrayOf("pidof", nmax.pkg))
            val reader = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            val buff = reader.readLine()
            reader.close()
            process.waitFor()
            process.destroy()
            nmax.pid = if (buff != null && buff.isNotEmpty()) buff.toInt() else 0
        }
    }
}