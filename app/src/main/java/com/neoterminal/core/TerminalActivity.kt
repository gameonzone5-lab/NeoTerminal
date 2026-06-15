package com.neoterminal.core

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.neoterminal.core.R

class TerminalActivity : AppCompatActivity() {

    private lateinit var terminalOutput: TextView
    private var isCtrlPressed = false
    private var isAltPressed = false

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
        } catch (e: Exception) {
            Log.e("NeoTerminal", "Error initializing terminal: ${e.message}")
            terminalOutput.append("\n[Error: Native Backend Failed]\n")
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
            handleKeyInput("\u001B")
        }
        findViewById<Button>(R.id.btn_tab).setOnClickListener {
            handleKeyInput("\t")
        }
    }

    private fun handleKeyInput(input: String) {
        terminalOutput.append(" [$input] ")
        try {
            writeCommand(input)
        } catch (e: Exception) {
            Log.e("NeoTerminal", "Write failed: ${e.message}")
        }
    }
}
