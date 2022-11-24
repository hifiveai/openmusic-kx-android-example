package com.kx.kxsdksample.model

import com.kx.kxsdksample.tools.Utils
import java.io.Serializable

//歌曲信息类
class SongInfo:Serializable {
    /**
     * 歌曲ID
     */
    var songId = ""
    /**
     * 歌曲名
     */
    var name = ""
    /**
     * 歌手名
     */
    var singer = ""

    /**
     * 导唱文件Url
     */
    var guideUrl = ""

    /**
     * 伴奏文件Url
     */
    var musicUrl = ""

    /**
     * 静态歌词文件Url
     */
    var staticLyricUrl = ""
    /**
     * 动态歌词文件Url
     */
    var dynamicLyricUrl = ""
    /**
     * 评分歌词文件Url
     */
    var scoreUrl = ""

    /**
     * 导唱文件本地(缓存)路径
     */
    var guidePath = ""

    /**
     * 伴奏文件本地(缓存)路径
     */
    var musicPath = ""

    /**
     * 静态歌词文件本地(缓存)路径
     */
    var staticLyricPath = ""
    /**
     * 动态歌词文件本地(缓存)路径
     */
    var dynamicLyricPath = ""
    /**
     * 评分歌词文件本地(缓存)路径
     */
    var scorePath = ""

    /**
     * 作品文件名
     */
    var recordPath = ""

    /**
     * 作品得分
     */
    var score:Float = 0.0f

    /**
     * 作品最低得分
     */
    var minScore:Float = 0.0f

    /**
     * 作品最高得分
     */
    var maxScore:Float = 0.0f

    /**
     * 作品完成度
     */
    var progress:Float = 0.0f

    /**
     * 歌曲时长(毫秒)
     */
    var dur:Double = 0.0
    /**
     * 歌曲类型：
     * 2-逐字评分，1-单句评分，0-无评分(默认)
     */
    var type:Int = 3


    /**
     * 歌曲版本信息
     */
    var songSubVersion:SongInfoVer = SongInfoVer(0,0,0)

    var notFoundLyric:Boolean = false
        get() {
            return scoreUrl.isEmpty() && staticLyricUrl.isEmpty() && dynamicLyricUrl.isEmpty()
        }

    constructor(){
    }

    constructor(item: SongDataItem){
        this.songId = item.song_id
        this.name = item.song_name
        this.singer = item.singer
        this.type = item.type
    }

    constructor(item:WorksDataItem) {
        this.songId = item.songId
        this.name = item.name
        this.singer = item.singer
        this.type = item.type
        this.recordPath = item.recordPath
        this.score = item.score
        this.minScore = item.minScore
        this.maxScore = item.maxScore
        this.progress = item.progress
        Utils.log("WorksDataItem duration:${item.duration}")
        this.dur = item.duration.toDouble()

        this.songSubVersion = SongInfoVer(item.partDuration,item.startSec,item.endSec)
    }

    constructor(song: SongInfo) {
        this.songId = song.songId
        this.name = song.name
        this.singer = song.singer
        this.guideUrl = song.guideUrl
        this.musicUrl = song.musicUrl
        this.scoreUrl = song.scoreUrl
        this.staticLyricUrl = song.staticLyricUrl
        this.dynamicLyricUrl = song.dynamicLyricUrl

        this.guidePath = song.guidePath
        this.musicPath = song.musicPath
        this.staticLyricPath = song.staticLyricPath
        this.dynamicLyricPath = song.dynamicLyricPath
        this.scorePath = song.scorePath
        this.recordPath = song.recordPath
        this.type = song.type
        this.score = song.score
        this.minScore = song.minScore
        this.maxScore = song.maxScore
        this.progress = song.progress
        this.dur = song.dur

        this.songSubVersion = song.songSubVersion
    }

    constructor(song: SongInfo, recordPath:String) {
        this.songId = song.songId
        this.name = song.name
        this.singer = song.singer
        this.guideUrl = song.guideUrl
        this.musicUrl = song.musicUrl
        this.scoreUrl = song.scoreUrl
        this.scoreUrl = song.scoreUrl
        this.staticLyricUrl = song.staticLyricUrl
        this.dynamicLyricUrl = song.dynamicLyricUrl

        this.guidePath = song.guidePath
        this.musicPath = song.musicPath
        this.staticLyricPath = song.staticLyricPath
        this.dynamicLyricPath = song.dynamicLyricPath
        this.scorePath = song.scorePath

        this.recordPath = recordPath
        this.type = song.type

        this.songSubVersion = song.songSubVersion
    }

    /**
     * 歌曲信息转作品信息
     */
    fun createWorkItem() : WorksDataItem{
        val duration = this.dur.toInt()
        Utils.log("createWorkItem duration:$duration")
        return WorksDataItem(
            this.type,
            duration,
            this.maxScore,
            this.minScore,
            this.progress,
            this.score,
            this.songId,
            this.name,
            this.singer,
            this.recordPath,
            this.songSubVersion.partDuration,
            this.songSubVersion.startSec,
            this.songSubVersion.endSec
        )
    }

    fun setScore(total:Float, min:Float, max:Float, progress:Float, dur:Double){
        this.score = total
        this.minScore = min
        this.maxScore = max
        this.progress = progress
        this.dur = dur
    }
}