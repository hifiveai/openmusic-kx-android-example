package com.kx.kxsdksample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.kx.kxsdksample.databinding.ActivityLaunchBinding
import com.kx.kxsdksample.service.DataManager
import com.kx.kxsdksample.tools.ScreenUtil
import com.kx.kxsdksample.tools.Utils
import com.kx.kxsdksample.ui.views.LoadingView

/**
 * 启动 Activity
 */
class LaunchActivity : BaseActivity() {


    private lateinit var loadingView: LoadingView
    private lateinit var binding: ActivityLaunchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenUtil.init(resources)
        binding = ActivityLaunchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadingView = LoadingView(this)
    }

    override fun onBack() {
    }

    override fun onResume() {
        super.onResume()
        //App权限申请
        requestPermissions()
    }

    private val REQUEST_P_CODE = 101
    private val REQUEST_CODE_SETTING = 102

    //权限：读/写文件、网络、录音
    private val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )

    private fun initApp() {
        val _context = this
        loadingView.show()
        object : Thread() {
            override fun run() {
                try {
                    Utils.init(_context)
                    DataManager.manager.initManager(_context)
                    runOnUiThread {
                        toMainActivity()
                    }
                } catch (e: Exception) {
                    Utils.log("初始化失败 e:$e")
                    runOnUiThread {
                        permissionAlert("初始化失败!")
                    }
                }
            }
        }.start()
    }

    //进入列表页
    private fun toMainActivity() {
        loadingView.close()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    //App权限申请
    private fun requestPermissions() {
        val checkList = checkPermission(permissions)
        if (checkList.isNotEmpty()) {
            requestPermission(checkList.toTypedArray())
        } else {
            Utils.log("requestPermissions")
            initApp()
        }
    }

    //检查权限
    private fun checkPermission(checkList: Array<String>): List<String> {
        val list: MutableList<String> = ArrayList()
        for (s in checkList) {
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat
                    .checkSelfPermission(this, s)
            ) {
                list.add(s)
            }
        }
        return list
    }

    //请求权限
    private fun requestPermission(list: Array<String>) {
        ActivityCompat.requestPermissions(this, list, REQUEST_P_CODE)
    }


    /**
     * 跳转应用设置界面进行授权
     */
    private fun goToSetting() {
        loadingView.close()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, REQUEST_CODE_SETTING)
    }

    //权限设置回调
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode === REQUEST_P_CODE) {

            var count = 0
            for (i in permissions.indices) {
                Utils.log(permissions[i])
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    //用户已授权
                    count += 1
                } else {
                    //判断用户是否勾选了不再询问
                    if (!shouldShowRequestPermissionRationale(permissions[i])) {
                        //跳转到应用设置界面授权
                        goToSetting()
                        break
                    }
                }
            }
            if (count == permissions.size) {
                initApp()
            }
        }
    }

    //应用设置界面回调
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === REQUEST_CODE_SETTING) {
            //从设置界面回来，是不知道用户是否有授权的，所以需要重新再判断
            val checkList: List<String> = checkPermission(permissions)
            if (checkList.isEmpty()) {
                Utils.log("onActivityResult")
                //已授权
                initApp()
            } else {
                val info = "您未能取得 读写文件/录音 权限, 请申请权限后再使用！"
                permissionAlert(info)
            }
        }
    }

    private fun permissionAlert(text: String) {
        loadingView.close()
        val builder = AlertDialog.Builder(this)
        builder.setMessage(text)
        builder.setPositiveButton("确定") { _, _ ->
            finish()
        }
        var dialog = builder.create()
        dialog.show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Utils.getColor(this, R.color.black))
    }
}