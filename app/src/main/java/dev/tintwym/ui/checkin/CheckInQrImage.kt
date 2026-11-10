package dev.tintwym.ui.checkin

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dev.tintwym.ui.theme.SleekSurface
import dev.tintwym.ui.theme.SleekTextSecondary
import dev.tintwym.ui.theme.Space

@Composable
fun CheckInQrImage(
  code: String,
  size: Dp = 180.dp,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val px = with(density) { size.roundToPx().coerceAtLeast(256) }
  val bitmap = remember(code, px) {
    if (code.isBlank()) null
    else runCatching { encodeQr(code, px) }.getOrNull()
  }
  Box(
    modifier
      .size(size)
      .clip(RoundedCornerShape(Space.radiusMd))
      .background(SleekSurface),
    contentAlignment = Alignment.Center,
  ) {
    when {
      bitmap != null -> Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Check-in QR code",
        modifier = Modifier.size(size),
      )
      else -> Text(
        if (code.isBlank()) "No code" else "Couldn’t render QR",
        style = MaterialTheme.typography.labelMedium,
        color = SleekTextSecondary,
        textAlign = TextAlign.Center,
      )
    }
  }
}

private fun encodeQr(content: String, px: Int): Bitmap {
  val hints = mapOf(
    EncodeHintType.MARGIN to 1,
    EncodeHintType.CHARACTER_SET to "UTF-8",
  )
  val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, px, px, hints)
  val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
  val black = android.graphics.Color.BLACK
  val white = android.graphics.Color.WHITE
  for (x in 0 until px) {
    for (y in 0 until px) {
      bmp.setPixel(x, y, if (matrix[x, y]) black else white)
    }
  }
  return bmp
}
