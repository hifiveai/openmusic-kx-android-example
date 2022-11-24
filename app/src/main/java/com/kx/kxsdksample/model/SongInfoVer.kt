package com.kx.kxsdksample.model

import java.io.Serializable

class SongInfoVer(
    //片段相关
    var partDuration:Int = 0,
    //片段开始时间 (秒)
    var startSec:Int = 0,
    //片段结束时间 (秒)
    var endSec:Int = 0
) : Serializable