package com.neoterminal.core
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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
            hint = "Enter command (e.g. ls /sdcard)..."
        }

        val runBtn = Button(this).apply { text = "RUN" }
        rootLayout.addView(scrollView)
        rootLayout.addView(inputCommand)
        rootLayout.addView(runBtn)
        setContentView(rootLayout)

        // Request Storage Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
        }

        try {
            System.loadLibrary("neoterminal_native")
            isNativeLoaded = true
            outputText.text = "[*] NEO TERMINAL ACTIVE.\n[+] Toybox Wrapper Enabled.\n"
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