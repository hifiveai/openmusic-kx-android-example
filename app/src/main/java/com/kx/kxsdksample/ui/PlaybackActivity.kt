package com.kx.kxsdksample.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hero.kxktvsdk.KXHandleBlock
import com.hero.kxktvsdk.KXKTVError
import com.hero.kxktvsdk.KXKTVSDKValue
import com.hero.kxktvsdk.KXOkEditPlayer
import com.kx.kxsdksample.BaseActivity
import com.kx.kxsdksample.MainActivity
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.ActivityPlaybackBinding
import com.kx.kxsdksample.model.*
import com.kx.kxsdksample.service.DataManager
import com.kx.kxsdksample.service.HeadsetPlugReceiver
import com.kx.kxsdksample.tools.ScreenUtil
import com.kx.kxsdksample.tools.Utils
import com.kx.kxsdksample.ui.views.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PlaybackActivity : BaseActivity(), TuningListener, KXOkEditPlayer.KXOkEditPlayerDelegate {

    private lateinit var song: SongInfo
    private lateinit var binding: ActivityPlaybackBinding
    private var isPlaying = false

    private lateinit var headsetPlugReceiver: HeadsetPlugReceiver

    private lateinit var trackLayout: ConstraintLayout
    private lateinit var trackView: FrameLayout
    private lateinit var lyricView: FrameLayout
    private lateinit var timeTv: TextView
    private lateinit var playBtn: ImageView
    private lateinit var seekBar: SeekBar

    private lateinit var scoreLayout: ConstraintLayout
    private var scoreViews: RecyclerView? = null

    //用于显示得分柱形图
    private var scoreArray: MutableList<Int>? = null
    private var scoreAdapter: ScoreRecyclerAdapter? = null
    private var singleScoreTv: TextView? = null
    private var totalScoreTv: TextView? = null


    //测试用：记录得分，与回放页同步
//    private var scoreList = ArrayList<Int>()
    private lateinit var loadingView: LoadingView

    private var scoreBarIndex: Int = 0
    private var minScore: Float = 0f
    private var maxScore: Float = 0f
    private var totalScore: Float = 0f

    private var newWork = true
    private var recordChanged = false
    private var duration: Float = 0f
    private var draggedSeekBar = false

    private val kxPlayer = KXOkEditPlayer(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        song = intent.getSerializableExtra("ktv_song") as SongInfo
//        var list = intent.getIntegerArrayListExtra("ktv_score")
//        if (list != null) {
//            scoreList.addAll(list)
//        }
        newWork = !DataManager.manager.hasWork(song.songId, song.recordPath)

        binding = ActivityPlaybackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val titleTv = binding.playbackTitleTv
        titleTv.text = song.name

        val backBtn = binding.playbackBack
        backBtn.setOnClickListener {
            onBack()
        }

        val scoreBtn = binding.playbackScoreBtn
        scoreBtn.setOnClickListener {
            showResult()
        }

        timeTv = binding.playbackTitleTimeTv
        timeTv.text = "00:00|00:00"

        seekBar = binding.playbackSeekBar

        trackLayout = binding.playbackTrackLayout
        trackView = binding.playbackTrackView
        trackLayout.visibility = View.GONE

        lyricView = binding.playbackLyricLayout

        scoreLayout = binding.playbackScoreLayout

        if (song.type == 0) {
            scoreLayout.visibility = View.GONE
        } else {
            scoreLayout.visibility = View.VISIBLE
            scoreViews = binding.playbackScoreView
            scoreArray = MutableList(ScreenUtil.scoreBarSize) { 0 }
            val linearLayoutManager = LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL, false
            )
            scoreViews!!.layoutManager = linearLayoutManager
            scoreAdapter = ScoreRecyclerAdapter(this, scoreArray!!)
            scoreViews!!.adapter = scoreAdapter!!
            //音程区域禁用手势滑动
            scoreViews!!.setOnTouchListener { _, _ -> true }
            singleScoreTv = binding.playbackSingleScoreTv
            totalScoreTv = binding.playbackTotalScoreTv
        }

        playBtn = binding.playbackPlay
        playBtn.setOnClickListener {
            if (!isPlaying && kxPlayer.currKTVStatus == KXKTVSDKValue.KXKTVPlayStatus.KXPlayStatusClose) {
                startPlayer()
            } else {
                if (isPlaying) {
                    kxPlayer?.pauseKTV()
                } else {
                    kxPlayer?.startKTV()
                }
                updatePlayBtn(!isPlaying)
            }
        }

        val editorBtn = binding.playbackEditBtn
        editorBtn.setOnClickListener {
            Utils.toast(this, "敬请期待!")
        }

        val tuningBtn = binding.playbackTuningBtn
        tuningBtn.setOnClickListener {
            doTuning()
        }

        val againBtn = binding.playbackAgainBtn
        againBtn.setOnClickListener {
            doAgain()
        }

        val saveBtn = binding.playbackSaveBtn
        saveBtn.setOnClickListener {
            when {
                newWork -> {
                    doSave(true)
                }
                recordChanged -> {
                    showSaveMenus()
                }
                else -> {
                    Utils.toast(this, "已保存！")
                }
            }
        }

        registerHeadsetPlugReceiver()

        loadingView = LoadingView(this)

        trackLayout.visibility = if (song.type == 2) {
            View.VISIBLE
        } else {
            View.GONE
        }
        updatePlayBtn(false)
        duration = (song.dur * song.progress / 100).toFloat()
        initSeekBar()

        startPlayer()
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

    private fun initSeekBar() {
        seekBar.max = 100
        seekBar.progress = 0
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updatePlayerTime(progress.toFloat())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                draggedSeekBar = true
                if (scoreViews != null && scoreAdapter != null) {
                    scoreBarIndex = 0
                    scoreArray = MutableList(ScreenUtil.scoreBarSize) { 0 }
                    scoreAdapter!!.setList(scoreArray!!)
                    scoreViews!!.smoothScrollToPosition(0)

                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                kxPlayer?.currKTVPos = (seekBar.progress.toFloat() * 0.01f) * kxPlayer.currKTVDuration
                draggedSeekBar = false
            }
        })
    }

    private fun startPlayer() {

        if (!isPlaying && kxPlayer.currKTVStatus == KXKTVSDKValue.KXKTVPlayStatus.KXPlayStatusClose) {
            loadingView.show()
            seekBar.progress = 0
            isPlaying = true
            duration = 0.0f
            trackView.removeAllViews()
            if (trackView.visibility == View.VISIBLE) {
                kxPlayer.setTrackView(trackView)
            }
            lyricView.removeAllViews()
            kxPlayer.setLrcView(lyricView)
            startPlayerTimer()
            updatePlayBtn(isPlaying)
            object : Thread() {
                override fun run() {
                    try {
                        val recordPath = Utils.cachePath(song.recordPath)
                        val item = DataManager.manager.convert2KTVSDKItem(song)
                        kxPlayer.openKTV(item, recordPath, true)
                    } catch (e: Exception) {
                        Utils.log("openKtv err:$e")
                    }
                }
            }.start()
        }
    }

    override fun onBack() {
        when {
            newWork -> {
                showNewWorkDialog()
            }
            recordChanged -> {
                showSaveMenus()
            }
            else -> {
                back2List()
            }
        }
    }

    private fun updatePlayBtn(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        if (isPlaying) {
            playBtn.setImageResource(R.drawable.pause)
        } else {
            playBtn.setImageResource(R.drawable.play)
        }
    }

    private var timeThread: Thread? = null

    private fun startPlayerTimer() {
        Utils.log("PlaybackActivity startPlayerTimer $timeThread")
        if (timeThread == null) {
            timeThread = object : Thread() {
                override fun run() {
                    try {

                        while (!isInterrupted) {
                            if (!draggedSeekBar && isPlaying) {
                                val time: Float = kxPlayer.currKTVPos * 1000f
                                if (duration == 0.0f) {
                                    duration = kxPlayer.currKTVDuration * 1000.0f
                                }
                                GlobalScope.launch(Dispatchers.Main) {
                                    if (kxPlayer.currKTVStatus == KXKTVSDKValue.KXKTVPlayStatus.KXPlayStatusPlay) {
                                        updatePlayerTime(time)
                                        seekBar.progress = ((time/duration) * 100).toInt()
                                    }
                                }
                            }
                            sleep(50)
                        }
                    } catch (e: Exception) {
                        Utils.log("PlaybackActivity timeThread err:$e")
                    }
                }
            }
            timeThread?.start()
        }
    }

    private fun cancelPlayerTimer() {
        isPlaying = false
        timeThread?.interrupt()
        timeThread = null
    }


    private fun updatePlayerTime(time: Float) {
        timeTv.text =
            Utils.formatPlayerTime(time.toLong()) + "|" + Utils.formatPlayerTime(duration.toLong())

    }

    private fun doTuning() {
        val accItem = TuningItem(TuningType.ACC_VOLUME, 0.0f, 1.0f, kxPlayer.accompVolume)
        val recordItem = TuningItem(TuningType.RECORD_VOLUME, 0.0f, 1.0f, kxPlayer.recordVolume)
        val reverbItem = TuningItem(TuningType.REVERB_VALUE, 0.0f, 1.0f, kxPlayer.reverbValue)
        val recordOffset = TuningItem(TuningType.RECORD_OFFSET, -20.0f, 20.0f, 0.0f)
        val dialog = PlaybackTuningDialog(this, accItem, recordItem, reverbItem, recordOffset, this)
        dialog.show()
    }

    //
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


    override fun onPause() {
        super.onPause()
        isPlaying = false
        kxPlayer.pauseKTV()
        updatePlayBtn(isPlaying)
    }

    override fun finish() {
        super.finish()
        loadingView.close()
        cancelPlayerTimer()
        kxPlayer.closeKTV()
    }

    override fun onDestroy() {
        unregisterReceiver(headsetPlugReceiver)
        Utils.log("onDestroy() cancelPlayerTimer")
        super.onDestroy()
    }

    private fun doAgain() {
        kxPlayer.pauseKTV()
        Utils.log("doAgain() cancelPlayerTimer")
        cancelPlayerTimer()
        Utils.delRecordFile(song.recordPath)
        val newSong = SongInfo(song, Utils.recordFileName(song.songId))
        val intent = Intent(this, KtvActivity::class.java)
        intent.putExtra("ktv_song", newSong)
        startActivity(intent)
//        //重新进入歌曲列表，选择演唱
        finish()
    }

    private fun showResult() {
        if (song.type > 0) {
            val resultDialog = ResultDialog(
                this,
                song.score.toInt(),
                song.maxScore.toInt(),
                song.minScore.toInt(),
                song.progress.toInt()
            )
            resultDialog.show()
        } else {
            val resultDialog = ResultWithoutScoreDialog(
                this,
                song.progress.toInt()
            )
            resultDialog.show()
        }
    }

    private fun back2List() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("to_work_tab", true)
        startActivity(intent)
        finish()
    }

    private fun showNewWorkDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("温馨提示")
        builder.setMessage("您的作品还未保存！")
        builder.setNegativeButton("保存") { _, _ ->
            saveNewWork(song)
        }
        builder.setPositiveButton("不保存") { _, _ ->
            back2List()
        }
        builder.setNeutralButton("取消") { _, _ ->
        }
        val dialog = builder.create()
        dialog.show()
        //系统对话框的按钮文本颜色默认为白色，需要重新设置为黑色
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setTextColor(Utils.getColor(this, R.color.light_gray))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Utils.getColor(this, R.color.black))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Utils.getColor(this, R.color.light_gray))
    }


    private fun showSaveMenus() {
        val dialog = BottomMensDialog(this, object : BottomMenusListener {
            override fun onClickedBottomMenu(isOverride: Boolean) {
                doSave(isOverride)
            }

            override fun onClickedBottomNoSave() {
                back2List()
            }
        })
        dialog.show()
    }

    private fun saveNewWork(song: SongInfo) {
        kxPlayer.closeKTV()
        updatePlayBtn(false)

        loadingView.show()
        object : Thread() {
            override fun run() {
                try {
                    DataManager.manager.addWork(song)
                } catch (e: Exception) {
                }
                runOnUiThread {
                    loadingView.close()
                    Utils.toast(this@PlaybackActivity, "已保存！")
                    back2List()
                }
            }
        }.start()
    }

    private fun saveResult(isNew: Boolean, song: SongInfo, success: Boolean, err: KXKTVError?) {
        recordChanged = false
        newWork = false
        Utils.log("saveResult success:$success, err:$err")
        if (err != null) {
            Utils.log("saveResult success:$success, err:(${err.errorCode})${err.errorMsg}")
        }
        if (success) {
            if (isNew) {
                saveNewWork(song)
            }else{
                runOnUiThread {
                    loadingView.close()
                    Utils.toast(this@PlaybackActivity, "已保存！")
                    back2List()
                }
            }
        } else {
            runOnUiThread {
                loadingView.close()
                if (err != null && err!!.errorMsg != null) {
                    Utils.toast(this, err!!.errorMsg)
                } else {
                    Utils.toast(this, "保存作品失败！")
                }
            }
        }
    }

    private fun doSave(isOverride: Boolean) {
        updatePlayBtn(false)
        loadingView.show()
        kxPlayer.pauseKTV()
        cancelPlayerTimer()
        val newSong = SongInfo(song)
        if (!isOverride) {
            newSong.recordPath = Utils.recordFileName(newSong.songId)
        }
        val recordPath = Utils.cachePath(newSong.recordPath)
        if (isOverride) {
            //覆盖原作品
            kxPlayer.coverSaveRec(object : KXHandleBlock {
                override fun handleProgressBlock(p0: Float) {
                }

                override fun handleCompleteBlock(success: Boolean, err: KXKTVError?) {
                    saveResult(newWork, newSong, success, err)
                }
            })
        } else {
            //保存新作品
            kxPlayer.exportNewRecFile(recordPath, object : KXHandleBlock {
                override fun handleProgressBlock(p: Float) {
                }

                override fun handleCompleteBlock(success: Boolean, err: KXKTVError?) {
                    saveResult(true, newSong, success, err)
                }
            })
        }
    }

    override fun onTuned(type: TuningType, value: Float) {
        when (type) {
            TuningType.ACC_VOLUME -> {
                recordChanged = true
                kxPlayer.accompVolume = value
            }
            TuningType.REVERB_VALUE -> {
                recordChanged = true
                kxPlayer.reverbValue = value
            }
            TuningType.RECORD_VOLUME -> {
                recordChanged = true
                kxPlayer.recordVolume = value
            }
            TuningType.RECORD_OFFSET -> {
//                recordChanged = true
            }
        }
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
    }

    override fun recPlayerDidCallbackTotalScore(score: Float) {
        totalScore = score
        updateTotalScoreLabel(totalScore.toInt())
    }

    override fun recPlayerDidError(error: KXKTVError?) {
        loadingView.close()
        recPlayerDidPlayEnd()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("温馨提示")
        builder.setMessage("播放失败！作品已损坏...")
        builder.setNegativeButton("确定") { _, _ ->
            loadingView.show()
            DataManager.manager.delWork(song) {
                runOnUiThread {
                    loadingView.close()
                    back2List()
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
        //系统对话框的按钮文本颜色默认为白色，需要重新设置为黑色
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Utils.getColor(this, R.color.black))
    }

    override fun recPlayerDidPlayEnd() {
        try {
            Utils.log("onCompletion() cancelPlayerTimer")
            cancelPlayerTimer()
            updatePlayerTime(duration)
            updatePlayBtn(isPlaying)
            seekBar.progress = 100
        } catch (e: Exception) {
        }
    }

    override fun recPlayerDidRecStatusChanged(status: KXKTVSDKValue.KXKTVPlayStatus) {
        if (status == KXKTVSDKValue.KXKTVPlayStatus.KXPlayStatusOpened) {
            startPlayerTimer()
            updatePlayBtn(true)
        } else if (status == KXKTVSDKValue.KXKTVPlayStatus.KXPlayStatusPlay) {
            loadingView.close()
        }
    }

}