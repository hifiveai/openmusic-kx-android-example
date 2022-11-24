package com.kx.kxsdksample.ui.views


import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.DialogPlaybackTuningBinding
import com.kx.kxsdksample.model.*
import com.kx.kxsdksample.tools.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PlaybackTuningDialog(
    context: Context,
    private val accVolume: TuningItem,
    private val recordVolume: TuningItem,
    private val reverbValue: TuningItem,
    private val recordOffset: TuningItem,
    private val listener: TuningListener,
) : Dialog(context, R.style.CustomProgressDialog), SeekBar.OnSeekBarChangeListener,
    View.OnClickListener {

    private val binding: DialogPlaybackTuningBinding =
        DialogPlaybackTuningBinding.inflate(LayoutInflater.from(context))

    private var accSeekBar: SeekBar
    private var accTv: TextView
    private var offsetSeekBar: SeekBar
    private var offsetTv: TextView
    private var recordSeekBar: SeekBar
    private var recordTv: TextView
    private var reverbSeekBar: SeekBar
    private var reverbTv: TextView
    private var reverbLayout: LinearLayout
    private var reverbTipsTv: TextView
    private var addBtn: LinearLayout
    private var minusBtn: LinearLayout

    init {
        setContentView(binding.root)
        //伴奏
        accSeekBar = binding.playbackTuningAccSeekBar
        accSeekBar.setOnSeekBarChangeListener(this)
        accSeekBar.progress = accVolume.progress
        accTv = binding.playbackTuningAccSeekTv
        accTv.text = "${accVolume.progress}%"
        //人声移动
        offsetSeekBar = binding.playbackTuningKeySeekBar
        offsetSeekBar.setOnSeekBarChangeListener(this)
        offsetSeekBar.progress = recordOffset.progress
        offsetTv = binding.playbackTuningKeyTv
        offsetTv.text = "${recordOffset.value.toInt()}"
        addBtn = binding.playbackTuningKeyAdd
        addBtn.setOnClickListener(this)
        minusBtn = binding.playbackTuningKeyMinus
        minusBtn.setOnClickListener(this)


        //人声
        recordSeekBar = binding.playbackTuningRecordSeekBar
        recordSeekBar.setOnSeekBarChangeListener(this)
        recordSeekBar.progress = recordVolume.progress
        recordTv = binding.playbackTuningRecordSeekTv
        recordTv.text = "${recordVolume.progress}%"
        //美声
        reverbLayout = binding.playbackTuningReverbValueLayout
        reverbTipsTv = binding.playbackTuningReverbTips
        reverbSeekBar = binding.playbackTuningReverbSeekBar
        reverbSeekBar.setOnSeekBarChangeListener(this)
        reverbSeekBar.progress = reverbValue.progress
        reverbTv = binding.playbackTuningReverbSeekTv
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
        if (fromUser && seekBar != null) {
            doProgressChanged(seekBar)
        }
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

    private fun updateRecordOffset() {
        offsetTv.text = "${recordOffset.value.toInt()}"
        offsetSeekBar.progress = recordOffset.progress
        listener.onTuned(TuningType.RECORD_OFFSET, recordOffset.value)
    }

    private fun doProgressChanged(seekBar: SeekBar) {
        when (seekBar) {
            accSeekBar -> {
                accTv.text = "${seekBar.progress}%"
                listener.onTuned(
                    TuningType.ACC_VOLUME,
                    accVolume.tuningValue(seekBar.progress)
                )
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
                listener.onTuned(
                    TuningType.REVERB_VALUE,
                    reverbValue.tuningValue(seekBar.progress)
                )
            }
            offsetSeekBar -> {
                val temp: Int = recordOffset.tuningValue(seekBar.progress).toInt()
                recordOffset.value = temp.toFloat()
                updateRecordOffset()
            }
        }
    }

    override fun onClick(view: View) {
        when (view) {
            addBtn -> {
                when {
                    recordOffset.value < -15.0f -> {
                        recordOffset.value = -15.0f
                    }
                    recordOffset.value < -10.0f -> {
                        recordOffset.value = -10.0f
                    }
                    recordOffset.value < -5.0f -> {
                        recordOffset.value = -5.0f
                    }
                    recordOffset.value < 0.0f -> {
                        recordOffset.value = 0.0f
                    }
                    recordOffset.value < 5.0f -> {
                        recordOffset.value = 5.0f
                    }
                    recordOffset.value < 10.0f -> {
                        recordOffset.value = 10.0f
                    }
                    recordOffset.value < 15.0f -> {
                        recordOffset.value = 15.0f
                    }
                    recordOffset.value < 20.0f -> {
                        recordOffset.value = 20.0f
                    }
                }
                updateRecordOffset()
            }
            minusBtn -> {
                when {
                    recordOffset.value > 15 -> {
                        recordOffset.value = 15.0f
                    }
                    recordOffset.value > 10 -> {
                        recordOffset.value = 10.0f
                    }
                    recordOffset.value > 5 -> {
                        recordOffset.value = 5.0f
                    }
                    recordOffset.value > 0 -> {
                        recordOffset.value = 0.0f
                    }
                    recordOffset.value > -5 -> {
                        recordOffset.value = -5.0f
                    }
                    recordOffset.value > -10 -> {
                        recordOffset.value = -10.0f
                    }
                    recordOffset.value > -15 -> {
                        recordOffset.value = -15.0f
                    }
                    recordOffset.value > -20 -> {
                        recordOffset.value = -20.0f
                    }
                }
                updateRecordOffset()
            }
        }
    }
}