package com.neoterminal.core

import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream
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

        setupTermuxEnvironment()

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.lowercase() == "bootstrap") {
                installTermuxBootstrap()
            } else if (cmd.isNotEmpty()) {
                runShellCommandLive(cmd)
            }
            inputCommand.text.clear()
        }

        outputText.text = "[*] NeoTerm Pro Active (PRoot Virtual Bridge).\n[*] Type 'bootstrap' to install Engine.\n"
    }

    private fun setupTermuxEnvironment() {
        val usrDir = File(filesDir, "usr")
        val homeDir = File(filesDir, "home")
        if (!usrDir.exists()) usrDir.mkdirs()
        if (!homeDir.exists()) homeDir.mkdirs()
    }

    private fun runShellCommandLive(cmd: String) {
        runBtn.isEnabled = false
        outputText.append("\n$ $cmd\n")

        thread {
            try {
                val prootFile = File(filesDir, "proot")

                // Virtual Environment mapping exactly what Termux binaries expect
                val envList = mutableListOf(
                    "PATH=/data/data/com.termux/files/usr/bin:/system/bin:/system/xbin",
                    "PREFIX=/data/data/com.termux/files/usr",
                    "LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib",
                    "HOME=/data/data/com.termux/files/home",
                    "TERM=xterm-256color"
                )

                // If PRoot is installed, bind-mount our directory to fake the com.termux path
                val finalCmdArray = if (prootFile.exists()) {
                    arrayOf(
                        prootFile.absolutePath,
                        "-b", "${filesDir.absolutePath}:/data/data/com.termux/files",
                        "/data/data/com.termux/files/usr/bin/sh", "-c", cmd
                    )
                } else {
                    arrayOf("sh", "-c", cmd)
                }

                val process = Runtime.getRuntime().exec(finalCmdArray, envList.toTypedArray(), filesDir)

                val reader = process.inputStream.bufferedReader()
                val errorReader = process.errorStream.bufferedReader()

                var line: String?
                var lineCount = 0

                while (reader.readLine().also { line = it } != null) {
                    val safeLine = line ?: ""
                    runOnUiThread {
                        outputText.append(safeLine + "\n")
                        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                    }
                    lineCount++
                    if (lineCount > 2000) { process.destroy(); break }
                }

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

    private fun installTermuxBootstrap() {
        runBtn.isEnabled = false
        outputText.append("\n[*] Downloading Termux Bootstrap & PRoot Engine...\n")

        thread {
            try {
                // 1. Download and Extract Official Termux Bootstrap
                val usrDir = File(filesDir, "usr")
                val url = URL("https://github.com/termux/termux-packages/releases/latest/download/bootstrap-aarch64.zip")
                val zipStream = ZipInputStream(url.openStream())
                var entry = zipStream.nextEntry

                while (entry != null) {
                    val file = File(usrDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { output ->
                            zipStream.copyTo(output)
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
                zipStream.close()
                File(usrDir, "bin").listFiles()?.forEach { it.setExecutable(true) }

                // 2. Download PRoot Engine for Linker Bypass
                runOnUiThread { outputText.append("[*] Configuring Virtual Bridge...\n") }
                val prootFile = File(filesDir, "proot")
                val prootUrl = URL("https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/tar/proot/proot-aarch64")
                prootUrl.openStream().use { input -> FileOutputStream(prootFile).use { output -> input.copyTo(output) } }
                prootFile.setExecutable(true)

                runOnUiThread {
                    outputText.append("[+] Termux Core & PRoot Engine Installed! 🎉\n")
                    outputText.append("[+] Linker Bypass Active. APT is ready.\n")
                    outputText.append("[+] Try command: 'apt update'\n")
                    runBtn.isEnabled = true
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    outputText.append("[-] Setup Error: ${e.message}\n")
                    runBtn.isEnabled = true
                }
            }
        }
    }
}
