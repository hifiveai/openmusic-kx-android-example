package com.kx.kxsdksample.tools

import android.os.Handler
import android.os.Looper
import com.kx.kxsdksample.model.SongInfo

enum class DownloadOrder {
    None,
    Accomp,
    Guide,
    StaticLrc,
    DynamicLrc,
    ScoreLrc,
    Completed
}

/**
 * 下载歌曲管理
 */
class DownloadSong(val song: SongInfo, var callback: OnSongDownloadProgress?) {

    var status: DownloadStatus = DownloadStatus.START
    private var downloader: Downloader? = null
    private val mHandler = Handler(Looper.getMainLooper())
    private var order: DownloadOrder = DownloadOrder.None
    private var pOffset = 49

    /**
     * 开始下载
     */
    fun start() {
        Utils.log("DownloadSong start... song:${song.name}")
        order = DownloadOrder.Guide
        pOffset = 49
        status = DownloadStatus.DOWNLOADING
        if (song.guideUrl.isEmpty()) {
            order = DownloadOrder.Accomp
            pOffset = 0
        }
        doDownload()
    }

    /**
     * 取消下载
     */
    fun cancel() {
        status = DownloadStatus.PAUSED
        downloader?.cancel()
        downloader = null
    }

    /**
     * 清理资源
     */
    fun release() {
        mHandler.removeCallbacksAndMessages(null)
        callback = null
        downloader?.cancel()
        downloader = null
    }

    private fun doDownload() {
        when (order) {
            DownloadOrder.Guide -> {
                //下载 导唱文件
                downloadFile(song.guideUrl, song.guidePath) { p, err ->
                    mHandler.post {
                        if (err != null) {
                            callback?.invoke(song.songId, DownloadStatus.ERROR, 0)
                        } else {
                            val temp = p * 0.49f
                            callback?.invoke(song.songId, status, temp.toInt())
                            if (p >= 100f && status == DownloadStatus.DOWNLOADING) {
                                Utils.log("《${song.name}》的导唱文件下载完成")
                                order = DownloadOrder.Accomp
                                doDownload()
                            }
                        }
                    }
                }
            }
            DownloadOrder.Accomp -> {
                //下载 伴奏文件
                downloadFile(song.musicUrl, song.musicPath) { p, err ->
                    mHandler.post {
                        if (err != null) {
                            callback?.invoke(song.songId, DownloadStatus.ERROR, 0)
                        } else {

                            val temp = if (pOffset > 0) {
                                p * 0.49f + pOffset
                            } else {
                                p * 0.98f
                            }
                            callback?.invoke(song.songId, status, temp.toInt())
                            if (p >= 100f && status == DownloadStatus.DOWNLOADING) {
                                Utils.log("《${song.name}》的伴奏文件下载完成")
                                order = if (song.staticLyricUrl.isEmpty()) {
                                    if (song.dynamicLyricUrl.isEmpty()) {
                                        DownloadOrder.ScoreLrc
                                    } else {
                                        DownloadOrder.DynamicLrc
                                    }
                                } else {
                                    DownloadOrder.StaticLrc
                                }
                                doDownload()
                            }
                        }
                    }
                }
            }
            DownloadOrder.StaticLrc -> {
                //下载 静态歌词文件
                downloadFile(song.staticLyricUrl, song.staticLyricPath) { p, err ->
                    mHandler.post {
                        if (err != null) {
                            callback?.invoke(song.songId, DownloadStatus.ERROR, 0)
                        } else {
                            callback?.invoke(song.songId, status, 98)
                            if (p >= 100f && status == DownloadStatus.DOWNLOADING) {
                                Utils.log("《${song.name}》的静态歌词下载完成")
                                order = if (song.dynamicLyricUrl.isEmpty()) {
                                    DownloadOrder.ScoreLrc
                                } else {
                                    DownloadOrder.DynamicLrc
                                }
                                doDownload()
                            }
                        }
                    }
                }
            }
            DownloadOrder.DynamicLrc -> {
                //下载 动态歌词文件
                downloadFile(song.dynamicLyricUrl, song.dynamicLyricPath) { p, err ->
                    mHandler.post {
                        if (err != null) {
                            callback?.invoke(song.songId, DownloadStatus.ERROR, 0)
                        } else {
                            callback?.invoke(song.songId, status, 98)
                            if (p >= 100f && status == DownloadStatus.DOWNLOADING) {
                                Utils.log("《${song.name}》的动态歌词下载完成")
                                order = if (song.scoreUrl.isEmpty()) {
                                    DownloadOrder.Completed
                                } else {
                                    DownloadOrder.ScoreLrc
                                }
                                doDownload()
                            }
                        }
                    }
                }
            }
            DownloadOrder.ScoreLrc -> {
                //下载 评分歌词文件
                downloadFile(song.scoreUrl, song.scorePath) { p, err ->
                    mHandler.post {
                        if (err != null) {
                            callback?.invoke(song.songId, DownloadStatus.ERROR, 0)
                        } else {
                            callback?.invoke(song.songId, status, 99)
                            if (p >= 100f && status == DownloadStatus.DOWNLOADING) {
                                Utils.log("《${song.name}》的评分歌词下载完成")
                                order = DownloadOrder.Completed
                                doDownload()
                            }
                        }
                    }
                }
            }
            DownloadOrder.Completed -> {
                Utils.log("《${song.name}》的歌曲文件下载完成")
                //完成歌曲资源文件下载
                callback?.invoke(song.songId, status, 100)
            }
        }
    }

    //下载文件
    private fun downloadFile(url: String, path: String, callback: OnDownloadProgress) {
        Utils.log("downloadFile ...url=${url}, file:${path}")
        if (status == DownloadStatus.DOWNLOADING) {
            downloader = Downloader(url, path, callback)
            downloader!!.start()
        }
    }
}

typealias OnSongDownloadProgress = (String, DownloadStatus, Int) -> Unit