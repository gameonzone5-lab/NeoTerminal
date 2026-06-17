package com.neoterminal.core
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import java.io.File

class TerminalActivity : Activity() {
    private external fun executeCommand(command: String): String
    private var isNativeLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK) 
        }
        val scrollView = ScrollView(this).apply { 
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f) 
        }
        val outputText = TextView(this).apply { 
            setTextColor(Color.GREEN)
            textSize = 14f
            setPadding(16, 16, 16, 16) 
        }
        scrollView.addView(outputText)

        val inputCommand = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.LTGRAY)
            setHintTextColor(Color.GRAY)
            hint = "Enter native command (e.g., ls /system/bin)..."
        }

        val runBtn = Button(this).apply { text = "RUN" }
        rootLayout.addView(scrollView)
        rootLayout.addView(inputCommand)
        rootLayout.addView(runBtn)
        setContentView(rootLayout)

        // CLEANUP: Delete the broken busybox payload causing Bad System Call
        val busyboxFile = File(filesDir, "busybox")
        if (busyboxFile.exists()) {
            busyboxFile.delete()
        }

        try {
            System.loadLibrary("neoterminal_native")
            isNativeLoaded = true
            outputText.text = "[*] NEO TERMINAL ACTIVE.\n[+] Native Shell Restored. Try 'ls /sdcard' or 'top'.\n"
        } catch (e: Throwable) {
            outputText.text = "[-] Native Error: ${e.message}\n"
        }

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString()
            if (cmd.isNotEmpty()) {
                outputText.append("\nroot@android:~# $cmd\n")
                if (isNativeLoaded) {
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
}