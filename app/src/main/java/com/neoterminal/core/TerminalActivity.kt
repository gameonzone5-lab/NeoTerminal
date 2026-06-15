package com.neoterminal.core

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TerminalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val terminalOutput = findViewById<TextView>(R.id.terminalOutput)
        terminalOutput.append(stringFromJNI())
    }
    external fun stringFromJNI(): String
    companion object {
        init { System.loadLibrary("neoterminal_native") }
    }
}
