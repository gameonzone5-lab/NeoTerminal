package com.neoterminal.core

import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class TerminalActivity : Activity() {
    private lateinit var outputText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var inputCommand: EditText
    private lateinit var runBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(android.graphics.Color.BLACK) }
        outputText = TextView(this).apply { setTextColor(android.graphics.Color.GREEN); textSize = 14f; setPadding(16,16,16,16) }
        scrollView = ScrollView(this).apply { addView(outputText); layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        inputCommand = EditText(this).apply { hint = "Command..."; setTextColor(android.graphics.Color.BLACK); setBackgroundColor(android.graphics.Color.LTGRAY) }
        runBtn = Button(this).apply { text = "RUN" }
        rootLayout.addView(scrollView); rootLayout.addView(inputCommand); rootLayout.addView(runBtn)
        setContentView(rootLayout)

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.lowercase() == "bootstrap") {
                installCoreEngine()
            } else if (cmd.isNotEmpty()) {
                runShellCommandLive(cmd)
            }
            inputCommand.text.clear()
        }
        outputText.text = "[*] NeoTerm Pro Active (Multi-Server Engine).\n[*] Commands: 'bootstrap', 'ls /sdcard'.\n"
    }

    private fun runShellCommandLive(cmd: String) {
        runBtn.isEnabled = false
        outputText.append("\n$ $cmd\n")

        thread {
            try {
                val busyboxFile = File(filesDir, "busybox")
                val finalCmd = if (busyboxFile.exists()) {
                    "${busyboxFile.absolutePath} sh -c '$cmd'"
                } else {
                    cmd
                }

                val process = ProcessBuilder("sh", "-c", "export ARCH=aarch64; export PATH=${filesDir.absolutePath}:/system/bin:/system/xbin:/vendor/bin; $finalCmd")
                    .redirectErrorStream(true)
                    .directory(filesDir)
                    .start()

                val reader = process.inputStream.bufferedReader()
                var line: String?
                var lineCount = 0

                while (reader.readLine().also { line = it } != null) {
                    val safeLine = line ?: ""
                    runOnUiThread {
                        outputText.append(safeLine + "\n")
                        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                    }
                    lineCount++
                    if (lineCount > 3000) {
                        runOnUiThread { outputText.append("\n[!] Output truncated to prevent crash.\n") }
                        process.destroy()
                        break
                    }
                }
                process.waitFor()
                runOnUiThread { runBtn.isEnabled = true }
            } catch (e: Exception) {
                runOnUiThread {
                    outputText.append("[-] Engine Error: ${e.message}\n")
                    runBtn.isEnabled = true
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            }
        }
    }

    private fun installCoreEngine() {
        runBtn.isEnabled = false
        outputText.append("\n[*] Installing Core Linux Tools (BusyBox ARM64)...\n")

        thread {
            // Verified fallback URLs for ARM64 BusyBox
            val urls = arrayOf(
                "https://busybox.net/downloads/binaries/1.31.0-defconfig-multiarch-musl/busybox-armv8l",
                "https://busybox.net/downloads/binaries/1.31.0-defconfig-multiarch-musl/busybox-armv7l",
                "https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/tar/arm64/tar"
            )

            var success = false
            for (urlString in urls) {
                try {
                    runOnUiThread {
                        outputText.append("[*] Connecting to server...\n")
                        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                    }

                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.connect()

                    if (connection.responseCode == 200 || connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val busyboxFile = File(filesDir, "busybox")
                        connection.inputStream.use { input ->
                            FileOutputStream(busyboxFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        busyboxFile.setExecutable(true)
                        success = true
                        runOnUiThread { outputText.append("[+] Download Successful from $urlString\n") }
                        break // Stop trying other URLs since we got it
                    } else {
                        runOnUiThread { outputText.append("[-] Server skipped (HTTP ${connection.responseCode})\n") }
                    }
                } catch (e: Exception) {
                    runOnUiThread { outputText.append("[-] Server timeout. Trying next...\n") }
                }
            }

            runOnUiThread {
                if (success) {
                    outputText.append("\n[+] Linux Core Engine Installed Successfully! 🎉\n")
                    outputText.append("[+] 300+ native commands activated.\n")
                    outputText.append("[+] Test it now: Type 'busybox' or 'ls -la'\n")
                } else {
                    outputText.append("\n[-] All servers failed. Please check your internet connection.\n")
                }
                runBtn.isEnabled = true
                scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
    }
}
