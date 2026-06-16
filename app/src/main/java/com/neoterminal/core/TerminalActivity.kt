package com.neoterminal.core
import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
class TerminalActivity : Activity() {
private var isNativeLoaded = false
private external fun executeCommand(command: String): String
override fun onCreate(savedInstanceState: Bundle?) {

super.onCreate(savedInstanceState)
setContentView(R.layout.activity_main)
val outputText = findViewById<TextView>(R.id.terminalOutput)
val inputCommand = findViewById<EditText>(R.id.inputCommand)
val runBtn = findViewById<Button>(R.id.runButton)
try {
System.loadLibrary("neoterminal_native")
isNativeLoaded = true
outputText.text = "NeoTerminal Started.\nSystem Ready.\n"
} catch (e: Throwable) {
outputText.text = "Error Loading C++ Library: ${e.message}\n"
}
runBtn.setOnClickListener {
val cmd = inputCommand.text.toString()
if (cmd.isNotEmpty()) {
outputText.append("\n$ $cmd\n")
if (isNativeLoaded) {
try {
outputText.append(executeCommand(cmd))
} catch (e: Exception) {
outputText.append("Error: ${e.message}\n")
}
}
inputCommand.text.clear()
}
}
}
}