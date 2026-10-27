package com.spworld.gallery

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.media.MediaMetadataRetriever
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var status: TextView
    private val items = mutableListOf<MediaItem>()
    private var manualMode = false
    private var manualItemIndex = -1
    private val pickCode = 101
    private val prefs by lazy { getSharedPreferences("sp_world", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        buildUi()
        restoreItems()
        refreshMarkers()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 8)
        }
        val title = TextView(this).apply {
            text = "📍 SP World — ફોટો અને વીડિયો મેપ"
            textSize = 20f
            setPadding(4, 8, 4, 8)
        }
        root.addView(title)
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(Button(this).apply {
            text = "મીડિયા પસંદ કરો"
            setOnClickListener { pickMedia() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        buttons.addView(Button(this).apply {
            text = "મેન્યુઅલ લોકેશન"
            setOnClickListener { chooseManualItem() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(buttons)
        status = TextView(this).apply {
            text = "ફોટા/વીડિયો પસંદ કરો. માર્કર પર ટેપ કરતાં સ્લાઇડશો શરૂ થશે."
            textSize = 13f
            setPadding(4, 4, 4, 8)
        }
        root.addView(status)
        map = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.5)
            controller.setCenter(GeoPoint(22.3, 71.2))
        }
        map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (manualMode && manualItemIndex in items.indices) {
                    val item = items[manualItemIndex]
                    item.lat = p.latitude
                    item.lon = p.longitude
                    item.place = "મેન્યુઅલ સ્થળ (${String.format("%.4f", p.latitude)}, ${String.format("%.4f", p.longitude)})"
                    manualMode = false
                    manualItemIndex = -1
                    saveItems()
                    refreshMarkers()
                    status.text = "લોકેશન સાચવ્યું. સ્થળનું નામ બદલવા માટે પછીથી સંપાદન સુવિધા ઉમેરો."
                    return true
                }
                return false
            }
            override fun longPressHelper(p: GeoPoint) = false
        }))
        root.addView(map, LinearLayout.LayoutParams(-1, 0, 1f))
        val attribution = TextView(this).apply {
            text = "© OpenStreetMap contributors"
            textSize = 11f
            setPadding(4, 4, 4, 0)
        }
        root.addView(attribution)
        setContentView(root)
    }

    private fun pickMedia() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, pickCode)
    }

    @Deprecated("Use document picker result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickCode || resultCode != Activity.RESULT_OK || data == null) return
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip -> for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri) }
        data.data?.let { if (uris.isEmpty()) uris.add(it) }
        for (uri in uris) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            if (items.any { it.uri == uri }) continue
            val type = contentResolver.getType(uri) ?: ""
            val video = type.startsWith("video/")
            if (!video && !type.startsWith("image/")) continue
            val item = MediaItem(uri, video)
            readLocation(item)
            items.add(item)
        }
        saveItems()
        refreshMarkers()
        status.text = "${items.size} ફાઇલ પસંદ થઈ. લોકેશન ન હોય તેવી ફાઇલ માટે મેન્યુઅલ લોકેશન પસંદ કરો."
    }

    private fun readLocation(item: MediaItem) {
        try {
            if (item.isVideo) {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, item.uri)
                val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                retriever.release()
                if (!location.isNullOrBlank()) {
                    val parsed = parseVideoLocation(location)
                    if (parsed != null) {
                        item.lat = parsed.first
                        item.lon = parsed.second
                        item.place = "GPS വീഡിയോ"
                    }
                }
            } else {
                val stream: InputStream? = contentResolver.openInputStream(item.uri)
                if (stream != null) {
                    val exif = ExifInterface(stream)
                    val out = FloatArray(2)
                    if (exif.getLatLong(out)) {
                        item.lat = out[0].toDouble()
                        item.lon = out[1].toDouble()
                        item.place = "GPS ફોટો"
                    }
                    stream.close()
                }
            }
        } catch (_: Exception) { }
    }

    private fun parseVideoLocation(s: String): Pair<Double, Double>? {
        // Common ISO 6709 format, e.g. +22.3000+070.8000/
        val regex = Regex("([+-][0-9]{2,3}\\\\.[0-9]+)([+-][0-9]{3}\\\\.[0-9]+)")
        val m = regex.find(s) ?: return null
        return try { m.groupValues[1].toDouble() to m.groupValues[2].toDouble() } catch (_: Exception) { null }
    }

    private fun chooseManualItem() {
        if (items.isEmpty()) {
            Toast.makeText(this, "પહેલાં ફોટા અથવા વીડિયો પસંદ કરો", Toast.LENGTH_SHORT).show()
            pickMedia()
            return
        }
        val labels = items.mapIndexed { i, item -> "${i + 1}. ${if (item.isVideo) "વીડિયો" else "ફોટો"} — ${displayName(item.uri)}" }.toTypedArray()
        AlertDialogBuilder.show(this, "કઈ ફાઇલને લોકેશન આપવું?", labels) { index ->
            manualItemIndex = index
            manualMode = true
            status.text = "હવે મેપ પર જ્યાં મૂકવું હોય ત્યાં ટેપ કરો."
            Toast.makeText(this, "મેપ પર ઇચ્છિત જગ્યા પર ટેપ કરો", Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshMarkers() {
        map.overlays.removeAll { it is Marker }
        val groups = items.filter { it.hasLocation() }.groupBy {
            "${"%.5f".format(java.util.Locale.US, it.lat)}:${"%.5f".format(java.util.Locale.US, it.lon)}"
        }
        groups.values.forEach { group ->
            val first = group.first()
            val marker = Marker(map)
            marker.position = GeoPoint(first.lat!!, first.lon!!)
            marker.title = "${first.place} (${group.size})"
            marker.snippet = "ટેપ કરો — સ્લાઇડશો શરૂ થશે"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.setOnMarkerClickListener { _, _ ->
                val locItems = group.toTypedArray()
                val intent = Intent(this, SlideshowActivity::class.java)
                intent.putExtra("uris", locItems.map { it.uri.toString() }.toTypedArray())
                intent.putExtra("videos", locItems.map { it.isVideo }.toBooleanArray())
                intent.putExtra("place", first.place)
                startActivity(intent)
                true
            }
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    private fun saveItems() {
        val arr = JSONArray()
        items.forEach {
            val o = JSONObject()
            o.put("uri", it.uri.toString())
            o.put("video", it.isVideo)
            o.put("lat", it.lat ?: JSONObject.NULL)
            o.put("lon", it.lon ?: JSONObject.NULL)
            o.put("place", it.place)
            arr.put(o)
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    private fun restoreItems() {
        try {
            val arr = JSONArray(prefs.getString("items", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val uri = Uri.parse(o.getString("uri"))
                val item = MediaItem(uri, o.getBoolean("video"))
                if (!o.isNull("lat")) item.lat = o.getDouble("lat")
                if (!o.isNull("lon")) item.lon = o.getDouble("lon")
                item.place = o.optString("place", "અજ્ઞાત સ્થળ")
                items.add(item)
            }
        } catch (_: Exception) { }
    }

    private fun displayName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "મીડિયા"
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) name = c.getString(0) ?: name
        }
        return name
    }

    override fun onResume() { super.onResume(); if (::map.isInitialized) map.onResume() }
    override fun onPause() { if (::map.isInitialized) map.onPause(); super.onPause() }
}
