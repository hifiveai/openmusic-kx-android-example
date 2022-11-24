package com.kx.kxsdksample.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import com.kx.kxsdksample.model.SongInfo
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.kx.kxsdksample.R
import com.kx.kxsdksample.tools.Utils


class WorksAdapter(context: Context, private val resourceId: Int, private var objects: ArrayList<SongInfo>, private val listener: IWorkAdapterListener) :
    ArrayAdapter<SongInfo>(context, resourceId, objects) {

    override fun getCount(): Int {
        return objects.size
    }

    override fun getItem(position: Int): SongInfo? {
        if (position >= 0 && position < objects.size){
            return objects[position]
        }
        return null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view:View = convertView ?: LayoutInflater.from(context).inflate(resourceId, parent, false)

        val holder:ItemViewHolder = if (convertView == null) {
           ItemViewHolder(view)
        }else{
            view.tag as ItemViewHolder
        }
        val index = position % 10
        holder.imgIv.setImageBitmap(Utils.getImageFromAssetsFile(context, index))
        val songInfo = getItem(position)
        if (songInfo != null) {
            holder.songNameTv.text = songInfo.name
            holder.singerTv.text = songInfo.singer
            val score = songInfo.score.toInt()
            if (songInfo.type > 0 && score > 0) {
                holder.scoreTv.text = "(${score}分)"
                holder.scoreTv.visibility = View.VISIBLE
            }else{
                holder.scoreTv.visibility = View.GONE
            }

            holder.btn.setOnClickListener {
                listener.onSongItemClicked(songInfo)
            }
            holder.delBtn.setOnClickListener {
                listener.onDelWork(songInfo)
            }
        }
        return view
    }


    internal class ItemViewHolder(view: View){
        var imgIv: ImageView = view.findViewById(R.id.work_item_icon)
        var songNameTv: TextView = view.findViewById(R.id.work_item_song_tv)
        var singerTv: TextView = view.findViewById(R.id.work_item_singer_tv)
        var scoreTv: TextView = view.findViewById(R.id.work_item_score_tv)
        var btn:AppCompatButton = view.findViewById(R.id.work_item_playback_btn)
        var delBtn: AppCompatButton = view.findViewById(R.id.work_item_del_btn)
        init {
            view.tag = this
        }
    }

    public interface IWorkAdapterListener :ISongItemClickedListener{
        fun onDelWork(songInfo: SongInfo)
    }
}
