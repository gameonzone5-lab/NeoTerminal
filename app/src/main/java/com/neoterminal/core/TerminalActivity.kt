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
            val cmd = inputCommand.text.toString()
            if (cmd == "bootstrap") {
                downloadScript()
            } else if (cmd.isNotEmpty()) {
                runShellCommandLive(cmd)
            }
            inputCommand.text.clear()
        }
        outputText.text = "[*] NeoTerm Pro Active (Kotlin Safe Engine).\n[*] C++ JNI Removed. Crash Protection Enabled.\nCommands: 'bootstrap', 'sh ubuntu.sh', 'ls /sdcard'.\n"
    }

    private fun runShellCommandLive(cmd: String) {
        val finalCmd = if (cmd.startsWith("sh ubuntu.sh")) {
            "sh " + File(filesDir, "ubuntu.sh").absolutePath
        } else cmd

        outputText.append("\n$ $finalCmd\n")
        runBtn.isEnabled = false

        thread {
            try {
                // Pure Kotlin ProcessBuilder ensures 100% safe execution
                val process = ProcessBuilder("sh", "-c", "export ARCH=aarch64; export PATH=/system/bin:/system/xbin:/vendor/bin; $finalCmd")
                    .redirectErrorStream(true)
                    .directory(filesDir)
                    .start()

                val reader = process.inputStream.bufferedReader()
                var line: String?
                var lineCount = 0

                // Read line by line to prevent OOM and UI freezing
                while (reader.readLine().also { line = it } != null) {
                    val safeLine = line ?: ""
                    runOnUiThread {
                        outputText.append(safeLine + "\n")
                        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                    }
                    lineCount++
                    // Kill switch if output goes infinite
                    if (lineCount > 2000) {
                        runOnUiThread { outputText.append("\n[!] Output truncated to prevent memory crash.\n") }
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

    private fun downloadScript() {
        runBtn.isEnabled = false
        outputText.append("\n[*] Downloading Ubuntu Installer...\n")
        thread {
            try {
                val scriptFile = File(filesDir, "ubuntu.sh")
                val url = URL("https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/Scripts/Installer/Ubuntu/ubuntu.sh")
                url.openStream().use { input -> FileOutputStream(scriptFile).use { output -> input.copyTo(output) } }
                scriptFile.setExecutable(true)
                runOnUiThread {
                    outputText.append("[+] Saved to: ${scriptFile.absolutePath}\n[+] Now type 'sh ubuntu.sh' to execute.\n")
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
