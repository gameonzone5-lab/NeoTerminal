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
        init {
            try {
                System.loadLibrary("neoterminal_native")
            } catch (e: UnsatisfiedLinkError) {
                // Log error or handle missing library
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
                
                // Correctly scroll to bottom
                scroll?.post {
                    scroll.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }
}