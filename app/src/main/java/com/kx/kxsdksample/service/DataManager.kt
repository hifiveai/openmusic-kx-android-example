package com.kx.kxsdksample.service

import android.content.Context
import android.os.Looper
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hero.kxktvsdk.KTVSDKSongItem
import com.hfopen.sdk.entity.HQListen
import com.hfopen.sdk.hInterface.DataResponse
import com.hfopen.sdk.manager.HFOpenApi
import com.hfopen.sdk.rx.BaseException
import com.kx.kxsdksample.model.SongData
import com.kx.kxsdksample.model.SongInfo
import com.kx.kxsdksample.model.SongInfoVer
import com.kx.kxsdksample.model.WorksData
import com.kx.kxsdksample.tools.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException


/**
 *  数据管理类
 */
class DataManager {


    private val worksPath = Utils.cachePath("my_works")

    private var isInited = false

    val workArray: ArrayList<SongInfo> by lazy {
        arrayListOf()
    }

    private var currTask: DownloadSong? = null

    companion object {
        val manager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            DataManager()
        }
    }

    fun initManager(context: Context) {
        if (!isInited) {
            isInited = true
            checkNetwork(context)
            loadWorks()
        }
    }

    private fun gson(): Gson {
        return GsonBuilder()
            .setPrettyPrinting()
            .serializeSpecialFloatingPointValues() // 这是关键
            .create()
    }

    private fun checkNetwork(context: Context) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val tag: String? = sp.getString("kxsdksample", null)
        if (tag == null) {
            val editor = sp.edit()
            editor.putString("kxsdksample", "kxsdksample")
            editor.apply()
            object : Thread() {
                override fun run() {
                    try {
                        httpGet("http://www.baidu.com")
                    } catch (e: Exception) {
                    }
                }
            }.start()
        }
    }

    /**
     * 作品是否存在
     */
    fun hasWork(songId: String, recordPath: String): Boolean {
        if (workArray.isNotEmpty()) {
            for (work in workArray) {
                if (work.songId == songId && work.recordPath == recordPath) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 歌曲信息转演唱信息
     * @param song 伴奏信息
     */
    fun convert2KTVSDKItem(song:SongInfo) : KTVSDKSongItem {
        Utils.log("convert2KTVSDKItem:\nsongId:${song.songId} \ntype:${song.type} " +
                "\ndynamicLyricPath:${song.dynamicLyricPath} \nstaticLyricPath:${song.staticLyricPath} " +
                "\nscorePath:${song.scorePath} \nmusicPath:${song.musicPath} \nguidePath:${song.guidePath}" +
                "\nstartSec:${song.songSubVersion.startSec} \nendSec:${song.songSubVersion.endSec}")
        return KTVSDKSongItem(song.songId, song.type, song.dynamicLyricPath,
            song.staticLyricPath, song.scorePath, song.musicPath, song.guidePath,
            song.songSubVersion.startSec,song.songSubVersion.endSec)
    }

    /**
     * 加载伴奏列表
     */
    fun loadAccompanimentList(callback: OnLoadedCallback) {
        object : Thread() {
            override fun run() {
                try {
                    val dataPath = Utils.resourcePath("song_list.json")
                    val text = File(dataPath).readText()
                    val songList = parseSongList(text)
                    callback(songList, null)
                } catch (e: Exception) {
                    Utils.log("加载伴奏列表出错啦！${e.localizedMessage}")
                    callback(null, e.localizedMessage)
                }
            }
        }.start()
    }

    /**
     * 加载伴奏信息
     */
    fun loadSongInfo(song: SongInfo, singType: Int, callback: OnSongInfoLoadedCallback) {
        Utils.log("加载歌曲信息！${song.songId}")
        HFOpenApi.getInstance()
            .kHQListen(song.songId, "mp3", "320",
                object : DataResponse<HQListen> {
                    override fun onError(exception: BaseException) {
                        Utils.log("加载歌曲信息出错啦！$exception")
                        callback.invoke(0, null, null, null, null, null, null)
                    }

                    override fun onSuccess(data: HQListen, taskId: String) {
                        Utils.log("加载歌曲信息！taskId=$taskId")
                        var musicRes: String? = null
                        var guideRes: String? = null
                        if (data.subVersions.isNullOrEmpty()) {
                            callback.invoke(0, null, null, null, null, null, "获取歌曲信息失败!")
                        }else{
                            for (hq in data.subVersions!!) {
                                if (hq.versionName != null) {
                                    Utils.log("加载歌曲信息！${data.musicId} -- ${hq.versionName} -- start:${hq.startTime} --- end:${hq.endTime}")
                                    if (singType == 0) {
                                        if (hq.versionName!!.startsWith("伴奏_320_mp3")) {
                                            musicRes = hq.path
                                            song.songSubVersion = SongInfoVer(0,0,0)

                                        } else if (hq.versionName!!.startsWith("左右声道_320_mp3")) {
                                            guideRes = hq.path
                                        }
                                    } else if (singType == 2) {
                                        if (hq.versionName!!.startsWith("60s伴奏_320_mp3")) {
                                            musicRes = hq.path
                                            song.songSubVersion = SongInfoVer(60,hq.startTime!!.toInt(),hq.endTime!!.toInt())
                                        } else if (hq.versionName!!.startsWith("60s左右声道_320_mp3")) {
                                            guideRes = hq.path
                                        }
                                    } else if (singType == 1) {
                                        if (hq.versionName!!.startsWith("30s伴奏_320_mp3")) {
                                            song.songSubVersion = SongInfoVer(30,hq.startTime!!.toInt(),hq.endTime!!.toInt())
                                            musicRes = hq.path
                                        } else if (hq.versionName!!.startsWith("30s左右声道_320_mp3")) {
                                            guideRes = hq.path
                                        }
                                    }
                                }
                            }
                            var type = 0
                            if (!data.songKind.isNullOrEmpty()) {
                                try {
                                    type = data.songKind!!.toInt()
                                }catch (e:Exception){

                                }
                            }
                            callback.invoke(type, data.staticLyricUrl, data.dynamicLyricUrl, data.scoreUrl, musicRes, guideRes, null)
                        }

                    }

                })
    }

    /**
     * 简单的Http请求
     */
    private fun httpGet(url: String): String? {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        try {
            val response = client.newCall(request).execute()
            return response.body?.string()
        } catch (e: IOException) {
            Utils.log("http err:${e.localizedMessage}")
        }
        return null
    }

    /**
     * 增加作品
     */
    fun addWork(songInfo: SongInfo) {
        if (workArray.size > 0) {
            workArray.add(0, songInfo)
        } else {
            workArray.add(songInfo)
        }
        saveWorks()
    }

    /**
     * 删除作品
     */
    fun delWork(songInfo: SongInfo, callback: OnDoneCallback) {
        object : Thread() {
            override fun run() {
                try {
                    Utils.log("delWork 1  ${workArray.size}")
                    if (workArray.isNotEmpty()) {
                        for (i in workArray.indices) {
                            val work = workArray[i]
                            if (work.songId == songInfo.songId && work.recordPath == songInfo.recordPath) {
                                //增加SDK中删除录音缓存的处理
                                Utils.delRecordFile(songInfo.recordPath)
                                workArray.remove(work)
                                break
                            }
                        }
                        Utils.log("delWork  2  ${workArray.size}")
                        saveWorks()
                    }

                } catch (e: Exception) {
                    Utils.log("删除作品出错啦！${e.localizedMessage}")
                }
                callback()
            }
        }.start()
    }


    /**
     * 保存所有作品记录
     */
    private fun saveWorks() {
        val data = myWorks()
        try {
            val file = File(worksPath)
            file.deleteOnExit()
            file.createNewFile()
            val json = gson().toJson(data, WorksData::class.javaObjectType)
            file.writeText(json)
        } catch (e: Exception) {
            Utils.log("保存作品出错啦！${e.localizedMessage}")
        }
    }

    /**
     * 作品记录转换为作品列表
     */
    private fun myWorks(): WorksData {
        val data = WorksData()
        if (workArray.isNotEmpty()) {
            for (work in workArray) {
                val item = work.createWorkItem()
                data.add(item)
            }
        }
        return data
    }

    /**
     * 读取作品记录文件
     */
    private fun loadWorks() {
        try {
            val file = File(worksPath)
            if (file.exists()) {
                val text = file.readText()
                val data = gson().fromJson(text, WorksData::class.javaObjectType)
                for (item in data) {
                    val songInfo = SongInfo(item)
                    setSongPath(songInfo)
                    workArray.add(songInfo)
                }
            }
        } catch (e: Exception) {
            Utils.log("loadWorks err:$e")
        }

    }

    //
    //解析伴奏列表
    private fun parseSongList(text: String?): ArrayList<SongInfo>? {
        text?.let {
            val songList: ArrayList<SongInfo> = arrayListOf()
            try {

                val data = gson().fromJson(text, SongData::class.javaObjectType)
                for (item in data) {
                    val songInfo = SongInfo(item)
                    setSongPath(songInfo)
                    songList.add(songInfo)
                }
            } catch (e: Exception) {
                Utils.log("解析伴奏列表出错啦！${e.localizedMessage}")
            }
            return songList
        }
        return null
    }

    /**
     * 根据歌曲类型设置不同的资源文件绝对路径
     */
    fun setSongPath(songInfo: SongInfo) {
        when (songInfo.type) {
            2 -> {
                songInfo.scorePath = Utils.resourcePath("${songInfo.songId}.lrc")
                songInfo.musicPath = Utils.resourcePath("${songInfo.songId}.mp3")
            }
            else -> {
                if (songInfo.songSubVersion.partDuration == 0) {
                    songInfo.scorePath = Utils.cachePath("${songInfo.songId}_score.lrc")
                    songInfo.staticLyricPath = Utils.cachePath("${songInfo.songId}_static.lrc")
                    songInfo.dynamicLyricPath = Utils.cachePath("${songInfo.songId}_dynamic.lrc")
                    songInfo.musicPath = Utils.cachePath("${songInfo.songId}.mp3")
                    songInfo.guidePath = Utils.cachePath("LeadSing_${songInfo.songId}.mp3")
                } else {
                    songInfo.scorePath = Utils.cachePath("${songInfo.songId}_${songInfo.songSubVersion.partDuration}_score.lrc")
                    songInfo.staticLyricPath = Utils.cachePath("${songInfo.songId}_${songInfo.songSubVersion.partDuration}_static.lrc")
                    songInfo.dynamicLyricPath = Utils.cachePath("${songInfo.songId}_${songInfo.songSubVersion.partDuration}_dynamic.lrc")
                    songInfo.musicPath = Utils.cachePath("${songInfo.songId}_${songInfo.songSubVersion.partDuration}.mp3")
                    songInfo.guidePath = Utils.cachePath("LeadSing_${songInfo.songId}_${songInfo.songSubVersion.partDuration}.mp3")
                }
            }
        }
    }

    /**
     * 重新当前下载任务
     */
    fun resetDownloadTask() {
        currTask?.release()
        currTask = null
    }

    /**
     * 下载歌曲
     */
    fun downloadSong(song: SongInfo, callback: OnSongDownloadProgress) {
        try {
            var isNew = true
            if (currTask != null) {
                var tempSong: SongInfo? = currTask!!.song
                var taskCallback = currTask!!.callback
                android.os.Handler(Looper.getMainLooper()).post {
                    tempSong?.songId?.let { taskCallback?.invoke(it, DownloadStatus.PAUSED, -1) }
                    taskCallback = null
                    tempSong = null
                }
                if (song.songId == currTask!!.song.songId && song.songSubVersion.partDuration == currTask!!.song.songSubVersion.partDuration) {
                    isNew = false
                }
                currTask?.cancel()
                resetDownloadTask()
            }
            if (isNew) {
                downloadNewSong(song, callback)
            }
        } catch (e: Exception) {
            Utils.log("下载伴奏资源出错啦！${e.localizedMessage}")
        }
    }

    private fun downloadNewSong(song: SongInfo, callback: OnSongDownloadProgress) {
        Utils.log("downloadNewSong")
        currTask = DownloadSong(song, callback)
        currTask!!.start()
    }
}



typealias OnLoadedCallback = (ArrayList<SongInfo>?, String?) -> Unit
typealias OnDoneCallback = () -> Unit
typealias OnSongInfoLoadedCallback = (Int, String?, String?, String?,String?, String?, String?) -> Unit

