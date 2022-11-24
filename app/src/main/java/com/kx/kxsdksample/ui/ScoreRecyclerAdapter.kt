package com.kx.kxsdksample.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.widget.ProgressBar
import com.kx.kxsdksample.R


class ScoreRecyclerAdapter(private var context: Context, private var intArray: MutableList<Int>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    fun setList(array: MutableList<Int>) {
        intArray = array
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.score_bar, null, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder = holder as ViewHolder
        viewHolder.progressBar.progress = intArray[position]
    }

    override fun getItemCount(): Int {
        return intArray.size
    }

    internal class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var progressBar: ProgressBar = view.findViewById(R.id.score_bar_progress)

        init {
            progressBar.progress = 0
        }
    }
}