package com.neoterminal.core

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class TerminalActivity : AppCompatActivity() {
    private external fun executeCommand(command: String): String

    companion object {
        var libraryLoaded = true
        init {
            try {
                System.loadLibrary("neoterminal_native")
            } catch (t: Throwable) {
                libraryLoaded = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val outputText = findViewById<TextView>(R.id.terminalOutput)
        val inputCommand = findViewById<EditText>(R.id.inputCommand)
        val runBtn = findViewById<Button>(R.id.runButton)
        val scroll = findViewById<ScrollView>(R.id.terminalScroll)

        // Immediate crash-proof check: if library failed to load, notify user in UI
        if (!libraryLoaded) {
            outputText.text = "FATAL ERROR: Native library 'neoterminal_native' could not be loaded.\n" +
                              "This app cannot function without its JNI backend.\n" +
                              "Please ensure the APK is compiled for your device architecture."
            runBtn.isEnabled = false
        }

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                outputText.append("\n$ cmd\n")
                try {
                    // Double check library status before calling external method
                    if (libraryLoaded) {
                        val result = executeCommand(cmd)
                        outputText.append(result)
                    } else {
                        outputText.append("Error: Native bridge unavailable.\n")
                    }
                } catch (t: Throwable) {
                    outputText.append("Execution Error: ${t.message}\n")
                }
                inputCommand.text.clear()
                
                scroll?.post {
                    scroll.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }
}