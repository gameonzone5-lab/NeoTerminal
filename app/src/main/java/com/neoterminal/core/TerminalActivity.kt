package com.neoterminal.core

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TerminalActivity : AppCompatActivity() {
    private external fun executeCommand(command: String): String

    companion object {
        init { System.loadLibrary("neoterminal_native") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val outputText = findViewById<TextView>(R.id.terminalOutput)
        val inputCommand = findViewById<EditText>(R.id.inputCommand)
        val runBtn = findViewById<Button>(R.id.runButton)

        outputText.text = "NeoTerminal Initialized.\nWelcome to Termux Alternative.\n"

        runBtn.setOnClickListener {
            val cmd = inputCommand.text.toString()
            if(cmd.isNotEmpty()){
                outputText.append("\nneoterminal~$ $cmd\n")
                try {
                    val result = executeCommand(cmd)
                    outputText.append(result)
                } catch (e: Exception) {
                    outputText.append("Error: ${e.message}\n")
                }
                inputCommand.text.clear()
            }
        }
    }
}