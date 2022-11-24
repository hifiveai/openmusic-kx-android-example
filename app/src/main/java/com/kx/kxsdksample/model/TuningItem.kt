package com.kx.kxsdksample.model

import kotlin.math.abs

class TuningItem(
    var type:TuningType,
    var min:Float,
    var max:Float,
    var value:Float
){
    val progress:Int
        get() {
            val temp = abs((value - min)/ (max - min) * 100)
            return temp.toInt()
        }

    fun tuningValue(progress: Int): Float {
        return progress / 100.0f * (max - min) + min
    }
}

/**
 * 调音类型
 */
enum class TuningType(private val value:Int, private val title:String) {
    /**
     *  伴奏
     */
    ACC_VOLUME(1, "伴奏"),
    /**
     *  音调
     */
    KEY_VALUE(2, "音调"),
    /**
     *  人声(录音音量)
     */
    RECORD_VOLUME(3, "人声"),
    /**
     *  美声
     */
    REVERB_VALUE(4, "美声"),
    /**
     *  人声移动
     */
    RECORD_OFFSET(5, "人声移动");

    fun code():Int{
        return value
    }

    fun typeName():String {
        return title
    }

    companion object {
        fun getTuningType(value:Int):TuningType? {
            for (type in values()) {
                if (type.code() == value) {
                    return type
                }
            }
            return null
        }
    }
}