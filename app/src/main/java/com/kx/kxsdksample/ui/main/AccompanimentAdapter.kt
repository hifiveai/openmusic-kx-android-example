package com.kx.kxsdksample.ui.main

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import com.kx.kxsdksample.model.SongInfo
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.FragmentActivity
import com.khoiron.actionsheets.ActionSheet
import com.khoiron.actionsheets.callback.ActionSheetCallBack
import com.kx.kxsdksample.R
import com.kx.kxsdksample.service.DataManager
import com.kx.kxsdksample.tools.DownloadStatus
import com.kx.kxsdksample.tools.Utils


class AccompanimentAdapter(
    context: Context,
    private val resourceId: Int,
    private var list: ArrayList<SongInfo>, private val listener: ISongItemClickedListener
) :
    ArrayAdapter<SongInfo>(context, resourceId, list) {

    var activity:FragmentActivity? = null

    private val mHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    private val viewList: ArrayList<View> by lazy {
        ArrayList()
    }

    fun release() {
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(position: Int): SongInfo? {
        if (position < list.size) {
            return list[position]
        }
        return null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View =
            convertView ?: LayoutInflater.from(context).inflate(resourceId, parent, false)

        val holder: ItemViewHolder = if (convertView == null) {
            ItemViewHolder(context, view)
        } else {
            view.tag as ItemViewHolder
        }

        val index = position % 10
        holder.imgIv.setImageBitmap(Utils.getImageFromAssetsFile(context, index))
        val songInfo = getItem(position)
        if (songInfo != null) {
            holder.songNameTv.text = songInfo.name
            holder.singerTv.text = songInfo.singer
            holder.typeTv.text = Utils.getSongTypeName(songInfo.type)

            view.setTag(R.id.accompaniment_list, songInfo.songId)
            viewList.add(view)

            //按钮统一用红色
            holder.updateBtnCompat(false)
            holder.btn.setOnClickListener {
                if (songInfo.type == 2) {
                    //精确评分
                    if (listener != null) {
                        listener.onSongItemClicked(songInfo)
                    }
                } else {
                    onItemEvent(songInfo, holder)
                }
            }
        }
        return view
    }

    private fun onItemEvent(song:SongInfo,holder: ItemViewHolder) {
        if (activity != null) {
            val dataList by lazy { ArrayList<String>() }
            dataList.add("唱整首")
            dataList.add("30s片段")
            dataList.add("60s片段")
            val actionSheet = ActionSheet(activity!!,dataList)
            actionSheet.setCancelTitle("取消")
            actionSheet.setTitle("选择演唱方式")
            actionSheet.create(object:ActionSheetCallBack {
                override fun data(data: String, position: Int) {
                    Utils.log("选择演唱:$data")
                    if (data == dataList[0]) {
                        goKTV(song,holder,0)
                    } else if (data == dataList[1]) {
                        goKTV(song,holder,1)
                    } else if (data == dataList[2]) {
                        goKTV(song,holder,2)
                    }
                }
            })
        }
    }

    private fun goKTV(songInfo:SongInfo,holder: ItemViewHolder,singType:Int) {

        Utils.showProgressDialogOfBase(activity,null)
        Utils.log("${songInfo.name}, type:${songInfo.type}, singType:$singType, song info not found...")
        DataManager.manager.loadSongInfo(songInfo,singType) { type, staticLrcUrl, dynamicLrcUrl, scoreUrl, musicUrl, guideUrl, err ->
            mHandler.post {
                if (err != null) {
                    holder.updateBtnCompat(true)
                    Utils.closeProgressDialogOfBase()

                    Utils.toast(context, "获取《${songInfo.name}》信息失败!")

                } else {
                    songInfo.type = type
                    songInfo.scoreUrl = scoreUrl ?: ""
                    songInfo.staticLyricUrl = staticLrcUrl ?: ""
                    songInfo.dynamicLyricUrl = dynamicLrcUrl ?: ""
                    songInfo.musicUrl = musicUrl ?: ""
                    songInfo.guideUrl = guideUrl ?: ""
                    DataManager.manager.setSongPath(songInfo) //歌曲路径设置

                    var success = true
                    var errStr = ""
                    //获取到的歌曲信息中至少要有歌词、伴奏文件，否则将停止演唱并弹出错误信息
                    if (songInfo.notFoundLyric) {
                        success = false
                        errStr = "无歌词文件！"
                    }
                    if (songInfo.musicUrl.isEmpty()) {
                        success = false
                        errStr = if (errStr.isNotEmpty()) {
                            "无伴奏、歌词文件！"
                        } else {
                            "无伴奏文件！"
                        }
                    }
                    if (success) {
                        if (canSing(songInfo)) {
                            Utils.log("${songInfo.name}, type:${songInfo.type}, loadSongInfo done. to sing...")
                            holder.updateBtnCompat(false)

                            Utils.closeProgressDialogOfBase()

                            if (listener != null) {
                                listener.onSongItemClicked(songInfo)
                            }
                        } else {
                            Utils.log("${songInfo.name}, type:${songInfo.type}, loadSongInfo done. to download resources...")
                            holder.setProgress(1)
                            downloadSong(songInfo)
                        }
                    } else {
                        mHandler.post {
                            Utils.closeProgressDialogOfBase()
                            Utils.toast(context, errStr)
                        }
                    }
                }
            }
        }
    }

    //检查歌曲是否可以演唱(如果本地存在静态歌词或者动态歌词或者评分文件，即可演唱)
    private fun canSing(song: SongInfo): Boolean {
        return when (song.type) {
            2 -> {
                true
            }
            else -> {
                Utils.cached(song)
            }
        }
    }

    private fun downloadSong(song: SongInfo) {

        DataManager.manager.downloadSong(song) { songId, status, progress ->
            var holderView: View? = null
            for (view in viewList) {
                if (view.getTag(R.id.accompaniment_list) == songId) {
                    holderView = view
                    break
                }
            }
            if (status == DownloadStatus.ERROR) {
                if (holderView != null) {
                    val holder = holderView.tag as ItemViewHolder
                    holder.updateBtnCompat(true)
                }
                viewList.remove(holderView)
                DataManager.manager.resetDownloadTask()
                Utils.toast(context, "歌曲资源下载失败！请稍后再试...")
            } else {
            if (holderView != null) {
                val holder = holderView.tag as ItemViewHolder
                    if (status == DownloadStatus.PAUSED || progress < 0) {
                        holder.updateBtnCompat(true)
                    } else if (progress >= 100) {
                        viewList.remove(holderView)
                        DataManager.manager.resetDownloadTask()
                        holder.updateBtnCompat(false)
                        if (listener != null) {
                            listener.onSongItemClicked(song)
                        }
                    } else if (progress >= 0) {
                        holder.setProgress(progress)
                    }
                }
            }
        }
    }


    internal class ItemViewHolder(private val context: Context, view: View) {
        val imgIv: ImageView = view.findViewById(R.id.acc_item_icon)
        val songNameTv: TextView = view.findViewById(R.id.acc_item_song_tv)
        val singerTv: TextView = view.findViewById(R.id.acc_item_singer_tv)
        val typeTv: TextView = view.findViewById(R.id.acc_item_type_tv)
        val btn: AppCompatButton = view.findViewById(R.id.item_progress_btn)
        private val progress: ProgressBar = view.findViewById(R.id.item_progress)
        private val bgView: View = view.findViewById(R.id.item_btn)


        private val whiteBg: Drawable? = Utils.getDrawable(context, R.drawable.rect_normal)
        private val focusBg: Drawable? = Utils.getDrawable(context, R.drawable.rect_pressed)

        init {
            view.tag = this
        }

        fun updateBtnCompat(defStatus: Boolean) {
            this.btn.text = "演唱"
            this.progress.visibility = View.GONE
            this.progress.progress = 0
            if (defStatus) {
                this.btn.text = "演唱"
                this.btn.setTextColor(Utils.focusColor)
                bgView.background = whiteBg
            } else {
                this.btn.setTextColor(Utils.white)
                bgView.background = focusBg!!
            }
        }

        fun setProgress(value: Int) {
            if (value >= 0) {
                if (value >= 100) {
                    updateBtnCompat(false)
                } else {
                    if (value > this.progress.progress) {
                        this.progress.progress = value
                        this.btn.text = "$value%"
                        this.btn.setTextColor(Color.BLACK)
                        this.progress.visibility = View.VISIBLE
                    }
                }
            } else {
                updateBtnCompat(true)
            }
        }
    }
}
