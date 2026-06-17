package com.neoterminal.core
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.concurrent.thread
class TerminalActivity : Activity() {
private external fun executeCommand(command: String): String
private var isNativeLoaded = false
private lateinit var outputText: TextView
private lateinit var scrollView: ScrollView
private lateinit var inputCommand: EditText
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
val rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }
scrollView = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f) }
outputText = TextView(this).apply { setTextColor(Color.GREEN); textSize = 14f; setPadding(16, 16, 16, 16) }
scrollView.addView(outputText)
inputCommand = EditText(this).apply {
layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
setTextColor(Color.BLACK); setBackgroundColor(Color.LTGRAY); setHintTextColor(Color.DKGRAY)
hint = "Enter linux command..."
}
val runBtn = Button(this).apply { text = "RUN" }
rootLayout.addView(scrollView); rootLayout.addView(inputCommand); rootLayout.addView(runBtn)
setContentView(rootLayout)
try {
System.loadLibrary("neoterminal_native")
isNativeLoaded = true
outputText.text = "[*] NEO TERMINAL INITIALIZED.\n"
} catch (e: Throwable) {
outputText.text = "[-] Native Error: ${e.message}\n"
}
autoInstallTools()
runBtn.setOnClickListener {
val cmd = inputCommand.text.toString()
if (cmd.isNotEmpty()) {
outputText.append("\nroot@android:~# $cmd\n")
if (isNativeLoaded) {
try {
val busyboxFile = File(filesDir, "busybox")
val finalCmd = if (busyboxFile.exists() && !cmd.startsWith("./busybox")) "./busybox $cmd" else cmd
outputText.append(executeCommand(finalCmd))
} catch (e: Exception) {
outputText.append("[-] Error: ${e.message}\n")
}
}
inputCommand.text.clear()
scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
}
}
}
private fun autoInstallTools() {
val busyboxFile = File(filesDir, "busybox")
if (busyboxFile.exists() && busyboxFile.canExecute()) {
outputText.append("[+] Linux core tools are already installed and ready.\n")
return
}
outputText.append("[*] First boot detected. Downloading Linux core tools. Please wait...\n")
inputCommand.isEnabled = false
thread {
try {
val url = URL("https://busybox.net/downloads/binaries/1.31.0-defconfig-multiarch-musl/busybox-armv8l")
url.openStream().use { input -> FileOutputStream(busyboxFile).use { output -> input.copyTo(output) } }
busyboxFile.setExecutable(true, false)
runOnUiThread {
outputText.append("[+] Download complete! Security bypassed.\n[+] Environment ready. Try commands like 'ls', 'pwd', 'ifconfig'.\n[!] Note: This is a core shell, not full Ubuntu, so 'apt' is not available.\n")
inputCommand.isEnabled = true
scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
}
} catch (e: Exception) {
runOnUiThread {
outputText.append("[-] Download failed. Check internet connection. Error: ${e.message}\n")
inputCommand.isEnabled = true
}
}
}
}
}