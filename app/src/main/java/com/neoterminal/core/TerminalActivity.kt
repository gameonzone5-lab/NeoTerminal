package com.neoterminal.core

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TerminalActivity : AppCompatActivity() {
    private external fun executeCommand(command: String): String

    companion object {
        init {
            System.loadLibrary("neoterminal_native")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val outputText = findViewById<TextView>(R.id.terminalOutput)
        val inputCommand = findViewById<EditText>(R.id.inputCommand)
        val runBtn = findViewById<Button>(R.id.runButton)

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString().trim()
            if (cmd.isNotEmpty()) {
                outputText.append("\n$ cmd\n")
                try {
                    val result = executeCommand(cmd)
                    outputText.append(result)
                } catch (e: Exception) {
                    outputText.append("Runtime Error: ${e.message}\n")
                }
                inputCommand.text.clear()
                // Auto-scroll to bottom
                outputText.post {
                    val scroll = findViewById<android.widget.ScrollView>(R.id.terminalOutput).parent as? android.widget.ScrollView
                    // In the XML I put the TextView inside the ScrollView. 
                    // The ScrollView doesn't have an ID in the XML provided, let me fix that in the logic.
                }
            }
        }
    }
}