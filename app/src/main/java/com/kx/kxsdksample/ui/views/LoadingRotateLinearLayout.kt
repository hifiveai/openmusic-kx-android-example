package com.kx.kxsdksample.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import com.kx.kxsdksample.R

/**
 * @类说明:loading图旋转的布局
 */
class LoadingRotateLinearLayout : LinearLayout {
    private var mContext: Context

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mContext = context
        addAnimationImageView()
    }

    constructor(context: Context) : super(context) {
        mContext = context
        addAnimationImageView()
    }

    fun addAnimationImageView() {
        val rotateAni = AnimationUtils.loadAnimation(
            mContext,
            R.anim.loading_rotate
        )
        val lin = LinearInterpolator()
        rotateAni.interpolator = lin
        val image = ImageView(mContext)
        image.setImageResource(R.drawable.loading_image)
        removeAllViews()
        this.addView(image)
        image.clearAnimation()
        image.startAnimation(rotateAni)
    }
}