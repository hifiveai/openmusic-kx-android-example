package com.kx.kxsdksample.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.hero.kxktvsdk.KXKTVSDKManager
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.FragmentWorksBinding
import com.kx.kxsdksample.model.SongInfo
import com.kx.kxsdksample.service.DataManager
import com.kx.kxsdksample.tools.Utils
import com.kx.kxsdksample.ui.PlaybackActivity
import com.kx.kxsdksample.ui.views.LoadingView

/**
 * A placeholder fragment containing a simple view.
 */
class WorksFragment : Fragment(), WorksAdapter.IWorkAdapterListener{

    private var _binding: FragmentWorksBinding? = null

    private val binding get() = _binding!!

    private var loadingView:LoadingView? = null
    private var listView:ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentWorksBinding.inflate(inflater, container, false)
        val root = binding.root
        listView = binding.workList
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingView = LoadingView(requireContext())
        loadingView?.show()
        listView!!.adapter = WorksAdapter(requireContext(), R.layout.work_item, DataManager.manager.workArray, this)
        loadingView?.close()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingView?.close()
        _binding = null
    }

    override fun onDelWork(songInfo: SongInfo) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("提示")
        builder.setMessage("确定要删除此作品吗？")
        builder.setNegativeButton("确定") { _,_ ->
            loadingView?.show()
            DataManager.manager.delWork(songInfo) {
                requireActivity().runOnUiThread{
                    try {
                        listView!!.adapter = WorksAdapter(requireContext(), R.layout.work_item, DataManager.manager.workArray, this)
                        loadingView?.close()
                    }catch (e:Exception){
                        Utils.log("refresh works err:$e")
                    }
                }
            }
        }
        builder.setPositiveButton("取消"){ _,_ ->
        }

        val dialog = builder.create()
        dialog.show()
        //系统对话框的按钮文本颜色默认为白色，需要重新设置为黑色
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Utils.getColor(requireContext(), R.color.black))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Utils.getColor(requireContext(), R.color.black))

    }

    override fun onSongItemClicked(song: SongInfo) {
        val activity = requireActivity()
        val intent = Intent(activity, PlaybackActivity::class.java)
        intent.putExtra("ktv_song",song)
        activity.startActivity(intent)
    }
}