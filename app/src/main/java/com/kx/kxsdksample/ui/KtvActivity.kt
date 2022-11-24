package com.kx.kxsdksample.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hero.kxktvsdk.KXKTVError
import com.hero.kxktvsdk.KXKTVSDKValue
import com.hero.kxktvsdk.KXOkAudioPlayer
import com.kx.kxsdksample.BaseActivity
import com.kx.kxsdksample.MainActivity
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.ActivityKtvBinding
import com.kx.kxsdksample.model.SongInfo
import com.kx.kxsdksample.model.TuningItem
import com.kx.kxsdksample.model.TuningType
import com.kx.kxsdksample.service.DataManager
import com.kx.kxsdksample.service.HeadsetPlugReceiver
import com.kx.kxsdksample.tools.ScreenUtil
import com.kx.kxsdksample.tools.Utils
import com.kx.kxsdksample.tools.WavWriter
import com.kx.kxsdksample.ui.views.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class KtvActivity : BaseActivity(), TuningListener, KXOkAudioPlayer.KXOkAudioPlayerDelegate {
    private lateinit var binding: ActivityKtvBinding

    private lateinit var song: SongInfo
    private lateinit var headsetPlugReceiver: HeadsetPlugReceiver

    private lateinit var trackLayout: ConstraintLayout
    private lateinit var trackView: FrameLayout
    private lateinit var lyricView: FrameLayout
    private lateinit var timeTv: TextView
    private lateinit var playBtn: ImageView
    private lateinit var guideLayout: LinearLayout
    private lateinit var guideVolumeSeekBar: SeekBar

    private lateinit var scoreLayout: ConstraintLayout
    private var scoreViews: RecyclerView? = null

    //用于显示得分柱形图
    private var scoreArray: MutableList<Int>? = null
    private var scoreAdapter: ScoreRecyclerAdapter? = null
    private var singleScoreTv: TextView? = null
    private var totalScoreTv: TextView? = null

    //测试用：记录得分，与回放页同步
//    private var scoreList = ArrayList<Int>()


    private var isPlaying = false
    private var recorder: KXOkAudioPlayer? = null

    private lateinit var loadingView: LoadingView

    private var scoreBarIndex: Int = 0
    private var minScore: Float = 0f
    private var maxScore: Float = 0f
    private var totalScore: Float = 0f
    private var userGuide = false
    private var duration: Double = 0.0
    private var guideVolume: Float = -1f

    private var recWriter: WavWriter? = null
    private var recAccWriter: WavWriter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        song = intent.getSerializableExtra("ktv_song") as SongInfo
        binding = ActivityKtvBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recorder = KXOkAudioPlayer()

        trackLayout = binding.ktvTrackLayout
        trackView = binding.ktvTrackView
        trackLayout.visibility = View.GONE

        timeTv = binding.ktvTitleTimeTv
        timeTv.text = "00:00|00:00"

        scoreLayout = binding.ktvScoreLayout
        if (song.type == 0) {
            scoreLayout.visibility = View.GONE
            //无评分不显示分数lable
            binding.ktvSingleScoreTv.visibility = View.GONE
            binding.ktvTotalScoreTv.visibility = View.GONE
        } else {
            scoreLayout.visibility = View.VISIBLE
            scoreViews = binding.ktvScoreView
            scoreArray = MutableList(ScreenUtil.scoreBarSize) { 0 }
            val linearLayoutManager = LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL, false
            )
            scoreViews!!.layoutManager = linearLayoutManager
            scoreAdapter = ScoreRecyclerAdapter(this, scoreArray!!)
            scoreViews?.adapter = scoreAdapter!!
            //音程区域禁用手势滑动
            scoreViews!!.setOnTouchListener { _, _ -> true }
            singleScoreTv = binding.ktvSingleScoreTv
            totalScoreTv = binding.ktvTotalScoreTv
        }
        val titleTv = binding.ktvTitleTv
        titleTv.text = song.name

        val backBtn = binding.ktvBack
        backBtn.setOnClickListener {
            onBack()
        }

        playBtn = binding.ktvPlay
        playBtn.setOnClickListener {

            if (isPlaying) {
                recorder?.pauseKTV()
            } else {
                recorder?.startKTV()
            }
            updatePlayBtn(!isPlaying)
        }
        guideLayout = binding.ktvGuideVolumeLayout
        guideVolumeSeekBar = binding.ktvGuideVolumeSeekBar

        val guideImg = binding.ktvGuideImg
        val guideTv = binding.ktvGuideTv
        val guideBtn = binding.ktvGuideBtn
        guideLayout.visibility = View.INVISIBLE
        if (song.type == 2) {
            guideImg.setImageResource(R.drawable.guide_u)
            guideTv.setTextColor(Utils.getColor(this, R.color.white_36))
        } else {
            guideVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    guideVolume = progress / 100f
                    recorder?.guideSingVolume = guideVolume
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            })
            guideBtn.setOnClickListener {
                userGuide = !userGuide
                if (userGuide) {
                    if (guideVolume == -1f) {
                        guideVolume = recorder?.accompVolume!!
                    }
                    recorder?.guideSingVolume = guideVolume
                    guideVolumeSeekBar.progress = (guideVolume * 100f).toInt()
                    guideImg.setImageResource(R.drawable.guide_f)
                    guideTv.setTextColor(Utils.focusColor)
                    guideLayout.visibility = View.VISIBLE
                } else {
                    recorder?.guideSingVolume = 0.0f
                    guideImg.setImageResource(R.drawable.guide)
                    guideTv.setTextColor(Utils.white)
                    guideLayout.visibility = View.INVISIBLE
                }
            }
            guideVolumeSeekBar.max = 100
            guideVolumeSeekBar.progress = (guideVolume * 100).toInt()
        }

        val tuningBtn = binding.ktvTuningBtn
        tuningBtn.setOnClickListener {
            doTuning()
        }

        val againBtn = binding.ktvAgainBtn
        againBtn.setOnClickListener {
            doAgain()
        }

        val completedBtn = binding.ktvCompletedBtn
        completedBtn.setOnClickListener {
            doComplete()
        }
        registerHeadsetPlugReceiver()

        loadingView = LoadingView(this)
        loadingView.show()

        lyricView = binding.ktvLyricLayout
        trackLayout.visibility = if (song.type == 2) {
            View.VISIBLE
        } else {
            View.GONE
        }
        updatePlayBtn(false)

        startRecord()
    }


    override fun onBack() {
        recorder?.closeKTV()
        closeTestPcmFiles()
        cancelPlayerTimer()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * 注册监测耳机插拔的BroadcastReceiver
     */
    private fun registerHeadsetPlugReceiver() {
        headsetPlugReceiver = HeadsetPlugReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG)
        registerReceiver(headsetPlugReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            isPlaying = false
            recorder?.pauseKTV()
            updatePlayBtn(isPlaying)
            loadingView.close()
        }
    }

    override fun finish() {
        recorder?.closeKTV()
        closeTestPcmFiles()
        cancelPlayerTimer()

        super.finish()
    }

    override fun onDestroy() {
        unregisterReceiver(headsetPlugReceiver)
        super.onDestroy()
    }

    private fun initTestPcmFiles() {
        closeTestPcmFiles()
        recSize = 0
        recMixSize = 0
        Utils.log("recorder sample:${recorder?.sampleRate}, channel:${recorder?.channel}")
        val recTestPath = Utils.pcmTestFileName(song.songId, false)
//        Utils.delFile(recTestPath)
//        val recF = File(recTestPath)
//        recF.createNewFile()
        recWriter = WavWriter(recTestPath, recorder?.sampleRate!!.value, recorder?.channel!!.value)
        val recMixTextPath = Utils.pcmTestFileName(song.songId, true)
//        Utils.delFile(recMixTextPath)
//        val mixF = File(recMixTextPath)
//        mixF.createNewFile()
        recAccWriter =
            WavWriter(recMixTextPath, recorder?.sampleRate!!.value, recorder?.channel!!.value)
    }

    private fun closeTestPcmFiles() {
        recWriter?.close()
        recWriter = null
        recAccWriter?.close()
        recAccWriter = null
        Utils.log("recorder recSize:$recSize, recMixSize:$recMixSize")
    }


    private fun startRecord() {
        duration = 0.0
        trackView.removeAllViews()
        if (trackView.visibility == View.VISIBLE) {
            recorder?.setTrackView(trackView)
        }
        lyricView.removeAllViews()
        recorder?.setLrcView(lyricView)
        //设置歌词行间距
        recorder?.setLrcLineSpaceHeight(30)
        //设置歌词样式
//        recorder?.setLrcDisplay(KXKTVSDKValue.KTVSDKLyricDisplay.DoubleLine)
        recorder?.delegate = this
        initTestPcmFiles()
        object : Thread() {
            override fun run() {
                try {
                    val recordPath = Utils.cachePath(song.recordPath)
                    val item = DataManager.manager.convert2KTVSDKItem(song)
                    recorder?.openKTV(item, recordPath, true)
                } catch (e: Exception) {
                    Utils.log("openKtv err:$e")
                }
            }
        }.start()
    }


    private var timeThread: Thread? = null

    private fun startPlayerTimer() {
        if (timeThread == null) {
            timeThread = object : Thread() {
                override fun run() {
                    try {
                        while (!isInterrupted) {
                            if (recorder?.currKTVStatus == KXKTVSDKValue.KXKTVPlayStatus.KXPlayStatusPlay) {
                                val time: Float = recorder?.currKTVPos!! * 1000f
                                if (duration == 0.0) {
                                    duration = recorder?.currKTVDuration!! * 1000.0
                                }
                                GlobalScope.launch(Dispatchers.Main) {
                                    updatePlayerTime(time)
                                }
                            }
                            sleep(50)
                        }
                    } catch (e: Exception) {
                        Utils.log("KtvActivity playerTimer err:$e")
                    }
                }
            }
            timeThread?.start()
        }
    }

    private fun cancelPlayerTimer() {
        timeThread?.interrupt()
        timeThread = null
    }

    private fun updatePlayBtn(playing: Boolean) {
        isPlaying = playing
        if (isPlaying) {
            playBtn.setImageResource(R.drawable.pause)
        } else {
            playBtn.setImageResource(R.drawable.play)
        }
    }

    private fun updatePlayerTime(time: Float) {
        timeTv.text =
            Utils.formatPlayerTime(time.toLong()) + "|" + Utils.formatPlayerTime(duration.toLong())
    }

    private fun updateScoreBar(score: Float) {
        if (scoreArray != null && scoreAdapter != null && scoreViews != null) {
            var toEnd = false
            val newScore = score.toInt()
            val index = scoreBarIndex
            if (index >= scoreArray!!.size) {
                scoreArray!!.add(0)
                if (scoreArray!!.size * ScreenUtil.barWidth >= ScreenUtil.scoreBarViewsWidth) {
                    toEnd = true
                }
            }
            scoreArray!![index] = newScore
            scoreAdapter!!.setList(scoreArray!!)
            if (toEnd) {
                scoreViews!!.smoothScrollToPosition(scoreArray!!.size - 1)
            }
            scoreBarIndex = index + 1
        }
    }

    private fun updateSingleScoreLabel(score: Int) {
        if (score >= 0) {
            singleScoreTv?.text = "单句得分：$score"
        } else {
            singleScoreTv?.text = "单句得分：--"
        }
    }

    private fun updateTotalScoreLabel(score: Int) {
        if (score >= 0) {
            totalScoreTv?.text = "总分：$score"
        } else {
            totalScoreTv?.text = "总分：--"
        }
    }

    private fun showResult(progress: Float) {
        if (song.type > 0) {
            val total = totalScore
            val resultDialog = ResultDialog(
                this,
                total.toInt(),
                maxScore.toInt(),
                minScore.toInt(),
                progress.toInt()
            )
            resultDialog.setOnDismissListener {
                song.setScore(total, minScore, maxScore, progress, duration)
                playback()
            }
            resultDialog.show()
        } else {
            val resultDialog = ResultWithoutScoreDialog(this, progress.toInt())
            resultDialog.setOnDismissListener {
                song.setScore(0f, 0f, 0f, progress, duration)
                playback()
            }
            resultDialog.show()
        }
    }

    private fun playback() {
        val intent = Intent(this, PlaybackActivity::class.java)
        intent.putExtra("ktv_song", song)
        startActivity(intent)
        finish()
    }

    private fun doTuning() {
        Utils.log("accompVolume=${recorder?.accompVolume},recordVolume=${recorder?.recordVolume},reverbValue:${recorder?.reverbValue},accompKeyValue:${recorder?.accompKeyValue}")
        val accItem = TuningItem(TuningType.ACC_VOLUME, 0.0f, 1.0f, recorder?.accompVolume!!)
        val recordItem = TuningItem(TuningType.RECORD_VOLUME, 0.0f, 1.0f, recorder?.recordVolume!!)
        val reverbItem = TuningItem(TuningType.REVERB_VALUE, 0.0f, 1.0f, recorder?.reverbValue!!)
        val keyItem = TuningItem(TuningType.KEY_VALUE, -5.0f, 5.0f, recorder?.accompKeyValue!! * 1.0f)
        val dialog = KtvTuningDialog(this, accItem, keyItem, recordItem, reverbItem, this)
        dialog.show()
    }

    //点击《重唱》
    private fun doAgain() {
        Utils.log("开始重唱")
        loadingView.show()
        recorder?.pauseKTV()
        isPlaying = false
        updatePlayerTime(0f)
        cancelPlayerTimer()
        updateSingleScoreLabel(0)
        updateTotalScoreLabel(0)
        maxScore = 0f
        minScore = 0f
        totalScore = 0f
        scoreBarIndex = 0
//        scoreList.clear()
        if (scoreViews != null) {
            scoreArray = MutableList(ScreenUtil.scoreBarSize) { 0 }
            scoreAdapter?.setList(scoreArray!!)
            scoreViews!!.smoothScrollToPosition(0)
        }
        recorder?.closeKTV()
        updatePlayBtn(isPlaying)
        startRecord()
    }

    //点击《完成》
    private fun doComplete() {
        recorder?.pauseKTV()
        val progress: Float = recorder?.currKTVPos!! / recorder?.currKTVDuration!! * 100.0f
        isPlaying = false
        recorder?.closeKTV()
        cancelPlayerTimer()
        closeTestPcmFiles()
        updatePlayBtn(isPlaying)
        showResult(progress)
    }

    //KXOkAudioPlayer.KXOkAudioPlayerDelegate
    override fun recPlayerDidError(err: KXKTVError?) {
        Utils.log("recPlayerDidError, err:${err?.errorMsg}")
        loadingView.close()
    }

    override fun recPlayerDidPlayEnd() {
        isPlaying = false
        cancelPlayerTimer()
        closeTestPcmFiles()

        showResult(100f)
    }

    override fun recPlayerDidRecStatusChanged(status: KXKTVSDKValue.KXKTVPlayStatus) {
        if (status == KXKTVSDKValue.KXKTVPlayStatus.KXPlayStatusOpened) {
            startPlayerTimer()
            updatePlayBtn(true)
            if (guideVolume != -1.0F) {//导唱初始化值舍得
                recorder?.guideSingVolume = guideVolume
            }
        } else if (status == KXKTVSDKValue.KXKTVPlayStatus.KXPlayStatusPlay) {
            loadingView.close()
        }
    }


    override fun recPlayerDidCallbackTotalScore(score: Float) {
        totalScore = score
        updateTotalScoreLabel(score.toInt())
    }


    override fun recPlayerDidCallbackSingleScore(score: Float) {
        if (minScore > score) {
            minScore = score
        }
        if (maxScore < score) {
            maxScore = score
        }
        updateSingleScoreLabel(score.toInt())
        updateScoreBar(score)

        //测试用的
//        scoreList.add(score.toInt())
//        totalScore += score
//        val temp = (totalScore / scoreBar`Index).toInt()
//        updateTotalScoreLabel(temp)
    }

    private var recSize:Int = 0
    private var recMixSize:Int = 0

    override fun recPlayerdidCallbackPCM(pcmData: ByteArray, size: Int) {
//        try {
//            recWriter?.writeData(pcmData, size)
//            recSize += size
//        }catch (e:Exception){
//            e.printStackTrace()
//            Utils.log("recPlayerdidCallbackPCM err:${e.localizedMessage}")
//        }
    }

    override fun recPlayerdidCallbackMixPCM(pcmData: ByteArray, size: Int) {
//        try {
//            recAccWriter?.writeData(pcmData, size)
//            recMixSize += size
//        }catch (e:Exception){
//            e.printStackTrace()
//            Utils.log("recPlayerdidCallbackMixPCM err:${e.localizedMessage}")
//        }
    }

    override fun onTuned(type: TuningType, value: Float) {
        when (type) {
            TuningType.ACC_VOLUME -> {
                recorder?.accompVolume = value
            }
            TuningType.KEY_VALUE -> {
                recorder?.accompKeyValue = value.toInt()
            }
            TuningType.RECORD_VOLUME -> {
                recorder?.recordVolume = value
            }
            TuningType.REVERB_VALUE -> {
                recorder?.reverbValue = value
            }
        }
    }
}