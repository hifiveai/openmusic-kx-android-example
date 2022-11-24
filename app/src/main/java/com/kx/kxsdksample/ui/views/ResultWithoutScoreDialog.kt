package com.kx.kxsdksample.ui.views


import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.DialogResultBinding
import com.kx.kxsdksample.databinding.DialogResultNoScoreBinding
import com.kx.kxsdksample.tools.ScreenUtil
import com.kx.kxsdksample.tools.Utils


class ResultWithoutScoreDialog(context: Context, private val complete:Int) : Dialog(context, R.style.CustomProgressDialog) {

    private val binding: DialogResultNoScoreBinding = DialogResultNoScoreBinding.inflate(LayoutInflater.from(context))
    private var completeProgress: View
    private var completeLayout:ConstraintLayout

    private var isFirst = true

    init {

        setContentView(binding.root)
        setCancelable(false)

        val completeTv = binding.noScoreResultCompletedTv
        completeTv.text = "${complete}%"
        completeProgress = binding.noScoreResultCompletedProgress
        completeLayout = binding.noScoreResultCompletedLayout

        val closeBtn = binding.noScoreResultCloseBtn
        closeBtn.setOnClickListener {
            dismiss()
        }
    }



    private fun updateWidth(value:Int, layout: ConstraintLayout){
        val width:Float =  ScreenUtil.resultProgressWidth * value / 100.0f
        val minWidth = layout.minWidth
        if (minWidth < width) {
            layout.layoutParams.width = width.toInt()
            layout.requestLayout()
        }
    }

    override fun onStart() {
        super.onStart()
        if (isFirst) {
            isFirst = false
            updateWidth(complete, completeLayout)
        }
    }

}