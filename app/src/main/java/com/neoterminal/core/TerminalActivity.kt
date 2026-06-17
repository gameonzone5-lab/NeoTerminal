package com.neoterminal.core

import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.io.File
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

        // CLEANUP: Remove the incompatible SECCOMP-violating payload
        val badBusybox = File(filesDir, "busybox")
        if (badBusybox.exists()) badBusybox.delete()
        val badUbuntu = File(filesDir, "ubuntu.sh")
        if (badUbuntu.exists()) badUbuntu.delete()

        runBtn.setOnClickListener {
            val rawCmd = inputCommand.text.toString()
            val cmd = rawCmd.trim()

            if (cmd.isNotEmpty()) {
                runShellCommandLive(cmd)
            }
            inputCommand.text.clear()
        }
        outputText.text = "[*] NeoTerm Pro Active (Pure Native Engine).\n[*] Incompatible payloads removed. Try 'ls /system/bin' or 'ping google.com'.\n"
    }

    private fun runShellCommandLive(cmd: String) {
        runBtn.isEnabled = false
        outputText.append("\n$ $cmd\n")

        thread {
            try {
                // Execute using pure native Android shell and binaries
                val process = ProcessBuilder("sh", "-c", "export PATH=/sbin:/system/sbin:/system/bin:/system/xbin:/vendor/bin:/vendor/xbin; $cmd")
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
}
