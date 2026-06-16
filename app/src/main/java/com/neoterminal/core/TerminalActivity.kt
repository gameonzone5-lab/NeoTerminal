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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)

            val outputText = findViewById<TextView>(R.id.terminalOutput)
            val inputCommand = findViewById<EditText>(R.id.inputCommand)
            val runBtn = findViewById<Button>(R.id.runButton)
            val scroll = findViewById<ScrollView>(R.id.terminalScroll)

            // LOAD LIBRARY INSIDE ONCREATE (No Companion Object)
            try {
                System.loadLibrary("neoterminal_native")
                libraryLoaded = true
                Log.i("NeoTerminal", "Native library loaded successfully in onCreate")
            } catch (t: Throwable) {
                libraryLoaded = false
                Log.e("NeoTerminal", "Failed to load native library: ${t.message}")
                outputText?.text = "FATAL ERROR: Native library 'neoterminal_native' not found.\n" +
                                   "The app will not crash, but commands will not work."
            }

            if (outputText == null || inputCommand == null || runBtn == null) {
                throw IllegalStateException("Required UI views are missing")
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
                            outputText.append("Error: Native bridge unavailable.\n")
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
            Log.e("NeoTerminal", "Fatal crash during onCreate: ${t.message}")
        }
    }
}