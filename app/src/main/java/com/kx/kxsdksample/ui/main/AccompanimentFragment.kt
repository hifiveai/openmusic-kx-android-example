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
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.FragmentAccompanimentBinding
import com.kx.kxsdksample.model.SongInfo
import com.kx.kxsdksample.service.DataManager
import com.kx.kxsdksample.tools.Utils
import com.kx.kxsdksample.ui.KtvActivity
import com.kx.kxsdksample.ui.views.LoadingView

/**
 * A placeholder fragment containing a simple view.
 */
class AccompanimentFragment : Fragment() ,ISongItemClickedListener{

    private var _binding: FragmentAccompanimentBinding? = null

    private val binding get() = _binding!!

    private var loadingDialog:LoadingView? = null
    private var adapter:AccompanimentAdapter? = null
    private var listView:ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAccompanimentBinding.inflate(inflater, container, false)
        val root = binding.root
        listView = binding.accompanimentList
        listView!!.onItemClickListener = null
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = LoadingView(requireContext())
        loadingDialog?.show()
        DataManager.manager.loadAccompanimentList { arrayList, err ->
            Handler(Looper.getMainLooper()).post{
                if (arrayList != null) {
                    adapter = AccompanimentAdapter(requireContext(), R.layout.accompaniment_item, arrayList, this)
                    adapter?.activity = this.activity
                    listView!!.adapter = adapter
                }else{
                    var errInfo = "加载数据异常！"
                    if (err != null) {
                        errInfo = err
                    }
                    alert(errInfo)
                }
                loadingDialog?.close()
            }
        }
    }


    private fun alert(text:String){
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(text)
        builder.create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog?.close()
        adapter?.release()
        _binding = null
    }

    override fun onSongItemClicked(song: SongInfo) {
//        Utils.toast(requireContext(), "演唱《${song.name}》!")
        val activity = requireActivity()
        val intent = Intent(activity, KtvActivity::class.java)
        song.recordPath = Utils.recordFileName(song.songId)
        intent.putExtra("ktv_song",song)
        activity.startActivity(intent)
        activity.finish()
    }
}