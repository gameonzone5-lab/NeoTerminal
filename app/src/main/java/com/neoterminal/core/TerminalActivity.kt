package com.neoterminal.core
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*

class TerminalActivity : Activity() {
    private external fun executeCommand(command: String): String
    private var isNativeLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Build UI Programmatically (Bypassing XML entirely)
        val rootLayout = LinearLayout(this)
        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.setBackgroundColor(Color.BLACK)

        val scrollView = ScrollView(this)
        val scrollParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        scrollView.layoutParams = scrollParams

        val outputText = TextView(this)
        outputText.setTextColor(Color.GREEN)
        outputText.textSize = 14f
        outputText.setPadding(16, 16, 16, 16)
        scrollView.addView(outputText)

        val inputLayout = LinearLayout(this)
        inputLayout.orientation = LinearLayout.HORIZONTAL
        inputLayout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val inputCommand = EditText(this)
        inputCommand.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        inputCommand.setTextColor(Color.WHITE)
        inputCommand.setHintTextColor(Color.DKGRAY)
        inputCommand.hint = "Enter command..."

        val runBtn = Button(this)
        runBtn.text = "RUN"
        inputLayout.addView(inputCommand)
        inputLayout.addView(runBtn)
        rootLayout.addView(scrollView)
        rootLayout.addView(inputLayout)
        setContentView(rootLayout)

        // 2. Safely Load Native C++ Library
        try {
            System.loadLibrary("neoterminal_native")
            isNativeLoaded = true
            outputText.text = "Programmatic Terminal UI Active.\nNative C++ Library Loaded Successfully.\n"
        } catch (e: Throwable) {
            outputText.text = "Programmatic UI Active.\nFATAL ERROR: C++ Library failed to load: ${e.message}\n"
        }

        // 3. Handle Command Execution
        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString()
            if (cmd.isNotEmpty()) {
                outputText.append("\n$ $cmd\n")
                if (isNativeLoaded) {
                    try {
                        val result = executeCommand(cmd)
                        outputText.append(result)
                    } catch (e: Exception) {
                        outputText.append("Execution Error: ${e.message}\n")
                    }
                } else {
                    outputText.append("Cannot execute. Native library missing.\n")
                }
                inputCommand.text.clear()
                scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
    }
}