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
        val scrollView = ScrollView(this).apply { 
            addView(outputText)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) 
        }
        val input = EditText(this).apply { 
            hint = "Command..."
            setTextColor(android.graphics.Color.BLACK)
            setBackgroundColor(android.graphics.Color.LTGRAY)
        }
        val runBtn = Button(this).apply { text = "RUN" }
        
        rootLayout.addView(scrollView)
        rootLayout.addView(input)
        rootLayout.addView(runBtn)
        setContentView(rootLayout)

        runBtn.setOnClickListener {
            val cmd = input.text.toString()
            if (cmd == "bootstrap") {
                downloadScript()
            } else if (cmd.startsWith("sh ubuntu.sh")) {
                // FORCE ABSOLUTE PATH
                val absolutePath = File(filesDir, "ubuntu.sh").absolutePath
                outputText.append("\n$ sh $absolutePath\n")
                try {
                    outputText.append(executeCommand("sh $absolutePath"))
                } catch (e: Exception) {
                    outputText.append("[-] Error: ${e.message}\n")
                }
            } else if (cmd.isNotEmpty()) {
                outputText.append("\n$ $cmd\n")
                try {
                    outputText.append(executeCommand(cmd))
                } catch (e: Exception) {
                    outputText.append("[-] Error: ${e.message}\n")
                }
            }
            input.text.clear()
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
        outputText.text = "NeoTerm Pro Active. \nCommands: 'bootstrap', 'sh ubuntu.sh', 'ls /sdcard'.\n"
    }

    private fun downloadScript() {
        outputText.append("[*] Downloading Ubuntu bootstrap script...\n")
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
                }
            } catch (e: Exception) {
                runOnUiThread { 
                    outputText.append("[-] Download error: ${e.message}\n") 
                }
            }
        }
    }
}