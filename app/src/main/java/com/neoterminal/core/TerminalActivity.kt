package com.neoterminal.core

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.neoterminal.core.R

class TerminalActivity : AppCompatActivity() {

    private lateinit var terminalOutput: TextView
    
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
            val fd = startPty()
            if (fd != -1) {
                terminalOutput.append("\n[System] PTY initialized (FD: $fd)\n")
                terminalOutput.append("neo@android:~$ ")
            } else {
                terminalOutput.append("\n[Error] Failed to open PTY\n")
            }
        } catch (e: Exception) {
            Log.e("NeoTerminal", "Init failed: ${e.message}")
            terminalOutput.append("\n[Critical] Native load error\n")
        }
    }

    private fun setupExtraKeys() {
        findViewById<Button>(R.id.btn_ctrl).setOnClickListener { handleKey("\u0003") } // Ctrl+C
        findViewById<Button>(R.id.btn_alt).setOnClickListener { handleKey("\u001B") } // Alt/Esc
        findViewById<Button>(R.id.btn_esc).setOnClickListener { handleKey("\u001B") }
        findViewById<Button>(R.id.btn_tab).setOnClickListener { handleKey("\t") }
    }

    private fun handleKey(key: String) {
        terminalOutput.append(" [$key] ")
        try {
            writeCommand(key)
        } catch (e: Exception) {
            Log.e("NeoTerminal", "Write failed")
        }
    }
}
