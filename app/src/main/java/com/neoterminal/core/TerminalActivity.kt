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

        outputText.text = "[*] NeoTerm Pro (W^X BYPASS ENGINE).\n"
        checkSystemStatus()

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                if (cmd.lowercase() == "hack" || cmd.lowercase() == "bootstrap") {
                    deployUltimatePayload()
                } else {
                    executeCommand(cmd)
                }
            }
            inputCommand.text.clear()
        }
    }

    private fun checkSystemStatus() {
        val prootFile = File(filesDir, "proot")
        val bashFile = File(filesDir, "usr/bin/bash")

        runOnUiThread {
            outputText.append("\n[DEBUG] Core Status:\n")
            outputText.append(" -> proot: ${prootFile.exists()}\n")
            outputText.append(" -> bash: ${bashFile.exists()} (${bashFile.length()} bytes)\n")

            if (!bashFile.exists() || !prootFile.exists() || bashFile.length() == 0L) {
                outputText.append("[!] Engine missing. Type 'hack' to deploy.\n")
            } else {
                outputText.append("[+] Core Ready. Try 'apt update'.\n")
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
                val usrDir = File(filesDir, "usr")

                val guestPrefix = "/data/data/com.termux/files/usr"
                val guestHome = "/data/data/com.termux/files/home"
                val guestTmp = "$guestPrefix/tmp"

                val pb = ProcessBuilder()
                pb.directory(filesDir)
                pb.redirectErrorStream(true)

                val env = pb.environment()
                env.clear()
                env["PROOT_TMP_DIR"] = filesDir.absolutePath
                env["PROOT_NO_SECCOMP"] = "1"

                val secureCmd = "export PATH=$guestPrefix/bin:/system/bin:/system/xbin; export LD_LIBRARY_PATH=$guestPrefix/lib; export PREFIX=$guestPrefix; export TMPDIR=$guestTmp; export HOME=$guestHome; $cmd"

                if (prootFile.exists() && File(usrDir, "bin/bash").exists()) {
                    pb.command(
                        prootFile.absolutePath,
                        "--link2symlink",
                        "-0",
                        "-b", "${usrDir.absolutePath}:$guestPrefix",
                        "-w", guestHome,
                        "/system/bin/sh", "-c", secureCmd
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
                    if (exitCode != 0) outputText.append("[DEBUG] Exit Code: $exitCode\n")
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
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        conn.connect()

        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Server returned ${conn.responseCode} for $urlStr")
        }

        conn.inputStream.use { input -> FileOutputStream(dest).use { output -> input.copyTo(output) } }
    }

    private fun deployUltimatePayload() {
        runBtn.isEnabled = false
        outputText.append("\n[*] INITIATING ULTIMATE W^X BYPASS PAYLOAD...\n")
        thread {
            try {
                val usrDir = File(filesDir, "usr")
                if (usrDir.exists()) usrDir.deleteRecursively()
                usrDir.mkdirs()

                runOnUiThread { outputText.append("[*] Downloading Official Bootstrap...\n") }
                val zipFile = File(filesDir, "bootstrap.zip")
                downloadFile("https://github.com/termux/termux-packages/releases/latest/download/bootstrap-aarch64.zip", zipFile)

                if (zipFile.length() < 1000000) throw Exception("ZIP corrupted! Size: ${zipFile.length()}")
                runOnUiThread { outputText.append("[+] Bootstrap Cached. Extracting...\n") }

                val zipStream = ZipInputStream(zipFile.inputStream())
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
                zipFile.delete()

                runOnUiThread { outputText.append("[*] Restoring Linux Symlinks...\n") }
                symlinks.forEach { line ->
                    val parts = line.split("←")
                    if (parts.size == 2) {
                        val target = parts[0]
                        val link = File(usrDir, parts[1])
                        if (link.exists()) link.delete()
                        try { Os.symlink(target, link.absolutePath) } catch (e: Exception) {
                            try {
                                val targetFile = File(link.parentFile, target)
                                if (targetFile.exists()) targetFile.copyTo(link, overwrite = true)
                            } catch (ex: Exception) {}
                        }
                    }
                }

                runOnUiThread { outputText.append("[*] Setting Permissions...\n") }
                usrDir.walkTopDown().forEach { file ->
                    if (file.isFile) file.setExecutable(true)
                }

                runOnUiThread { outputText.append("[*] Injecting Official PRoot...\n") }
                val prootFile = File(filesDir, "proot")
                downloadFile("https://github.com/proot-me/proot/releases/download/v5.3.0/proot-v5.3.0-aarch64-static", prootFile)
                prootFile.setExecutable(true)

                runOnUiThread {
                    outputText.append("\n[+] ENGINE READY! W^X Bypass Active. 🎉\n")
                    checkSystemStatus()
                    runBtn.isEnabled = true
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
