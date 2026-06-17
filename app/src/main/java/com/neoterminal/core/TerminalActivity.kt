package com.neoterminal.core
import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.concurrent.thread

class TerminalActivity : Activity() {
    private external fun executeCommand(command: String): String
    private lateinit var outputText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var inputCommand: EditText
    private lateinit var runBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLACK) 
        }
        outputText = TextView(this).apply { 
            setTextColor(android.graphics.Color.GREEN)
            textSize = 14f
            setPadding(16, 16, 16, 16) 
        }
        scrollView = ScrollView(this).apply { 
            addView(outputText)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) 
        }
        inputCommand = EditText(this).apply { 
            hint = "Command..."
            setTextColor(android.graphics.Color.BLACK)
            setBackgroundColor(android.graphics.Color.LTGRAY) 
        }
        runBtn = Button(this).apply { text = "RUN" }
        
        rootLayout.addView(scrollView)
        rootLayout.addView(inputCommand)
        rootLayout.addView(runBtn)
        setContentView(rootLayout)

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString()
            if (cmd == "bootstrap") {
                downloadScript()
            } else {
                runShellCommand(cmd)
            }
            inputCommand.text.clear()
        }
        outputText.text = "NeoTerm Pro Active. \nCommands: 'bootstrap', 'sh ubuntu.sh', 'ls /sdcard'.\n"
    }

    private fun runShellCommand(cmd: String) {
        val finalCmd = if (cmd.startsWith("sh ubuntu.sh")) {
            "sh " + File(filesDir, "ubuntu.sh").absolutePath
        } else {
            cmd
        }
        outputText.append("\n$ $finalCmd\n[*] Executing in background... Please wait.\n")
        runBtn.isEnabled = false 

        thread {
            try {
                val result = executeCommand(finalCmd)
                runOnUiThread {
                    outputText.append(result)
                    runBtn.isEnabled = true
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    outputText.append("[-] Execution Error: ${e.message}\n")
                    runBtn.isEnabled = true
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            }
        }
    }

    private fun downloadScript() {
        runBtn.isEnabled = false
        thread {
            try {
                val scriptFile = File(filesDir, "ubuntu.sh")
                val url = URL("https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/Scripts/Installer/Ubuntu/ubuntu.sh")
                url.openStream().use { input ->
                    FileOutputStream(scriptFile).use { output ->
                        input.copyTo(output)
                    }
                }
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
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            }
        }
    }
}