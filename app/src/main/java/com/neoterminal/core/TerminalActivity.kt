package com.neoterminal.core

import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
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
        inputCommand = EditText(this).apply { hint = "root@ubuntu:~#"; setTextColor(Color.BLACK); setBackgroundColor(Color.LTGRAY) }
        runBtn = Button(this).apply { text = "EXECUTE" }
        rootLayout.addView(scrollView); rootLayout.addView(inputCommand); rootLayout.addView(runBtn)
        setContentView(rootLayout)

        outputText.text = "[*] NeoTerm Pro (PURE USERLAND ARCHITECTURE).\n"
        checkSystemStatus()

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

    private fun checkSystemStatus() {
        val rootfs = File(filesDir, "ubuntu-fs")
        val bashFile = File(rootfs, "bin/bash")
        val prootFile = File(filesDir, "proot")

        runOnUiThread {
            if (!bashFile.exists() || !prootFile.exists()) {
                outputText.append("[!] Ubuntu RootFS missing. Type 'bootstrap' to install correctly.\n")
            } else {
                outputText.append("[+] UserLAnd Ubuntu Core Active! Try 'apt update'.\n")
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
                val rootfs = File(filesDir, "ubuntu-fs")

                val pb = ProcessBuilder()
                pb.directory(filesDir)
                pb.redirectErrorStream(true)

                if (prootFile.exists() && File(rootfs, "bin/bash").exists()) {
                    // EXACT UserLAnd Execution Command (Pure Linux standard paths)
                    pb.command(
                        prootFile.absolutePath,
                        "--link2symlink",
                        "-0",
                        "-r", rootfs.absolutePath,
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sys",
                        "-w", "/root",
                        "/usr/bin/env", "-i",
                        "HOME=/root",
                        "PATH=/usr/local/sbin:/usr/local/bin:/bin:/usr/bin:/sbin:/usr/sbin:/usr/games:/usr/local/games",
                        "TERM=xterm-256color",
                        "LANG=C.UTF-8",
                        "/bin/bash", "-c", cmd
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
        conn.inputStream.use { input -> FileOutputStream(dest).use { output -> input.copyTo(output) } }
    }

    private fun deployUserlandPayload() {
        runBtn.isEnabled = false
        outputText.append("\n[*] INITIATING PURE USERLAND ARCHITECTURE...\n")
        thread {
            try {
                val rootfs = File(filesDir, "ubuntu-fs")
                if (rootfs.exists()) rootfs.deleteRecursively()
                rootfs.mkdirs()

                runOnUiThread { outputText.append("[*] Downloading Core Engines (PRoot & BusyBox)...\n") }
                val prootFile = File(filesDir, "proot")
                val busyboxFile = File(filesDir, "busybox")
                downloadFile("https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/tar/proot/proot-aarch64", prootFile)
                downloadFile("https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/tar/busybox/busybox-aarch64", busyboxFile)
                prootFile.setExecutable(true)
                busyboxFile.setExecutable(true)

                runOnUiThread { outputText.append("[*] Downloading Official Ubuntu RootFS (Please wait, ~30MB)...\n") }
                val tarFile = File(filesDir, "ubuntu.tar.xz")
                downloadFile("https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/Rootfs/Ubuntu/arm64/ubuntu-rootfs-arm64.tar.xz", tarFile)

                runOnUiThread { outputText.append("[*] Extracting RootFS Natively (UserLAnd Method)...\n") }
                // EXACT UserLAnd Native Extraction: Uses PRoot and Busybox to extract tarball so symlinks are created natively inside the chroot
                val extractCmd = arrayOf(
                    prootFile.absolutePath,
                    "--link2symlink",
                    "-0",
                    busyboxFile.absolutePath,
                    "tar",
                    "-xJf",
                    tarFile.absolutePath,
                    "-C",
                    rootfs.absolutePath
                )

                val process = Runtime.getRuntime().exec(extractCmd, null, filesDir)
                val reader = process.errorStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    runOnUiThread { outputText.append("  $line\n") }
                }
                process.waitFor()

                tarFile.delete() // Cleanup

                runOnUiThread {
                    outputText.append("\n[+] PURE UBUNTU ROOTFS INSTALLED SUCCESSFULLY! 🎉\n")
                    checkSystemStatus()
                    runBtn.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    outputText.append("[-] Setup Failed: ${e.message}\n")
                    runBtn.isEnabled = true
                }
            }
        }
    }
}
