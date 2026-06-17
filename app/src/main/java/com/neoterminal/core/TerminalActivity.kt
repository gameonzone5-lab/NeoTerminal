package com.neoterminal.core
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.io.FileOutputStream
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
            hint = "Enter command..."
        }

        val runBtn = Button(this).apply { text = "RUN" }
        rootLayout.addView(scrollView)
        rootLayout.addView(inputCommand)
        rootLayout.addView(runBtn)
        setContentView(rootLayout)

        try { 
            System.loadLibrary("neoterminal_native")
            isNativeLoaded = true 
        } catch (e: Exception) {
            // Library load failure handled silently or via outputText
        }

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                if (cmd == "bootstrap") {
                    startBootstrap()
                } else if (isNativeLoaded) {
                    outputText.append("\n$ $cmd\n")
                    try { 
                        outputText.append(executeCommand(cmd)) 
                    } catch (e: Exception) { 
                        outputText.append("Error: ${e.message}\n") 
                    }
                } else {
                    outputText.append("Native bridge not loaded.\n")
                }
                inputCommand.text.clear()
                scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }

        outputText.text = "[*] NeoTerm Pro Ready.\nType 'bootstrap' to start Linux environment installation.\n"
    }

    private fun startBootstrap() {
        outputText.append("[*] Initializing Linux environment (PRoot)...\n")
        thread {
            try {
                val prootDir = File(filesDir, "proot_env")
                if (!prootDir.exists()) prootDir.mkdirs()
                
                // Download PRoot binary
                val url = URL("https://proot.gitlab.io/proot/bin/proot-x86_64") 
                val prootFile = File(prootDir, "proot")
                
                url.openStream().use { input -> 
                    FileOutputStream(prootFile).use { output -> 
                        input.copyTo(output) 
                    } 
                }
                prootFile.setExecutable(true, false)

                runOnUiThread { 
                    outputText.append("[+] PRoot binary ready. Environment structure created.\n") 
                }
            } catch (e: Exception) {
                runOnUiThread { 
                    outputText.append("[-] Bootstrap failed: ${e.message}\n") 
                }
            }
        }
    }
}