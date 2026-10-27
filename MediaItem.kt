package com.spworld.gallery

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    var lat: Double? = null,
    var lon: Double? = null,
    var place: String = "અજ્ઞાત સ્થળ"
) {
    fun hasLocation() = lat != null && lon != null
}
