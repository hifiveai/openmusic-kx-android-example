package com.kx.kxsdksample.ui.views


import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.kx.kxsdksample.R
import com.kx.kxsdksample.databinding.DialogBottomMenusBinding


class BottomMensDialog(context: Context, private val listener:BottomMenusListener) : Dialog(context, R.style.BottomMenusDialog) {

    private val binding: DialogBottomMenusBinding = DialogBottomMenusBinding.inflate(LayoutInflater.from(context))

    private var isFirst = true

    init {

        setContentView(binding.root)
        setCancelable(false)
        binding.noSaveBackBtn.setOnClickListener {
            listener.onClickedBottomNoSave()
        }
        val override = binding.overrideBtn
        override.setOnClickListener {
            listener.onClickedBottomMenu(true)
        }
        val saveNew = binding.saveNewBtn
        saveNew.setOnClickListener {
            listener.onClickedBottomMenu(false)
        }
        val cancel = binding.cancelBtn
        cancel.setOnClickListener {
            dismiss()
        }
    }
}

interface BottomMenusListener {
    public fun onClickedBottomMenu(isOverride:Boolean)
    public fun onClickedBottomNoSave()
}