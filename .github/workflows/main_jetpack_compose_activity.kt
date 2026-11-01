package com.tikhikhabar.studio

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFDC2626),
                    secondary = Color(0xFFFACC15),
                    background = Color(0xFF09090B),
                    surface = Color(0xFF18181B)
                )
            ) {
                TikhiKhabarStudioScreen()
            }
        }
    }
}

@Composable
fun TikhiKhabarStudioScreen() {
    val context = LocalContext.current
    val exporter = remember { VideoOverlayExporter(context) }

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var customLogoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    var isShorts by remember { mutableStateOf(true) }
    var showLogo by remember { mutableStateOf(true) }
    var logoTopRight by remember { mutableStateOf(true) }
    var showDomainStrip by remember { mutableStateOf(true) }

    var showBreakingBanner by remember { mutableStateOf(true) }
    var bannerHeader by remember { mutableStateOf("ब्रेकिंग न्यूज़") }
    var headlineText by remember {
        mutableStateOf("हाईवे पर लगा भीषण जाम, घंटों फंसे रहे सैकड़ों यात्री")
    }
    var tickerText by remember {
        mutableStateOf("ताज़ा ख़बरों के लिए लॉगिन करें tikhikhabar.com • अपने क्षेत्र की हर बड़ी ख़बर सबसे पहले")
    }

    var showFlashCard by remember { mutableStateOf(true) }
    var showShortsTopPill by remember { mutableStateOf(false) }
    var locationTag by remember { mutableStateOf("बड़ी ख़बर • LIVE") }
    var flashText by remember {
        mutableStateOf("प्रशासन की टीम मौके पर मौजूद, यातायात सुचारू करने का प्रयास जारी")
    }
    var shortsBottomOffset by remember { mutableFloatStateOf(16f) }

    var isExporting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedVideoUri = uri
    }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                customLogoBitmap = BitmapFactory.decodeStream(stream)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
            .padding(14.dp)
    ) {
        // 1. Top Studio Header + 16:9 / Shorts Switch + Export Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDC2626)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ती", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("TikhiKhabar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("BROADCAST STUDIO", color = Color(0xFFFACC15), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Format Toggle Pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF18181B))
                    .border(1.dp, Color(0xFF27272A), CircleShape)
                    .padding(3.dp)
            ) {
                FormatChip(label = "16:9", selected = !isShorts) { isShorts = false }
                FormatChip(label = "Shorts", selected = isShorts) { isShorts = true }
            }

            Button(
                onClick = {
                    val uri = selectedVideoUri
                    if (uri == null) {
                        statusMessage = "Please select a video from your phone first!"
                        return@Button
                    }
                    isExporting = true
                    statusMessage = "Rendering ${if (isShorts) "9:16 Shorts" else "16:9"} MP4..."
                    exporter.exportVideo(
                        inputVideoUri = uri,
                        config = OverlayConfig(
                            isShorts = isShorts,
                            showLogo = showLogo,
                            logoTopRight = logoTopRight,
                            showDomainStrip = showDomainStrip,
                            customLogoBitmap = customLogoBitmap,
                            showBreakingBanner = showBreakingBanner,
                            bannerHeader = bannerHeader,
                            headlineText = headlineText,
                            tickerText = tickerText,
                            showFlashCard = showFlashCard,
                            showShortsTopPill = showShortsTopPill,
                            locationTag = locationTag,
                            flashText = flashText,
                            shortsBottomOffsetPercent = shortsBottomOffset
                        ),
                        onSuccess = { savedPath ->
                            isExporting = false
                            statusMessage = "Saved to Gallery: $savedPath"
                        },
                        onError = { err ->
                            isExporting = false
                            statusMessage = "Export Error: $err"
                        }
                    )
                },
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    if (isExporting) "Saving..." else "Export MP4",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status banner if active
        statusMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF27272A))
                    .padding(10.dp)
            ) {
                Text(msg, color = Color(0xFFFACC15), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. Live Interactive Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF121216))
                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val previewModifier = if (isShorts) {
                Modifier
                    .fillMaxHeight(0.92f)
                    .aspectRatio(9f / 16f)
            } else {
                Modifier
                    .fillMaxWidth(0.94f)
                    .aspectRatio(16f / 9f)
            }

            Box(
                modifier = previewModifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1B24))
                    .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(12.dp))
            ) {
                // Center Video Picker Prompt
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(onClick = { videoPicker.launch("video/*") }) {
                        Text(
                            if (selectedVideoUri != null) "Video Loaded ✓ (Tap to Change)" else "Pick Video (16:9 or Shorts)",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                // Corner Watermark Preview
                if (showLogo) {
                    Box(
                        modifier = Modifier
                            .align(if (logoTopRight) Alignment.TopEnd else Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(if (isShorts) 50 else 6))
                            .background(if (isShorts) Color(0xAA000000) else Color(0xFFDC2626))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isShorts) "• तीखी ख़बर" else "तीखी ख़बर | tikhikhabar.com",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp
                        )
                    }
                }

                // Breaking News Lower-Third Preview
                if (showBreakingBanner) {
                    if (!isShorts) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFFFACC15))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(bannerHeader, color = Color(0xFF450A0A), fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF991B1B))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    headlineText,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF09090B))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    tickerText,
                                    color = Color(0xFFFACC15),
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        // Uncluttered Floating Pill for 9:16 Shorts
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 8.dp, vertical = 24.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xCC09090B))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    bannerHeader,
                                    color = Color(0xFFF87171),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp
                                )
                                if (showDomainStrip) {
                                    Text("tikhikhabar.com", color = Color(0xFFFACC15), fontSize = 8.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                headlineText,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Scrollable Form Controls
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = bannerHeader,
                onValueChange = { bannerHeader = it },
                label = { Text("Ribbon Header (e.g. ब्रेकिंग न्यूज़ / EXCLUSIVE)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = headlineText,
                onValueChange = { headlineText = it },
                label = { Text("Main Breaking Headline") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            if (!isShorts) {
                OutlinedTextField(
                    value = tickerText,
                    onValueChange = { tickerText = it },
                    label = { Text("16:9 Bottom Scrolling Ticker Text") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = flashText,
                    onValueChange = { flashText = it },
                    label = { Text("16:9 Upper Flash News Context Card") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            } else {
                Text(
                    "Shorts Safe-Zone Offset: ${shortsBottomOffset.toInt()}% from bottom",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Slider(
                    value = shortsBottomOffset,
                    onValueChange = { shortsBottomOffset = it },
                    valueRange = 5f..28f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { logoTopRight = !logoTopRight },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (logoTopRight) "Logo: Top-Right" else "Logo: Top-Left", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { logoPicker.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (customLogoBitmap != null) "Custom PNG ✓" else "Upload PNG Logo", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun FormatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color(0xFFDC2626) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}