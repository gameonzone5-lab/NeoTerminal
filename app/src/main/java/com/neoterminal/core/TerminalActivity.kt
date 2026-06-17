package com.neoterminal.core
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
class TerminalActivity : Activity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
// HACKER MODE: No XML inflation. No C++ Native calls. Pure dynamic view injection.
val tv = TextView(this)
tv.text = "SYSTEM HACKED: Bare Metal UI is working!\nXML and C++ bypassed.\nWaiting for further instructions..."
tv.setTextColor(Color.GREEN)
tv.setBackgroundColor(Color.BLACK)
tv.textSize = 20f
tv.setPadding(40, 40, 40, 40)
setContentView(tv)
}
}