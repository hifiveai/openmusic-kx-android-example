package com.kx.kxsdksample.tools

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.kx.kxsdksample.R
import com.kx.kxsdksample.model.SongInfo
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


/**
 * 工具类
 */
object Utils {

    private var root: String = ""
    private var cachePath: String = ""
    private var resourcePath: String = ""

    private var _focus_c: Int = 0
    private var _def_c: Int = 0

    /**
     *  回放的录音类型：
     *  0 -- 正常的录音文件
     *  1 --  实时人声
     *  2 -- 实时伴奏+人声
     */
    var recType = 0

    /**
     *  回放录音类型选择开关：
     *  true -- 回放页中可以切换回放录音的类型
     */
    val recTypeOn = true

    /**
     * 白色
     */
    val white: Int
        get() {
            return _def_c
        }
    /**
     * APP主色
     */
    val focusColor: Int
        get() {
            return _focus_c
        }

    fun init(context: Context) {
        _focus_c = getColor(context, R.color.tab_focus)
        _def_c = getColor(context, R.color.white)
        if (root.isEmpty()) {
            root = context.filesDir.toString()
            resourcePath = "$root/resource/"
            cachePath = "$root/download/"
            makeDir(resourcePath)
            makeDir(cachePath)
        }
        saveAssets(context)
    }

    /**
     * 获取application中的meta-data数据
     */
    fun getMetaDataForApp(context: Context,key:String) : String {
        var tmpValue:String? = null
        try {
            tmpValue = context.packageManager.getApplicationInfo(context.packageName,PackageManager.GET_META_DATA).metaData.getString(key)
        } catch (e:Exception) {
            e.printStackTrace()
        }
        return tmpValue ?: ""
    }

    /**
     * 文件是否存在
     */
    fun cached(filePath: String): Boolean {
        val file = File(filePath)
        try {
            if (file.exists()) {
                return true
            }
        } catch (e: IOException) {
        }
        return false
    }

    fun  cached(song:SongInfo) : Boolean {
        return cached(song.scorePath) || cached(song.dynamicLyricPath) || cached(song.staticLyricPath)
    }

    /**
     * 将Assets中的内容保存到手机本地
     */
    private fun saveAssets(context: Context) {
        val list = context.assets.list("")
        if (list != null) {
            for (assetsName in list) {
                if (assetsName.endsWith(".mp3") || assetsName.endsWith(".lrc") || assetsName.endsWith(
                        ".json"
                    )
                ) {
                    val path = resourcePath(assetsName)
                    saveAssetsFile(context, assetsName, path)
                }
            }
        }
    }
    /**
     * 将Assets中文件保存到手机本地
     */
    private fun saveAssetsFile(context: Context, assetsName: String, filePath: String) {
        val file = File(filePath)
        try {
            file.deleteOnExit()
            val input = context.assets.open(assetsName)
            val output = FileOutputStream(filePath)
            val buffer = ByteArray(8192)
            var count = 0
            while (input.read(buffer).also { count = it } > 0) {
                output.write(buffer, 0, count)
            }
            output.close()
            input.close()
        } catch (e: IOException) {
            log("saveAssetsFile err:$e")
        }
    }
    /**
     * 创建文件夹
     */
    private fun makeDir(path: String): Boolean {
        val file = File(path)
        try {
            if (!file.exists()) {
                file.mkdirs()
                return true
            }
        } catch (e: IOException) {
        }
        return false
    }

    /**
     * 获取下载(缓存)文件绝对路径
     */
    fun cachePath(fileName: String): String {
        return "$cachePath$fileName"
    }

    /**
     * 获取资源文件的绝对路径
     */
    fun resourcePath(name: String): String {
        return "$resourcePath$name"
    }

    /**
     * 获取录音文件名
     */
    fun recordFileName(songId: String): String {
        val timeStr = currTimeStr()
        return "${songId}_${timeStr}.wav"
    }

    fun pcmTestFileName(songId:String, isMix:Boolean):String{
        return if (isMix) {
            "${cachePath}${songId}_pcm_rec_mix.wav"
        }else{
            "${cachePath}${songId}_pcm_rec.wav"
        }
    }

    /**
     * 删除录音文件
     */
    fun delRecordFile(fileName: String) {
        val path = cachePath(fileName)
        //删除录音文件
        delFile(path)
    }

    fun delFile(path:String){
        val file = File(path)
        file.deleteOnExit()
    }

    private fun currTimeStr(): String {
        val currTime = Date()
        return SimpleDateFormat("yyyyMMdd_hhmmss", Locale.getDefault()).format(currTime)
    }

    /**
     * 时间转换为String(00:00)，time单位:毫秒
     */
    fun formatPlayerTime(time: Long): String {
        val date = Date(time)
        return SimpleDateFormat("mm:ss", Locale.getDefault()).format(date)
    }

    fun getDrawable(context: Context, @DrawableRes id: Int): Drawable? {
        return ContextCompat.getDrawable(context, id)
    }

    fun getColor(context: Context, @ColorRes id: Int): Int {
        return ContextCompat.getColor(context, id)
    }


    /**
     * 从Assets/image中读取图片
     * @param fileName 文件名
     */
    fun getImageFromAssetsFile(context: Context, index: Int): Bitmap? {
        var image: Bitmap? = null
        val am = context.resources.assets
        try {
            val input = am.open("$index.jpg")
            image = BitmapFactory.decodeStream(input)
            input.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return image
    }
    /**
     * 从Assets文件中读取数据
     */
    fun getAssetsData(assets: AssetManager, assetsName: String): ByteArray? {
        var res: ByteArray? = null
        try {
            val input = assets.open(assetsName)
            res = input.readBytes()
            input.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return res
    }

    /**
     * 获取歌曲类型说明
     */
    fun getSongTypeName(type: Int): String {
        return when (type) {

            2 -> "(逐字评分)"

            1 -> "(单句评分)"

            else -> "(无评分)"
        }
    }

    /**
     * 获取耳机插入状态
     */
    fun headsetPlugin(context: Context): Boolean {
        val manager: AudioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices: Array<AudioDeviceInfo> =
            manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            val deviceType = device.type
            if (deviceType == AudioDeviceInfo.TYPE_WIRED_HEADSET
                || deviceType == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            ) {
                return true
            }
        }
        return false
    }

    fun log(log: String) {
        var msg = log
        ////信息太长时,分段打印
        while (msg.length > 3072) {
            Log.d("kxsdksample->>>", msg.substring(0, 3072))
            msg = msg.substring(3072)
        }
        //剩余部分
        Log.d("kxsdksample->>>", msg)
    }

    fun toast(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    private var progressDialog: CustomProgressDialog? = null

    /**
     * 显示加载框，设置了取消监听器此框可以中断，不设置则无法中断
     *
     * @Title: showProgressDialogOfBase
     * @Description:
     * @param @param activity
     * @param @param listener
     * @return void
     * @throws
     */
    fun showProgressDialogOfBase(
        activity: Activity?,
        listener: CustomProgressDialog.OnCancelProgressDiaListener?
    ) {
        if (activity != null) {
            progressDialog = CustomProgressDialog(activity)
            progressDialog!!.setOnCancelProgressDiaListener(listener)
            progressDialog!!.show()
        }
    }

    /**
     * 关闭加载框
     *
     * @Title: closeProgressDialogOfBase
     * @Description:
     * @param
     * @return void
     * @throws
     */
    fun closeProgressDialogOfBase() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

}