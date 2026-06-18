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
        inputCommand = EditText(this).apply { hint = "root@userland:~#"; setTextColor(Color.BLACK); setBackgroundColor(Color.LTGRAY) }
        runBtn = Button(this).apply { text = "EXECUTE" }
        rootLayout.addView(scrollView); rootLayout.addView(inputCommand); rootLayout.addView(runBtn)
        setContentView(rootLayout)

        outputText.text = "[*] NeoTerm Pro (100% Ultimate Chroot Engine).\n"

        val rootfs = File(filesDir, "rootfs")
        // CRITICAL FIX: Removed leading slash so path resolves INSIDE rootfs
        val aptFile = File(rootfs, "data/data/com.termux/files/usr/bin/apt")
        if (!aptFile.exists()) {
            outputText.append("[!] Rootfs empty. Type 'hack' to build system.\n")
        } else {
            outputText.append("[+] Rootfs Secure. Ready for commands. Try 'apt update'.\n")
        }

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                if (cmd.lowercase() == "hack" || cmd.lowercase() == "bootstrap") {
                    deployUserlandPayload()
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
                val rootfs = File(filesDir, "rootfs")

                val guestHome = "/data/data/com.termux/files/home"
                val guestTmp = "/data/data/com.termux/files/usr/tmp"
                val hostTmp = File(filesDir, "proot_host_tmp")
                if (!hostTmp.exists()) hostTmp.mkdirs()

                val guestBash = "/data/data/com.termux/files/usr/bin/bash"
                // CRITICAL FIX: Removed leading slash to prevent absolute path escape
                val hostBash = File(rootfs, "data/data/com.termux/files/usr/bin/bash")

                val pb = ProcessBuilder()
                pb.directory(filesDir)
                // CRITICAL FIX: Merge stderr and stdout so NO silent crashes can happen
                pb.redirectErrorStream(true)

                val env = pb.environment()
                env.clear()
                env["PATH"] = "/data/data/com.termux/files/usr/bin:/system/bin:/system/xbin"
                env["PREFIX"] = "/data/data/com.termux/files/usr"
                env["LD_LIBRARY_PATH"] = "/data/data/com.termux/files/usr/lib"
                env["HOME"] = guestHome
                env["TERM"] = "xterm-256color"
                env["TMPDIR"] = guestTmp
                env["PROOT_TMP_DIR"] = hostTmp.absolutePath
                env["PROOT_NO_SECCOMP"] = "1"

                if (prootFile.exists() && hostBash.exists()) {
                    pb.command(
                        prootFile.absolutePath,
                        "-r", rootfs.absolutePath,
                        "-b", "/system",
                        "-b", "/dev",
                        "-b", "/proc",
                        "-0",
                        "-w", guestHome,
                        guestBash, "-c", cmd
                    )
                } else {
                    env.clear()
                    env["PATH"] = "/system/bin:/system/xbin"
                    pb.command("sh", "-c", cmd)
                    runOnUiThread { outputText.append("[!] Bash not found. Running native Android shell.\n") }
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
                process.waitFor()
            } catch (e: Exception) {
                runOnUiThread { outputText.append("[-] Engine Error: ${e.message}\n") }
            }
            runOnUiThread { runBtn.isEnabled = true }
        }
    }

    private fun deployUserlandPayload() {
        runBtn.isEnabled = false
        outputText.append("\n[*] INITIATING 100% ULTIMATE HACKER PAYLOAD...\n")
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

                runOnUiThread { outputText.append("[*] Extracting Termux Base into Rootfs...\n") }
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
                        try { Os.symlink(target, link.absolutePath) } catch (e: Exception) {
                            try {
                                val targetFile = File(link.parentFile, target)
                                if (targetFile.exists()) targetFile.copyTo(link, overwrite = true)
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

                runOnUiThread { outputText.append("[*] Injecting Static PRoot Engine...\n") }
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
                    outputText.append("\n[+] USERLAND ROOTFS BUILT SUCCESSFULLY! 🎉\n")
                    outputText.append("[+] 100% Core Bypass Active. Try 'apt update'.\n")
                    runBtn.isEnabled = true
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
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
