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
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
val rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }
val scrollView = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f) }
val outputText = TextView(this).apply { setTextColor(Color.GREEN); textSize = 14f; setPadding(16, 16, 16, 16) }
scrollView.addView(outputText)
val inputCommand = EditText(this).apply {
layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

setTextColor(Color.BLACK); setBackgroundColor(Color.LTGRAY); setHintTextColor(Color.GRAY)
hint = "Type 'hack-init' to install tools..."
}
val runBtn = Button(this).apply { text = "RUN" }
rootLayout.addView(scrollView); rootLayout.addView(inputCommand); rootLayout.addView(runBtn)
setContentView(rootLayout)
try {
System.loadLibrary("neoterminal_native")
isNativeLoaded = true
outputText.text = "HACKER TERMINAL READY.\nType 'hack-init' to bypass Android security and install tools.\n"
} catch (e: Throwable) {
outputText.text = "Error: ${e.message}\n"
}
runBtn.setOnClickListener {
val cmd = inputCommand.text.toString()
if (cmd.isNotEmpty()) {
outputText.append("\nroot@android:~# $cmd\n")
if (cmd == "hack-init") {
installHackerTools(outputText, scrollView)
} else if (isNativeLoaded) {
try { outputText.append(executeCommand(cmd)) }
catch (e: Exception) { outputText.append("Error: ${e.message}\n") }
}
inputCommand.text.clear()
scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
}
}
}
private fun installHackerTools(outputText: TextView, scrollView: ScrollView) {
outputText.append("[*] Bypassing security... Downloading BusyBox ARM64 payload...\n")
thread {
try {
val url = URL("https://busybox.net/downloads/binaries/1.31.1-defconfig-multiarch-musl/busybox-armv8l")
val file = File(filesDir, "busybox")
url.openStream().use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
file.setExecutable(true, false) // Make executable

runOnUiThread {
outputText.append("[+] Payload Installed Successfully!\n[+] Type './busybox' to see 300+ Linux commands (nc, wget, awk, vi, etc).\n")
scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
}
} catch (e: Exception) {
runOnUiThread { outputText.append("[-] Hack failed: ${e.message}\n") }
}
}
}
}