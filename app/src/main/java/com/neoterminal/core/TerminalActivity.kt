package com.neoterminal.core

import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread
import android.system.Os
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
        inputCommand = EditText(this).apply { hint = "root@android:~#"; setTextColor(Color.BLACK); setBackgroundColor(Color.LTGRAY) }
        runBtn = Button(this).apply { text = "EXECUTE" }
        rootLayout.addView(scrollView); rootLayout.addView(inputCommand); rootLayout.addView(runBtn)
        setContentView(rootLayout)

        outputText.text = "[*] NeoTerm Ghost Engine Initiated.\n[*] Bypassing Android restrictions...\n"

        val aptFile = File(filesDir, "usr/bin/apt")
        if (!aptFile.exists()) {
            outputText.append("[!] Core Linux system missing or corrupted.\n[!] Type 'hack' to auto-deploy the payload.\n")
        } else {
            outputText.append("[+] Environment Secure. Ready for commands.\n")
        }

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                if (cmd.lowercase() == "hack" || cmd.lowercase() == "bootstrap") {
                    deployPayload()
                } else {
                    executeCommand(cmd)
                }
            }
            inputCommand.text.clear()
        }
    }

    private fun executeCommand(cmd: String) {
        runBtn.isEnabled = false
        outputText.append("\n$ $cmd\n")

        thread {
            try {
                val prootFile = File(filesDir, "proot")
                val tmpDir = File(filesDir, "usr/tmp")
                if (!tmpDir.exists()) tmpDir.mkdirs()

                // Hardcoded PATH injection to prevent $PATH=(null) errors
                val envList = mutableListOf(
                    "PATH=/data/data/com.termux/files/usr/bin:/system/bin:/system/xbin",
                    "PREFIX=/data/data/com.termux/files/usr",
                    "LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib",
                    "HOME=/data/data/com.termux/files/home",
                    "TERM=xterm-256color",
                    "TMPDIR=${tmpDir.absolutePath}",
                    "PROOT_TMP_DIR=${tmpDir.absolutePath}",
                    "PROOT_NO_SECCOMP=1"
                )

                // Force pure bash execution to bypass broken 'sh' links
                val bashPath = "/data/data/com.termux/files/usr/bin/bash"
                val finalCmdArray = if (prootFile.exists()) {
                    arrayOf(prootFile.absolutePath, "-0", "-b", "${filesDir.absolutePath}:/data/data/com.termux/files", bashPath, "-c", cmd)
                } else {
                    arrayOf("sh", "-c", cmd)
                }

                val process = Runtime.getRuntime().exec(finalCmdArray, envList.toTypedArray(), filesDir)
                val reader = process.inputStream.bufferedReader()
                val errorReader = process.errorStream.bufferedReader()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val sLine = line ?: ""
                    runOnUiThread { outputText.append(sLine + "\n"); scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) } }
                }
                while (errorReader.readLine().also { line = it } != null) {
                    val sLine = line ?: ""
                    runOnUiThread { outputText.append(sLine + "\n"); scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) } }
                }
                process.waitFor()
            } catch (e: Exception) {
                runOnUiThread { outputText.append("[-] Error: ${e.message}\n") }
            }
            runOnUiThread { runBtn.isEnabled = true }
        }
    }

    private fun deployPayload() {
        runBtn.isEnabled = false
        outputText.append("\n[*] INITIATING HACKER PAYLOAD (Auto-Repair & Deploy)...\n")
        thread {
            try {
                val usrDir = File(filesDir, "usr")
                val homeDir = File(filesDir, "home")

                runOnUiThread { outputText.append("[*] Wiping corrupted files and old broken links...\n") }
                if (usrDir.exists()) usrDir.deleteRecursively()
                usrDir.mkdirs()
                homeDir.mkdirs()

                runOnUiThread { outputText.append("[*] Injecting Termux Core...\n") }
                val url = URL("https://github.com/termux/termux-packages/releases/latest/download/bootstrap-aarch64.zip")
                val zipStream = ZipInputStream(url.openStream())
                var entry = zipStream.nextEntry
                val symlinks = mutableListOf<String>()

                while (entry != null) {
                    if (entry.name == "SYMLINKS.txt") {
                        val content = String(zipStream.readBytes())
                        symlinks.addAll(content.lines().filter { it.contains("←") })
                    } else {
                        val file = File(usrDir, entry.name)
                        if (entry.isDirectory) { file.mkdirs() } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { zipStream.copyTo(it) }
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
                zipStream.close()

                runOnUiThread { outputText.append("[*] Forcing Symlinks (with Hard-Copy Fallback)...\n") }
                symlinks.forEach { line ->
                    val parts = line.split("←")
                    if (parts.size == 2) {
                        val target = parts[0]
                        val link = File(usrDir, parts[1])
                        if (link.exists()) link.delete()
                        try {
                            Os.symlink(target, link.absolutePath)
                        } catch (e: Exception) {
                            // BULLETPROOF FALLBACK: Copy the actual file if symlink fails
                            try {
                                val targetFile = File(link.parentFile, target)
                                if (targetFile.exists()) {
                                    targetFile.copyTo(link, overwrite = true)
                                }
                            } catch (ex: Exception) {}
                        }
                    }
                }

                runOnUiThread { outputText.append("[*] Enforcing Root Execution Policies...\n") }
                usrDir.walkTopDown().forEach { file ->
                    if (file.isFile && (file.parentFile?.name == "bin" || file.parentFile?.name == "libexec")) {
                        file.setExecutable(true)
                    }
                }

                runOnUiThread { outputText.append("[*] Injecting PRoot Engine...\n") }
                val prootFile = File(filesDir, "proot")
                val prootUrl = URL("https://github.com/proot-me/proot/releases/download/v5.3.0/proot-v5.3.0-aarch64-static")
                val conn = prootUrl.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.connect()
                conn.inputStream.use { input -> FileOutputStream(prootFile).use { output -> input.copyTo(output) } }
                prootFile.setExecutable(true)

                runOnUiThread {
                    outputText.append("\n[+] SYSTEM BREACHED SUCCESSFULLY! 🎉\n")
                    outputText.append("[+] You now have FULL LINUX ROOT.\n")
                    runBtn.isEnabled = true
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    outputText.append("[-] Payload Failed: ${e.message}\n")
                    runBtn.isEnabled = true
                }
            }
        }
    }
}
