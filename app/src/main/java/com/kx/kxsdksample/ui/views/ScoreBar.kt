package com.kx.kxsdksample.ui.views

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.ScoreBarBinding


class ScoreBar(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val binding = ScoreBarBinding.inflate(LayoutInflater.from(context))
    private var progressBar:ProgressBar = binding.scoreBarProgress

    init {
        progressBar.progress = 0
        if (attrs != null) {
            val attributes: TypedArray? = context.obtainStyledAttributes(attrs, R.styleable.ScoreBar)
            if (attributes != null) {
                progressBar.progress = attributes.getInteger(R.styleable.ScoreBar_score, 0)
                attributes.recycle()
            }
        }
    }

    fun setScore(score:Int){
        progressBar.progress = score
    }
}
