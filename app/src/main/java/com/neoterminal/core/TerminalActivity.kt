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

    // Termux Architecture Variables
    private lateinit var prefixDir: File
    private lateinit var binDir: File
    private lateinit var libDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(android.graphics.Color.BLACK) }
        outputText = TextView(this).apply { setTextColor(android.graphics.Color.GREEN); textSize = 14f; setPadding(16,16,16,16) }
        scrollView = ScrollView(this).apply { addView(outputText); layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        inputCommand = EditText(this).apply { hint = "Command..."; setTextColor(android.graphics.Color.BLACK); setBackgroundColor(android.graphics.Color.LTGRAY) }
        runBtn = Button(this).apply { text = "RUN" }
        rootLayout.addView(scrollView); rootLayout.addView(inputCommand); rootLayout.addView(runBtn)
        setContentView(rootLayout)

        setupTermuxEnvironment()

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                runShellCommandLive(cmd)
            }
            inputCommand.text.clear()
        }

        outputText.text = "[*] NeoTerm Pro Active (Termux NDK Architecture).\n[*] PREFIX: ${prefixDir.absolutePath}\n[*] Ready for Bionic Android binaries. Try 'ls /sdcard' or 'pwd'.\n"
    }

    private fun setupTermuxEnvironment() {
        // Create exact Termux folder structure
        prefixDir = File(filesDir, "usr")
        binDir = File(prefixDir, "bin")
        libDir = File(prefixDir, "lib")
        val tmpDir = File(prefixDir, "tmp")

        if (!prefixDir.exists()) prefixDir.mkdirs()
        if (!binDir.exists()) binDir.mkdirs()
        if (!libDir.exists()) libDir.mkdirs()
        if (!tmpDir.exists()) tmpDir.mkdirs()
    }

    private fun runShellCommandLive(cmd: String) {
        runBtn.isEnabled = false
        outputText.append("\n$ $cmd\n")

        thread {
            try {
                // Setup identical environment to Termux
                val envList = mutableListOf(
                    "PATH=${binDir.absolutePath}:/sbin:/system/sbin:/system/bin:/system/xbin:/vendor/bin:/vendor/xbin",
                    "PREFIX=${prefixDir.absolutePath}",
                    "LD_LIBRARY_PATH=${libDir.absolutePath}",
                    "TMPDIR=${File(prefixDir, "tmp").absolutePath}",
                    "HOME=${filesDir.absolutePath}",
                    "TERM=xterm-256color"
                )

                // Execute process with custom Termux environment
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd), envList.toTypedArray(), filesDir)

                val reader = process.inputStream.bufferedReader()
                val errorReader = process.errorStream.bufferedReader()

                var line: String?
                var lineCount = 0

                // Read stdout
                while (reader.readLine().also { line = it } != null) {
                    val safeLine = line ?: ""
                    runOnUiThread {
                        outputText.append(safeLine + "\n")
                        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                    }
                    lineCount++
                    if (lineCount > 2000) { process.destroy(); break }
                }

                // Read stderr (errors)
                while (errorReader.readLine().also { line = it } != null) {
                    val safeLine = line ?: ""
                    runOnUiThread {
                        outputText.append(safeLine + "\n")
                        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
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
