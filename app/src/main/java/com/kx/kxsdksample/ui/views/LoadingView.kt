package com.kx.kxsdksample.ui.views

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.LoadingViewBinding

class LoadingView(context: Context) : Dialog(context, R.style.CustomProgressDialog) {
    private val binding = LoadingViewBinding.inflate(LayoutInflater.from(context))
    init {
        setContentView(binding.root)
        setCancelable(false)
    }

    fun close(){
        dismiss()
    }
}