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

        outputText.text = "[*] NeoTerm Pro (APEX-BYPASS BIND ENGINE).\n"
        checkSystemStatus()

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                if (cmd.lowercase() == "hack" || cmd.lowercase() == "bootstrap") {
                    deployNuclearPayload()
                } else {
                    executeCommand(cmd)
                }
            }
            inputCommand.text.clear()
        }
    }

    private fun checkSystemStatus() {
        val rootfs = File(filesDir, "rootfs")
        val termuxBase = File(rootfs, "data/data/com.termux/files")
        val bashFile = File(termuxBase, "usr/bin/bash")
        val aptFile = File(termuxBase, "usr/bin/apt")
        val prootFile = File(filesDir, "proot")

        runOnUiThread {
            if (!bashFile.exists() || !prootFile.exists() || bashFile.length() == 0L) {
                outputText.append("[!] Core incomplete. Type 'hack' to run safe-deploy.\n")
            } else {
                outputText.append("[+] APEX Core Validated! Path Translation Active. Try 'apt update'.\n")
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
                val hostTmp = File(filesDir, "proot_host_tmp")
                if (!hostTmp.exists()) hostTmp.mkdirs()

                // Real local paths
                val rootfs = File(filesDir, "rootfs")
                val termuxBase = File(rootfs, "data/data/com.termux/files")

                // Virtual target paths
                val guestPrefix = "/data/data/com.termux/files/usr"
                val guestHome = "/data/data/com.termux/files/home"
                val guestTmp = "$guestPrefix/tmp"
                val guestBash = "$guestPrefix/bin/bash"

                val pb = ProcessBuilder()
                pb.directory(filesDir)
                pb.redirectErrorStream(true)

                val env = pb.environment()
                env.clear()
                env["PROOT_TMP_DIR"] = hostTmp.absolutePath
                env["PROOT_NO_SECCOMP"] = "1"

                // We run Android's native sh, export vars, and exec Termux bash
                // Using double quotes around cmd to handle arguments safely
                val secureCmd = "export PATH=$guestPrefix/bin:/system/bin:/system/xbin; export LD_LIBRARY_PATH=$guestPrefix/lib; export PREFIX=$guestPrefix; export TMPDIR=$guestTmp; export HOME=$guestHome; exec $guestBash -c \"$cmd\""

                if (prootFile.exists() && termuxBase.exists()) {
                    // THE ULTIMATE FIX: NO `-r rootfs`. Just bind-mount the specific folder!
                    // This leaves Android's /apex and /system perfectly intact, preventing Exit 255.
                    pb.command(
                        prootFile.absolutePath,
                        "--link2symlink",
                        "-0",
                        "-b", "${termuxBase.absolutePath}:/data/data/com.termux/files",
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

    private fun deployNuclearPayload() {
        runBtn.isEnabled = false
        outputText.append("\n[*] INITIATING SAFE-DEPLOY (Local ZIP Caching)...\n")
        thread {
            try {
                val rootfs = File(filesDir, "rootfs")
                if (rootfs.exists()) rootfs.deleteRecursively()
                rootfs.mkdirs()

                val termuxBase = File(rootfs, "data/data/com.termux/files")
                val usrDir = File(termuxBase, "usr")
                val homeDir = File(termuxBase, "home")
                usrDir.mkdirs()
                homeDir.mkdirs()
                File(usrDir, "tmp").mkdirs()

                runOnUiThread { outputText.append("[*] Downloading Bootstrap to Local Cache...\n") }
                val zipFile = File(filesDir, "bootstrap.zip")
                val url = URL("https://github.com/termux/termux-packages/releases/latest/download/bootstrap-aarch64.zip")
                val conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.connect()

                conn.inputStream.use { input -> FileOutputStream(zipFile).use { output -> input.copyTo(output) } }

                if (zipFile.length() < 1000000) {
                    throw Exception("ZIP download corrupted or empty! Size: ${zipFile.length()} bytes")
                }
                runOnUiThread { outputText.append("[+] Cache verified. Extracting...\n") }

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

                runOnUiThread { outputText.append("[*] Constructing Hardlink Architecture...\n") }
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

                runOnUiThread { outputText.append("[*] Setting Execution Permissions globally...\n") }
                usrDir.walkTopDown().forEach { file ->
                    if (file.isFile) file.setExecutable(true)
                }

                runOnUiThread { outputText.append("[*] Injecting Static PRoot Engine...\n") }
                val prootFile = File(filesDir, "proot")
                val prootUrl = URL("https://github.com/proot-me/proot/releases/download/v5.3.0/proot-v5.3.0-aarch64-static")
                val pConn = prootUrl.openConnection() as HttpURLConnection
                pConn.instanceFollowRedirects = true
                pConn.connect()
                pConn.inputStream.use { input -> FileOutputStream(prootFile).use { output -> input.copyTo(output) } }
                prootFile.setExecutable(true)

                runOnUiThread {
                    outputText.append("\n[+] NUCLEAR SYSTEM INITIALIZED! 🎉\n")
                    checkSystemStatus()
                    runBtn.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    outputText.append("[-] Build Failed: ${e.message}\n")
                    runBtn.isEnabled = true
                }
            }
        }
    }
}
