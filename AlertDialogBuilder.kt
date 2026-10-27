package com.spworld.gallery

import android.content.Context
import android.app.AlertDialog

object AlertDialogBuilder {
    fun show(context: Context, title: String, items: Array<String>, onPick: (Int) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(items) { _, which -> onPick(which) }
            .setNegativeButton("રદ કરો", null)
            .show()
    }
}
