package com.kx.kxsdksample

import com.hero.kxktvsdk.KXKTVSDKManager

import android.app.Application
import com.hfopen.sdk.common.HFOpenCallback
import com.hfopen.sdk.manager.HFOpenApi
import com.hfopen.sdk.rx.BaseException
import com.kx.kxsdksample.tools.Utils
import com.tencent.bugly.crashreport.CrashReport

class App :Application(), HFOpenCallback {

    override fun onCreate() {
        super.onCreate()

        CrashReport.initCrashReport(applicationContext, "47819eaa67", true);

        //打开控制台log输出
        KXKTVSDKManager.getInstance(baseContext)!!.setDebugMode(true)
        //初始化K歌引擎
        var appId = Utils.getMetaDataForApp(this,"HIFIVE_APPID")
        Utils.log("appId:$appId")
        KXKTVSDKManager.getInstance(baseContext)!!.initEngine(appId,null)
        //初始化歌曲信息获取服务
        HFOpenApi.registerApp(this, "1")
        HFOpenApi.setVersion("V4.1.4")
    }

    //HFOpenCallback
    override fun onError(exception: BaseException) {
        Utils.log("HFOpenCallback exception:$exception")
    }

    override fun onSuccess() {
        Utils.log("HFOpenCallback success")
    }


    companion object {
        fun  exitApp(){
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        }
    }
}