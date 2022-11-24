package com.kx.kxsdksample.tools


import android.os.Handler
import android.os.Looper
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 文件下载器
 * （非断点下载）
 */
class Downloader(
    private val taskUrl: String,
    private val filePath: String,
    private var callback: OnDownloadProgress?
) {

    private val TIME_OUT: Long = 5 * 60
    private var status = DownloadStatus.START
    private var call: Call? = null
    private var running = false

    /**
     * 取消下载
     */
    fun cancel() {
        running = false
        status = DownloadStatus.PAUSED
        removeCaches()
        callback = null
    }

    /**
     * 开始下载
     */
    fun start() {
        removeCaches()
        status = DownloadStatus.DOWNLOADING
        running = true
        doDownload()
    }

    /**
     * 清除下载文件
     */
    private fun removeCaches() {
        call?.cancel()
        call = null
        try {
            val file = File(filePath)
            file.deleteOnExit()
            val tempFile = File("${filePath}_temp")
            tempFile.deleteOnExit()
        } catch (e: Exception) {
        }
    }

    private fun httpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .callTimeout(TIME_OUT, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 下载
     */
    private fun doDownload() {
        val request = Request.Builder().url(taskUrl).build()
        val client = httpClient()
        val httpCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val errorMsg = if (e.message == null) {
                    "unKnow error"
                } else {
                    e.message
                }
                Utils.log("task download onFailure:$errorMsg")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful && response.code == 200) {
                    val body = response.body
                    if (body == null) {
                        Utils.log("task download onResponse:${response.message}")
                        return
                    }
                    val contentSize = body.contentLength()
                    val inputStream = body.byteStream()
                    var downloadSize: Long = 0
                    var readLen = 0
                    var fos: FileOutputStream? = null
                    val contentLen = 1024 * 8
                    try {
                        val tempFile = File("${filePath}_temp")
                        tempFile.deleteOnExit()
                        tempFile.createNewFile()
                        fos = FileOutputStream(tempFile)
                        val bytes = ByteArray(contentLen)
                        Handler(Looper.getMainLooper()).post {
                            Utils.log("$filePath, cs:$contentSize, bs:$contentLen")
                        }
                        while (running && inputStream.read(bytes, 0, contentLen).also { readLen = it } != -1) {
                            fos.write(bytes, 0, readLen)
                            downloadSize += readLen
                            if (downloadSize < contentSize) {
                                callback?.invoke(downloadSize * 100f / contentSize, null)
                            }
                        }
                        tempFile.renameTo(File(filePath))
                        callback?.invoke(100f, null)
                    } catch (e: Exception) {
                        Utils.log("task calculate err:${e.localizedMessage}")
                        callback?.invoke(0f, e.localizedMessage)
                    } finally {
                        try {
                            inputStream.close()
                            fos?.close()
                        } catch (e: Exception) {
                            Utils.log("task calculate finally err:${e.localizedMessage}")
                        }
                    }
                }else{
                    callback?.invoke(0f, "Response error:${response.message} code:${response.code}")
                    Utils.log("task download onResponse error:${response.message} code:${response.code}")
                }
            }

        }
        call = client.newCall(request)
        call!!.enqueue(httpCallback)
    }
}


enum class DownloadStatus {
    START, DOWNLOADING, PAUSED, ERROR
}


typealias OnDownloadProgress = (Float, String?) -> Unit
