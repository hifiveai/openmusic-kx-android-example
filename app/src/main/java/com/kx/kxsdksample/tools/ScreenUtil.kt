package com.kx.kxsdksample.tools

import android.content.res.Resources
import android.graphics.Point
import android.util.TypedValue

/**
 * 分辨率工具类
 */
class ScreenUtil private constructor() {
    companion object {
        private var size:Point = Point(0, 0)
        private var dialogProgressWidth:Float = 0f
        private var scoresWidth:Float = 0f
        private var barCount:Int = 0
        /**
         * 单句得分柱的宽度
         */
        val barWidth:Float = dp2px(12f)
        /**
         * 设备屏幕宽度
         */
        val width:Int
            get (){
                return size.x
            }
        /**
         * 设备屏幕高度
         */
        val height:Int
            get (){
                return size.y
            }
        /**
         * 演唱结果页中，得分/演唱进度条宽度
         */
        val resultProgressWidth:Float
            get() {
                return dialogProgressWidth
            }
        /**
         * 演唱页/回放页中，单句得分柱形图区域宽度
         */
        val scoreBarViewsWidth:Float
            get() {
                return scoresWidth
            }
        /**
         * 演唱页/回放页中，单句得分柱形图区域可容纳的得分柱数量
         */
        val scoreBarSize: Int
            get() {
                return barCount
            }



        fun init(resources:Resources) {
            val outMetrics = resources.displayMetrics
            size.x = outMetrics.widthPixels
            size.y = outMetrics.heightPixels
            dialogProgressWidth = outMetrics.widthPixels - dp2px(120f)
            scoresWidth = outMetrics.widthPixels - dp2px(36f)
            barCount = (scoresWidth / barWidth).toInt()
        }

        /**
         * 根据手机的分辨率从 dp 转成为 px
         */
        fun dp2px(dpValue: Float): Float {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dpValue, Resources.getSystem().displayMetrics)
        }

        /**
         * 根据手机的分辨率从 px 转成为 dp
         */
        fun px2dp(pxValue: Float): Float {
            val scale: Float = Resources.getSystem().displayMetrics.density
            return (pxValue / scale)
        }
    }
}