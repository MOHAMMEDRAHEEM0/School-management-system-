package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.ModernBlue600
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

/**
 * Production CameraX QR Code Scanner Composable for rapid Student Attendance.
 * Supports:
 * 1. Zero-latency scanning using CameraX ImageAnalysis + ZXing.
 * 2. Automatic Camera permission handling.
 * 3. Fallback manual input for emulators without physical camera access.
 * 4. Scan cooldown to prevent double-marking the same badge.
 */
@Composable
fun CameraXQrScanner(
    onStudentQrDetected: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    lastScannedInfo: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualInput by remember { mutableStateOf("") }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var scanCooldownUntil by remember { mutableStateOf(0L) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("camerax_qr_scanner_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GovNavyPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Attendance Scanner",
                            tint = GovNavyPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CameraX Student QR Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Point camera at Student ID Badge QR Code",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_qr_scanner_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Scanner")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Camera Viewfinder Box or Permission Fallback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

                    DisposableEffect(Unit) {
                        onDispose {
                            cameraExecutor.shutdown()
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val reader = MultiFormatReader().apply {
                                    val hints = mapOf(
                                        DecodeHintType.POSSIBLE_FORMATS to listOf(
                                            BarcodeFormat.QR_CODE,
                                            BarcodeFormat.CODE_128,
                                            BarcodeFormat.EAN_13
                                        )
                                    )
                                    setHints(hints)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val now = System.currentTimeMillis()
                                    if (now > scanCooldownUntil) {
                                        val scannedText = decodeQrFromImageProxy(imageProxy, reader)
                                        if (!scannedText.isNullOrBlank()) {
                                            scanCooldownUntil = now + 2500L // 2.5 second cooldown per scan
                                            lastScannedCode = scannedText
                                            onStudentQrDetected(scannedText)
                                        }
                                    }
                                    imageProxy.close()
                                }

                                runCatching {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Reticle overlay
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .border(2.5.dp, ModernBlue600, RoundedCornerShape(14.dp))
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera permission needed to scan ID cards",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = GovNavyPrimary)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            // Real-time Success Banner
            if (!lastScannedInfo.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = GovGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GovGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lastScannedInfo,
                            color = GovGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual fallback input (Useful for emulators or testing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text("Or Enter Student ID / Roll (e.g. ADM-2026-001)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qr_manual_input_field")
                )
                Button(
                    onClick = {
                        if (manualInput.isNotBlank()) {
                            onStudentQrDetected(manualInput.trim())
                            manualInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GovNavyPrimary),
                    modifier = Modifier.testTag("submit_manual_qr_button")
                ) {
                    Text("Mark")
                }
            }
        }
    }
}

private fun decodeQrFromImageProxy(imageProxy: ImageProxy, reader: MultiFormatReader): String? {
    return try {
        val plane = imageProxy.planes.getOrNull(0) ?: return null
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val width = imageProxy.width
        val height = imageProxy.height
        val source = PlanarYUVLuminanceSource(
            bytes, width, height, 0, 0, width, height, false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = reader.decode(binaryBitmap)
        reader.reset()
        result.text
    } catch (_: Exception) {
        null
    }
}
