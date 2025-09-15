// Fajl: screens/CustomRiderMarker.kt
package com.example.cyclelink.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.example.cyclelink.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.ui.geometry.Offset
@Composable
fun CustomRiderMarker(
    context: Context,
    position: LatLng,
    title: String,
    imageUrl: String
) {
    var bitmapDescriptor by remember { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(imageUrl) {
        bitmapDescriptor = createCustomMarkerBitmap(context, imageUrl)
    }

    bitmapDescriptor?.let {
        Marker(
            state = MarkerState(position = position),
            title = title,
            icon = it,
            anchor = Offset(0.5f, 1.0f)
        )
    }
}

private suspend fun createCustomMarkerBitmap(context: Context, imageUrl: String): BitmapDescriptor? {
    val markerBackgroundDrawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_background)
        ?: return null

    val markerWidth = 120
    val markerHeight = 120
    val backgroundBitmap = markerBackgroundDrawable.toBitmap(markerWidth, markerHeight)

    val loader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(if (imageUrl.isBlank()) R.drawable.ic_launcher_foreground else imageUrl)
        .size(80, 80)
        .allowHardware(false)
        .build()

    val result = loader.execute(request).drawable
    val profileBitmap = result?.toBitmapSafe(80, 80) ?: context.getDrawable(R.drawable.ic_launcher_foreground)?.toBitmap(80, 80)

    val roundedProfile = profileBitmap!!.getRoundedCroppedBitmap()

    val finalBitmap = Bitmap.createBitmap(markerWidth, markerHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(finalBitmap)
    canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
    val left = (markerWidth - roundedProfile.width) / 2f
    val top = (markerHeight - roundedProfile.height) / 2f - 10
    canvas.drawBitmap(roundedProfile, left, top, null)

    return BitmapDescriptorFactory.fromBitmap(finalBitmap)
}

fun android.graphics.drawable.Drawable.toBitmapSafe(width: Int, height: Int): Bitmap? {
    return when (this) {
        is android.graphics.drawable.BitmapDrawable -> bitmap
        else -> {
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            setBounds(0, 0, width, height)
            draw(canvas)
            bmp
        }
    }
}

fun Bitmap.getRoundedCroppedBitmap(): Bitmap {
    val width = this.width
    val height = this.height
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val color = -0xbdbdbe
    val paint = Paint()
    val rect = Rect(0, 0, width, height)
    val rectF = RectF(rect)
    val roundPx = (width / 2).toFloat()
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color
    canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, rect, rect, paint)
    return output
}