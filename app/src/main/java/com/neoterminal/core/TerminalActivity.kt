package com.neoterminal.core

import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.io.File
import java.io.FileOutputStream
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
            // CRITICAL FIX: Trim spaces and handle formatting
            val rawCmd = inputCommand.text.toString()
            val cmd = rawCmd.trim()
            val cmdLower = cmd.lowercase()

            if (cmdLower == "bootstrap") {
                installCoreEngine()
            } else if (cmd.isNotEmpty()) {
                runShellCommandLive(cmd)
            }
            inputCommand.text.clear()
        }
        outputText.text = "[*] NeoTerm Pro Active (Kotlin Engine).\n[*] Commands: 'bootstrap', 'ls /sdcard'.\n"
    }

    private fun runShellCommandLive(cmd: String) {
        runBtn.isEnabled = false
        outputText.append("\n$ $cmd\n")

        thread {
            try {
                val busyboxFile = File(filesDir, "busybox")
                // If BusyBox is installed, use its shell to run commands so we get 300+ linux tools natively
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
                    if (lineCount > 2000) {
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
            try {
                val busyboxFile = File(filesDir, "busybox")
                // Official static ARM64 BusyBox URL
                val url = URL("https://busybox.net/downloads/binaries/1.35.0-aarch64-linux-musl/busybox")
                url.openStream().use { input -> FileOutputStream(busyboxFile).use { output -> input.copyTo(output) } }
                busyboxFile.setExecutable(true)

                runOnUiThread {
                    outputText.append("[+] Linux Core Installed Successfully!\n")
                    outputText.append("[+] You now have 300+ native commands (wget, tar, dpkg, awk, etc).\n")
                    outputText.append("[+] Try commands like: 'wget google.com' or 'ls -la'\n")
                    runBtn.isEnabled = true
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    outputText.append("[-] Download error: ${e.message}\n")
                    runBtn.isEnabled = true
                }
            }
        }
    }
}
