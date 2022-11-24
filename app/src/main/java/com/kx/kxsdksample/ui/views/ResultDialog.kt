package com.kx.kxsdksample.ui.views


import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.DialogResultBinding
import com.kx.kxsdksample.tools.ScreenUtil
import com.kx.kxsdksample.tools.Utils


class ResultDialog(context: Context, private val score:Int,private val  max:Int,private val  min:Int, private val complete:Int) : Dialog(context, R.style.CustomProgressDialog) {

    private val binding: DialogResultBinding = DialogResultBinding.inflate(LayoutInflater.from(context))
    private var maxProgress: View
    private var maxLayout:ConstraintLayout
    private var minProgress:View
    private var minLayout:ConstraintLayout
    private var completeProgress:View
    private var completeLayout:ConstraintLayout

    private var isFirst = true

    init {

        Utils.log("ResultDialog s:$score, max:$max, min:$min, c:$complete")
        setContentView(binding.root)
        setCancelable(false)
        val scoreTv = binding.resultScoreTv
        scoreTv.text = "$score"

        val maxScoreTv = binding.resultMaxScoreTv
        maxScoreTv.text = "$max"
        maxProgress = binding.resultMaxScoreProgress
        maxLayout = binding.resultMaxScoreLayout

        val minScoreTv = binding.resultMinScoreTv
        minScoreTv.text = "$min"
        minProgress = binding.resultMinScoreProgress
        minLayout = binding.resultMinScoreLayout

        val completeTv = binding.resultCompletedTv
        completeTv.text = "${complete}%"
        completeProgress = binding.resultCompletedProgress
        completeLayout = binding.resultCompletedLayout

        val closeBtn = binding.resultCloseBtn
        closeBtn.setOnClickListener {
            dismiss()
        }
    }



    private fun updateWidth(value:Int, layout: ConstraintLayout){
        val width:Float =  ScreenUtil.resultProgressWidth * value / 100.0f
        if (layout.minWidth < width) {
            layout.layoutParams.width = width.toInt()
            layout.requestLayout()
        }
    }

    override fun onStart() {
        super.onStart()
        if (isFirst) {
            isFirst = false
            updateWidth(max,  maxLayout)
            updateWidth(min,  minLayout)
            updateWidth(complete, completeLayout)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

}