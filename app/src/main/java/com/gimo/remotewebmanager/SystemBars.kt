package com.gimo.remotewebmanager

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object SystemBars {
    /** 把状态栏/导航栏 insets 追加到 view 现有 padding 上。
     *  targetSdk 35 在 Android 15 上强制边到边，不做此处理内容会画到系统栏底下。 */
    fun apply(root: View) {
        val base = intArrayOf(root.paddingLeft, root.paddingTop, root.paddingRight, root.paddingBottom)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(base[0] + bars.left, base[1] + bars.top, base[2] + bars.right, base[3] + bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}
