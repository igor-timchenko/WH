package ru.contlog.mobile.helper.utils

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class CustomLinearLayoutManager(context: Context) : LinearLayoutManager(context, VERTICAL, false) {
    var isScrollEnabled = true
    override fun canScrollVertically(): Boolean = isScrollEnabled && super.canScrollVertically()
}