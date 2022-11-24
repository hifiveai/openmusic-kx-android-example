package com.kx.kxsdksample

import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity


/**
 * 普通Activity基类
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            com.kx.kxsdksample.tools.Utils.log("onKeyDown")
            onBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    abstract fun onBack()
}
