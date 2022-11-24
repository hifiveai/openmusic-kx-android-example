package com.kx.kxsdksample.model
//用于GSON
data class WorksDataItem(
    var type: Int,
    var duration: Int,
    var maxScore: Float,
    var minScore: Float,
    var progress: Float,
    var score: Float,
    var songId: String,
    var name: String,
    var singer: String,
    var recordPath: String,

    /**
     * 歌曲版本信息
     */
    //片段相关
    var partDuration:Int = 0,
    //片段开始时间 (秒)
    var startSec:Int = 0,
    //片段结束时间 (秒)
    var endSec:Int = 0
)