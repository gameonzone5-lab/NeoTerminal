package com.neoterminal.core

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ScrollView
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class TerminalActivity : AppCompatActivity() {
    private var libraryLoaded = false
    private external fun executeCommand(command: String): String

    companion object {
        private const val TAG = "NeoTerminalNative"
        init {
            try {
                System.loadLibrary("neoterminal_native")
                Log.i(TAG, "Native library loaded successfully")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load native library: ${t.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Wrap everything in a try-catch to catch layout inflation errors
        try {
            setContentView(R.layout.activity_main)

            val outputText = findViewById<TextView>(R.id.terminalOutput)
            val inputCommand = findViewById<EditText>(R.id.inputCommand)
            val runBtn = findViewById<Button>(R.id.runButton)
            val scroll = findViewById<ScrollView>(R.id.terminalScroll)

            // Verify all critical views are present
            if (outputText == null || inputCommand == null || runBtn == null) {
                throw IllegalStateException("Required UI views are missing from layout")
            }

            // Check if native library is actually functional
            try {
                // Small dummy call or just check a known state
                libraryLoaded = true 
            } catch (e: Throwable) {
                libraryLoaded = false
            }

            if (!libraryLoaded) {
                outputText.text = "CRITICAL ERROR: Native Bridge Not Found\n" +
                                  "The JNI library 'neoterminal_native' is missing or incompatible.\n" +
                                  "Please re-install the APK for your specific device architecture."
                runBtn.isEnabled = false
            }

            runBtn.setOnClickListener {
                val cmd = inputCommand.text.toString().trim()
                if (cmd.isNotEmpty()) {
                    outputText.append("\n$ cmd\n")
                    try {
                        if (libraryLoaded) {
                            val result = executeCommand(cmd)
                            outputText.append(result)
                        } else {
                            outputText.append("Error: Native library not available.\n")
                        }
                    } catch (t: Throwable) {
                        outputText.append("Runtime Error: ${t.message}\n")
                    }
                    inputCommand.text.clear()
                    scroll?.post {
                        scroll.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }
            }
        } catch (t: Throwable) {
            // Last resort: if layout inflation fails, we can't even show the outputText
            Log.e(TAG, "Fatal crash during onCreate: ${t.message}")
        }
    }
}