package com.kx.kxsdksample.ui.views


import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.DialogKtvTuningBinding
import com.kx.kxsdksample.model.*
import com.kx.kxsdksample.tools.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class KtvTuningDialog(
    context: Context,
    private val accVolume: TuningItem,
    private val keyValue: TuningItem,
    private val recordVolume: TuningItem,
    private val reverbValue: TuningItem,
    private val listener: TuningListener
) : Dialog(context, R.style.CustomProgressDialog), SeekBar.OnSeekBarChangeListener {

    private val binding: DialogKtvTuningBinding =
        DialogKtvTuningBinding.inflate(LayoutInflater.from(context))
    private var accSeekBar: SeekBar
    private var accTv: TextView
    private var keySeekBar: SeekBar
    private var keyTv: TextView
    private var recordSeekBar: SeekBar
    private var recordTv: TextView
    private var reverbSeekBar: SeekBar
    private var reverbTv: TextView
    private var reverbLayout: LinearLayout
    private var reverbTipsTv: TextView

    init {
        setContentView(binding.root)
        //伴奏
        accSeekBar = binding.tuningAccSeekBar
        accSeekBar.setOnSeekBarChangeListener(this)
        accSeekBar.progress = accVolume.progress
        accTv = binding.tuningAccSeekTv
        accTv.text = "${accVolume.progress}%"
        //音调
        keySeekBar = binding.tuningKeySeekBar
        keySeekBar.setOnSeekBarChangeListener(this)
        keySeekBar.progress = keyValue.progress
        keyTv = binding.tuningKeyTv
        keyTv.text = "${keyValue.value.toInt()}"
        //人声
        recordSeekBar = binding.tuningRecordSeekBar
        recordSeekBar.setOnSeekBarChangeListener(this)
        recordSeekBar.progress = recordVolume.progress
        recordTv = binding.tuningRecordSeekTv
        recordTv.text = "${recordVolume.progress}%"
        //美声
        reverbLayout = binding.tuningReverbValueLayout
        reverbTipsTv = binding.tuningReverbTips
        reverbSeekBar = binding.tuningReverbSeekBar
        reverbSeekBar.setOnSeekBarChangeListener(this)
        reverbSeekBar.progress = reverbValue.progress
        reverbTv = binding.tuningReverbSeekTv
        reverbTv.text = "${reverbValue.progress}%"
        updateHeadsetState(Utils.headsetPlugin(context))
        setCancelable(true)
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGetEventBus(msg: EventMsg) {
        if (msg.type == EventType.HeadsetPlugState && msg.value >= 0) {
            updateHeadsetState(msg.value == 1)
            EventBus.getDefault().removeStickyEvent(msg)
        }
    }

    private fun updateHeadsetState(isPlugin: Boolean) {
        if (isPlugin) {
            reverbLayout.visibility = View.VISIBLE
            reverbTipsTv.visibility = View.GONE
        } else {
            reverbLayout.visibility = View.GONE
            reverbTipsTv.visibility = View.VISIBLE
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        Utils.log("seekBar:$seekBar, p:${progress}, f:${fromUser}")
        if (fromUser && seekBar != null) {
            Utils.log("seekBar progress:${seekBar.progress}")
            doProgressChanged(seekBar)
        }
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

    private fun doProgressChanged(seekBar: SeekBar) {
        Utils.log("doProgressChanged seekBar progress:${seekBar.progress}")
        when (seekBar) {
            accSeekBar -> {
                accTv.text = "${seekBar.progress}%"
                listener.onTuned(TuningType.ACC_VOLUME, accVolume.tuningValue(seekBar.progress))
            }
            recordSeekBar -> {
                recordTv.text = "${seekBar.progress}%"
                listener.onTuned(
                    TuningType.RECORD_VOLUME,
                    recordVolume.tuningValue(seekBar.progress)
                )
            }
            reverbSeekBar -> {
                reverbTv.text = "${seekBar.progress}%"
                listener.onTuned(TuningType.REVERB_VALUE, reverbValue.tuningValue(seekBar.progress))
            }
            keySeekBar -> {
                val temp: Int = keyValue.tuningValue(seekBar.progress).toInt()
                keyValue.value = temp.toFloat()
                keyTv.text = "$temp"
                keySeekBar.progress = keyValue.progress
                listener.onTuned(TuningType.KEY_VALUE, keyValue.value)
            }
        }
    }
}

interface TuningListener {
    public fun onTuned(type: TuningType, value: Float)
}