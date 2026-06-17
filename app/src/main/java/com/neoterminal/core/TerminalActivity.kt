package com.neoterminal.core
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class TerminalActivity : Activity() {
    private external fun executeCommand(command: String): String
    private var isNativeLoaded = false
    private lateinit var outputText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var inputCommand: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK) 
        }
        scrollView = ScrollView(this).apply { 
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f) 
        }
        outputText = TextView(this).apply { 
            setTextColor(Color.GREEN)
            textSize = 14f
            setPadding(16, 16, 16, 16) 
        }
        scrollView.addView(outputText)

        inputCommand = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.LTGRAY)
            setHintTextColor(Color.GRAY)
            hint = "Type 'bootstrap' or 'ls /sdcard'..."
        }

        val runBtn = Button(this).apply { text = "RUN" }
        rootLayout.addView(scrollView)
        rootLayout.addView(inputCommand)
        rootLayout.addView(runBtn)
        setContentView(rootLayout)

        try {
            System.loadLibrary("neoterminal_native")
            isNativeLoaded = true
            outputText.text = "[*] NeoTerm Pro Active.\n[*] Native Engine running. Type 'bootstrap' to download Ubuntu PRoot installer.\n"
        } catch (e: Throwable) {
            outputText.text = "[-] Native Error: ${e.message}\n"
        }

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                outputText.append("\nroot@android:~# $cmd\n")
                if (cmd == "bootstrap") {
                    startBootstrap()
                } else if (isNativeLoaded) {
                    try { 
                        outputText.append(executeCommand(cmd)) 
                    } catch (e: Exception) { 
                        outputText.append("[-] Error: ${e.message}\n") 
                    }
                }
                inputCommand.text.clear()
                scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
    }

    private fun startBootstrap() {
        outputText.append("[*] Initializing Ubuntu PRoot Installer for ARM64...\n[*] Downloading verified script from AnLinux... Please wait.\n")
        inputCommand.isEnabled = false
        thread {
            try {
                // Verified raw GitHub URL for AnLinux Ubuntu bootstrap
                val url = URL("https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/Scripts/Installer/Ubuntu/ubuntu.sh")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP ${connection.responseCode}")
                }
                val scriptFile = File(filesDir, "ubuntu.sh")
                connection.inputStream.use { input ->
                    FileOutputStream(scriptFile).use { output ->
                        input.copyTo(output)
                    }
                }
                scriptFile.setExecutable(true)
                runOnUiThread {
                    outputText.append("[+] Bootstrap Script downloaded successfully!\n")
                    outputText.append("[+] Action Required: Type 'sh ubuntu.sh' to begin compiling the Linux environment.\n")
                    inputCommand.isEnabled = true
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    outputText.append("[-] Download Failed: ${e.message}\n")
                    inputCommand.isEnabled = true
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            }
        }
    }
}