package com.kx.kxsdksample.tools

import android.app.AlertDialog
import android.content.Context
import android.widget.TextView
import com.kx.kxsdksample.ui.views.LoadingRotateLinearLayout
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import com.kx.kxsdksample.R

/**
 * 可以被取消的加载进度对话框
 *
 * @author jason.ye
 */
class CustomProgressDialog : AlertDialog {
    private var info: String? = null
    private var infoView: TextView? = null
    private var listener: OnCancelProgressDiaListener? = null
    private var loadingView: LoadingRotateLinearLayout? = null

    constructor(context: Context) : super(context, R.style.AlertDialogIOSStyle) {}
    constructor(
        context: Context,
        listener: OnCancelProgressDiaListener?
    ) : super(context, R.style.AlertDialogIOSStyle) {
        this.listener = listener
    }

    constructor(
        context: Context,
        listener: OnCancelProgressDiaListener?, info: String?
    ) : super(context, R.style.AlertDialogIOSStyle) {
        this.listener = listener
        this.info = info
    }

    fun setOnCancelProgressDiaListener(
        listener: OnCancelProgressDiaListener?
    ) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_progress_dialog)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        loadingView = findViewById<View>(R.id.loadingView) as LoadingRotateLinearLayout
        if (info != null && info!!.trim { it <= ' ' }.isNotEmpty()) {
            infoView = findViewById<View>(R.id.infoView) as TextView
            infoView!!.text = info
        }
    }

    fun setInfo(info: String?) {
        this.info = info
        if (info != null && info.trim { it <= ' ' }.isNotEmpty() && infoView != null) {
            infoView!!.text = info
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (listener != null) {
                listener!!.onCancelProgressDia()
                dismiss()
            }
        }
        return true
    }

    override fun show() {
        // TODO Auto-generated method stub
        super.show()
        if (loadingView != null) {
            loadingView!!.addAnimationImageView()
        }
    }

    /**
     * 关闭进度对话框回调
     *
     * @author jason.ye
     */
    interface OnCancelProgressDiaListener {
        /**
         * 关闭对话框
         */
        fun onCancelProgressDia()
    }
}