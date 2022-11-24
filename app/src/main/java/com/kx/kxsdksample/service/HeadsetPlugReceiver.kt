package com.kx.kxsdksample.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kx.kxsdksample.model.EventMsg
import com.kx.kxsdksample.model.EventType
import org.greenrobot.eventbus.EventBus

/**
 *  耳机插拨事件监听
 */
class HeadsetPlugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_HEADSET_PLUG) {
            val state = intent.getIntExtra("state", 0)
            EventBus.getDefault().postSticky(EventMsg.getInstance(EventType.HeadsetPlugState, state))
        }
    }
}