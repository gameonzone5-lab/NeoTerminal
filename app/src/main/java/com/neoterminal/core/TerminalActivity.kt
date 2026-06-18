package com.neoterminal.core

import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.graphics.Color

class TerminalActivity : Activity() {
    private lateinit var outputText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var inputCommand: EditText
    private lateinit var runBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }
        outputText = TextView(this).apply { setTextColor(Color.GREEN); textSize = 14f; setPadding(16,16,16,16) }
        scrollView = ScrollView(this).apply { addView(outputText); layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        inputCommand = EditText(this).apply { hint = "root@alpine:~#"; setTextColor(Color.BLACK); setBackgroundColor(Color.LTGRAY) }
        runBtn = Button(this).apply { text = "EXECUTE" }
        rootLayout.addView(scrollView); rootLayout.addView(inputCommand); rootLayout.addView(runBtn)
        setContentView(rootLayout)

        outputText.text = "[*] NeoTerm Pro (NATIVE ALPINE CORE).\n"
        checkSystemStatus()

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                if (cmd.lowercase() == "hack" || cmd.lowercase() == "bootstrap") {
                    deployAlpinePayload()
                } else {
                    executeCommand(cmd)
                }
            }
            inputCommand.text.clear()
        }
    }

    private fun checkSystemStatus() {
        val rootfs = File(filesDir, "alpine-fs")
        val shFile = File(rootfs, "bin/sh")
        val prootFile = File(filesDir, "proot")

        runOnUiThread {
            if (!shFile.exists() || !prootFile.exists()) {
                outputText.append("[!] Alpine Linux missing. Type 'bootstrap' to deploy.\n")
            } else {
                outputText.append("[+] Pure Linux Core Ready! Try 'apk update'.\n")
            }
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun executeCommand(cmd: String) {
        runBtn.isEnabled = false
        outputText.append("\n$ $cmd\n")

        thread {
            try {
                val prootFile = File(filesDir, "proot")
                val rootfs = File(filesDir, "alpine-fs")

                val pb = ProcessBuilder()
                pb.directory(filesDir)
                pb.redirectErrorStream(true)

                val env = pb.environment()
                env.clear()
                env["PATH"] = "/bin:/usr/bin:/sbin:/usr/sbin"
                env["HOME"] = "/root"
                env["TERM"] = "xterm-256color"
                env["PROOT_NO_SECCOMP"] = "1"

                if (prootFile.exists() && File(rootfs, "bin/sh").exists()) {
                    pb.command(
                        prootFile.absolutePath, 
                        "--link2symlink",
                        "-0", 
                        "-r", rootfs.absolutePath, 
                        "-b", "/dev", 
                        "-b", "/proc", 
                        "-b", "/sys", 
                        "-w", "/root", 
                        "/bin/sh", "-c", cmd
                    )
                } else {
                    pb.command("sh", "-c", cmd)
                }

                val process = pb.start()
                val reader = process.inputStream.bufferedReader()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val sLine = line ?: ""
                    runOnUiThread { 
                        outputText.append(sLine + "\n")
                        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) } 
                    }
                }
                
                val exitCode = process.waitFor()
                runOnUiThread {
                    if(exitCode != 0) outputText.append("[DEBUG] Exit Code: $exitCode\n")
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            } catch (e: Exception) {
                runOnUiThread { outputText.append("[-] Engine Error: ${e.message}\n") }
            }
            runOnUiThread { runBtn.isEnabled = true }
        }
    }

    private fun downloadFile(urlStr: String, dest: File) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.connect()
        conn.inputStream.use { input -> FileOutputStream(dest).use { output -> input.copyTo(output) } }
        if (dest.length() == 0L) throw Exception("Downloaded file is 0 bytes.")
    }

    private fun deployAlpinePayload() {
        runBtn.isEnabled = false
        outputText.append("\n[*] INITIATING NATIVE DEPLOYMENT...\n")
        thread {
            try {
                val rootfs = File(filesDir, "alpine-fs")
                if (rootfs.exists()) rootfs.deleteRecursively()
                rootfs.mkdirs()

                runOnUiThread { outputText.append("[*] Downloading Static PRoot...\n") }
                val prootFile = File(filesDir, "proot")
                downloadFile("https://github.com/proot-me/proot/releases/download/v5.3.0/proot-v5.3.0-aarch64-static", prootFile)
                prootFile.setExecutable(true, false)

                runOnUiThread { outputText.append("[*] Downloading Pure Alpine Linux RootFS...\n") }
                val tarFile = File(filesDir, "alpine.tar.gz")
                downloadFile("https://dl-cdn.alpinelinux.org/alpine/v3.18/releases/aarch64/alpine-minirootfs-3.18.4-aarch64.tar.gz", tarFile)

                runOnUiThread { outputText.append("[*] Extracting Natively via Android OS tar...\n") }
                // CRITICAL FIX: Use Android's native tar, completely removing the failing busybox dependency
                val extractCmd = arrayOf("tar", "-xzf", tarFile.absolutePath, "-C", rootfs.absolutePath)
                val process = ProcessBuilder(*extractCmd).redirectErrorStream(true).directory(filesDir).start()
                
                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    runOnUiThread { outputText.append("  $line\n") }
                }
                val extCode = process.waitFor()
                if (extCode != 0) throw Exception("Android tar extraction failed with code $extCode")
                
                tarFile.delete()

                runOnUiThread {
                    outputText.append("\n[+] ALPINE LINUX INSTALLED FLAWLESSLY!\n")
                    checkSystemStatus()
                    runBtn.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    outputText.append("[-] Deployment Failed: ${e.message}\n")
                    runBtn.isEnabled = true
                }
            }
        }
    }
}
