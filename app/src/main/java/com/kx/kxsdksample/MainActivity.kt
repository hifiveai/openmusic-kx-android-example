package com.kx.kxsdksample

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.kx.kxsdksample.databinding.ActivityMainBinding
import com.kx.kxsdksample.tools.Utils
import com.kx.kxsdksample.ui.main.SectionsPagerAdapter


/**
 * 伴奏/作品列表 Activity
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val toWork = intent.getBooleanExtra("to_work_tab", false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.title.visibility = View.GONE

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)
        if (toWork) {
            viewPager.currentItem = 1
        }
    }

    override fun onBack() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("确定要离开K歌精灵吗？")
        builder.setNegativeButton("确定") { _, _ ->
            finish()
            App.exitApp()
        }
        builder.setPositiveButton("取消") { _, _ ->
        }
        val dialog = builder.create()
        dialog.show()
        //系统对话框的按钮文本颜色默认为白色，需要重新设置
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Utils.getColor(this, R.color.light_gray))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Utils.getColor(this, R.color.black))
    }
}

