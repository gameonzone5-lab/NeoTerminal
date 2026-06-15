package com.neoterminal.core

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class TerminalActivity : AppCompatActivity() {

    private lateinit var terminalOutput: TextView
    private var isCtrlPressed = false
    private var isAltPressed = false

    // JNI Declarations for NDK Backend
    private external fun startPty(): Int
    private external fun writeCommand(cmd: String)
    private external fun readOutput(): String

    companion object {
        init {
            System.loadLibrary("neoterminal_native")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        terminalOutput = findViewById(R.id.terminal_output)
        
        setupExtraKeys()
        initTerminal()
    }

    private fun initTerminal() {
        try {
            val ptyFd = startPty()
            terminalOutput.append("\n[Native PTY Started with FD: $ptyFd]\n")
            terminalOutput.append("neo@android:~$ ")
        } catch (e: UnsatisfelyLinkError) {
            Log.e("NeoTerminal", "Native library not loaded: ${e.message}")
            terminalOutput.append("\n[Error: Native Library not loaded]\n")
        }
    }

    private fun setupExtraKeys() {
        findViewById<Button>(R.id.btn_ctrl).setOnClickListener {
            isCtrlPressed = !isCtrlPressed
            handleKeyInput("CTRL")
        }
        findViewById<Button>(R.id.btn_alt).setOnClickListener {
            isAltPressed = !isAltPressed
            handleKeyInput("ALT")
        }
        findViewById<Button>(R.id.btn_esc).setOnClickListener {
            handleKeyInput("") // ESC character
        }
        findViewById<Button>(R.id.btn_tab).setOnClickListener {
            handleKeyInput("	") // TAB character
        }
    }

    private fun handleKeyInput(input: String) {
        terminalOutput.append(" [$input] ")
        
        // In a real scenario, we combine modifiers with the actual key
        // For now, we send the raw input to the native layer
        try {
            writeCommand(input)
        } catch (e: Exception) {
            Log.e("NeoTerminal", "Failed to write command: ${e.message}")
        }
    }
}
