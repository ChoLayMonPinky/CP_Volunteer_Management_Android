package dev.tintwym.ui.checkin

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.tintwym.ui.components.PrimaryAction
import dev.tintwym.ui.components.SecondaryAction
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekPrimarySoft
import dev.tintwym.ui.theme.SleekSurface
import dev.tintwym.ui.theme.SleekTextPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import dev.tintwym.ui.theme.Space
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QrScannerSheet(
  onDismiss: () -> Unit,
  onCode: (String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
  val context = LocalContext.current
  var invalidHint by remember { mutableStateOf<String?>(null) }
  var requestedOnce by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = SleekSurface,
  ) {
    Column(
      Modifier
        .fillMaxWidth()
        .padding(horizontal = Space.screen)
        .padding(bottom = 28.dp),
    ) {
      Box(Modifier.fillMaxWidth()) {
        Text(
          "Scan check-in QR",
          style = MaterialTheme.typography.titleLarge,
          color = SleekTextPrimary,
          modifier = Modifier.align(Alignment.CenterStart).padding(end = 48.dp),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
          Icon(Icons.Outlined.Close, contentDescription = "Close")
        }
      }
      Text(
        "Point your camera at the event QR code from your leader.",
        style = MaterialTheme.typography.bodyMedium,
        color = SleekTextSecondary,
      )
      Spacer(Modifier.height(16.dp))

      when {
        cameraPermission.status.isGranted -> {
          QrCameraPreview(
            modifier = Modifier
              .fillMaxWidth()
              .height(360.dp)
              .clip(RoundedCornerShape(Space.radiusLg)),
            onCode = { raw ->
              val code = extractCheckInCode(raw)
              if (code.isNotBlank()) {
                onCode(code)
                onDismiss()
              } else {
                invalidHint = "That QR isn’t a check-in code. Ask your leader for the event QR."
              }
            },
          )
          Spacer(Modifier.height(12.dp))
          Text(
            invalidHint ?: "Looking for a QR…",
            style = MaterialTheme.typography.labelLarge,
            color = if (invalidHint != null) MaterialTheme.colorScheme.error else SleekPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        cameraPermission.status.shouldShowRationale -> {
          Text(
            "Camera access is needed to scan the check-in QR.",
            color = SleekTextSecondary,
          )
          Spacer(Modifier.height(12.dp))
          PrimaryAction("Allow camera") { cameraPermission.launchPermissionRequest() }
          Spacer(Modifier.height(8.dp))
          SecondaryAction("Not now", onClick = onDismiss)
        }
        !requestedOnce -> {
          LaunchedEffect(Unit) {
            requestedOnce = true
            cameraPermission.launchPermissionRequest()
          }
          Text("Requesting camera…", color = SleekTextSecondary)
          Spacer(Modifier.height(12.dp))
          SecondaryAction("Cancel", onClick = onDismiss)
        }
        else -> {
          Text(
            "Camera permission is off. Enable it in Settings, or enter the code manually.",
            color = SleekTextSecondary,
          )
          Spacer(Modifier.height(12.dp))
          PrimaryAction("Open settings") {
            val intent = Intent(
              Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
              Uri.fromParts("package", context.packageName, null),
            )
            context.startActivity(intent)
          }
          Spacer(Modifier.height(8.dp))
          SecondaryAction("Not now", onClick = onDismiss)
        }
      }
    }
  }
}

@androidx.camera.core.ExperimentalGetImage
@Composable
private fun QrCameraPreview(
  modifier: Modifier = Modifier,
  onCode: (String) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
  val handled = remember { AtomicBoolean(false) }
  val disposed = remember { AtomicBoolean(false) }
  val providerRef = remember { AtomicReference<ProcessCameraProvider?>(null) }
  val scanner = remember {
    BarcodeScanning.getClient(
      BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
    )
  }

  DisposableEffect(Unit) {
    onDispose {
      disposed.set(true)
      handled.set(true)
      runCatching { providerRef.get()?.unbindAll() }
      analysisExecutor.shutdown()
      runCatching {
        analysisExecutor.awaitTermination(800, java.util.concurrent.TimeUnit.MILLISECONDS)
      }
      runCatching { scanner.close() }
    }
  }

  Box(modifier.background(Color.Black)) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { ctx ->
        val previewView = PreviewView(ctx).apply {
          scaleType = PreviewView.ScaleType.FILL_CENTER
          implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener(
          {
            if (disposed.get()) return@addListener
            val cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@addListener
            if (disposed.get()) return@addListener
            providerRef.set(cameraProvider)
            val preview = Preview.Builder().build().also {
              it.surfaceProvider = previewView.surfaceProvider
            }
            val resolutionSelector = ResolutionSelector.Builder()
              .setResolutionStrategy(
                ResolutionStrategy(
                  Size(1280, 720),
                  ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
              )
              .build()
            val analysis = ImageAnalysis.Builder()
              .setResolutionSelector(resolutionSelector)
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build()
            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
              if (disposed.get() || handled.get()) {
                imageProxy.close()
                return@setAnalyzer
              }
              val mediaImage = imageProxy.image
              if (mediaImage == null) {
                imageProxy.close()
                return@setAnalyzer
              }
              val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
              scanner.process(image)
                .addOnSuccessListener { barcodes ->
                  if (disposed.get()) return@addOnSuccessListener
                  val raw = barcodes.firstOrNull()?.rawValue
                  if (!raw.isNullOrBlank() && handled.compareAndSet(false, true)) {
                    onCode(raw)
                  }
                }
                .addOnFailureListener {
                  // Ignore closed-client / disposed races during teardown.
                }
                .addOnCompleteListener { imageProxy.close() }
            }
            runCatching {
              if (disposed.get()) return@runCatching
              cameraProvider.unbindAll()
              cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
              )
            }
          },
          ContextCompat.getMainExecutor(ctx),
        )
        previewView
      },
    )
    Box(
      Modifier
        .align(Alignment.Center)
        .size(220.dp)
        .border(2.dp, SleekPrimarySoft, RoundedCornerShape(20.dp)),
    )
  }
}

/** Accept plain codes or URLs / JSON-ish payloads that contain the code. */
fun extractCheckInCode(raw: String): String {
  val trimmed = raw.trim()
  if (trimmed.isBlank()) return ""
  Regex("""[?&]code=([^&]+)""", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.getOrNull(1)
    ?.let { return it.trim() }
  Regex("""check[-_]?in[/:=]([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE).find(trimmed)
    ?.groupValues?.getOrNull(1)?.let { return it }
  Regex(""""code"\s*:\s*"([^"]+)"""").find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
  if (trimmed.matches(Regex("""[A-Za-z0-9_-]{4,32}"""))) return trimmed
  return ""
}
