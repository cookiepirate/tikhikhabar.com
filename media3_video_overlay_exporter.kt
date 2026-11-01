package com.tikhikhabar.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import java.io.File

/**
 * Configuration data class for TikhiKhabar Studio video overlays.
 */
data class OverlayConfig(
    val isShorts: Boolean = true,               // false = 16:9 Full Broadcast, true = 9:16 Uncluttered Shorts
    val showLogo: Boolean = true,
    val logoTopRight: Boolean = true,
    val showDomainStrip: Boolean = true,
    val customLogoBitmap: Bitmap? = null,
    val showBreakingBanner: Boolean = true,
    val bannerHeader: String = "ब्रेकिंग न्यूज़",
    val headlineText: String = "हाईवे पर लगा भीषण जाम, घंटों फंसे रहे सैकड़ों यात्री",
    val tickerText: String = "ताज़ा ख़बरों के लिए लॉगिन करें tikhikhabar.com • अपने क्षेत्र की हर बड़ी ख़बर सबसे पहले",
    val showFlashCard: Boolean = true,          // Used in 16:9 mode
    val showShortsTopPill: Boolean = false,     // Optional 1-line pill in 9:16 Shorts mode
    val locationTag: String = "बड़ी ख़बर • LIVE",
    val flashText: String = "प्रशासन की टीम मौके पर मौजूद, यातायात सुचारू करने का प्रयास जारी",
    val shortsBottomOffsetPercent: Float = 16f  // Keeps Shorts banner above YouTube/Instagram captions
)

@OptIn(UnstableApi::class)
class VideoOverlayExporter(private val context: Context) {

    /**
     * Hardware-accelerated MP4 exporter that burns the TikhiKhabar logo,
     * 16:9 lower-third + scrolling ticker, or 9:16 uncluttered Shorts pill onto the video.
     */
    fun exportVideo(
        inputVideoUri: Uri,
        config: OverlayConfig,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val outputFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "TikhiKhabar_${if (config.isShorts) "Shorts_9x16" else "Full_16x9"}_${System.currentTimeMillis()}.mp4"
        )

        val canvasOverlay = object : CanvasOverlay(true) {
            private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                val timeSec = presentationTimeUs / 1_000_000f

                // ---------------------------------------------------------
                // 1. TIKHIKHABAR CORNER WATERMARK LOGO
                // ---------------------------------------------------------
                if (config.showLogo) {
                    drawBrandLogo(canvas, w, h, config)
                }

                // ---------------------------------------------------------
                // 2. UPPER FLASH NEWS CARD (16:9) OR COMPACT PILL (9:16)
                // ---------------------------------------------------------
                if (!config.isShorts && config.showFlashCard) {
                    drawLandscapeFlashCard(canvas, w, h, config)
                } else if (config.isShorts && config.showShortsTopPill) {
                    drawShortsCompactTopPill(canvas, w, h, config)
                }

                // ---------------------------------------------------------
                // 3. BREAKING NEWS BANNER (16:9 FULL VS 9:16 UNCLUTTERED)
                // ---------------------------------------------------------
                if (config.showBreakingBanner) {
                    if (!config.isShorts) {
                        drawLandscapeLowerThird(canvas, w, h, timeSec, config)
                    } else {
                        drawShortsFloatingPill(canvas, w, h, config)
                    }
                }
            }

            private fun drawBrandLogo(canvas: Canvas, w: Float, h: Float, cfg: OverlayConfig) {
                if (cfg.customLogoBitmap != null) {
                    val bmp = cfg.customLogoBitmap
                    val targetH = if (cfg.isShorts) 84f else 96f
                    val targetW = (bmp.width.toFloat() / bmp.height.toFloat()) * targetH
                    val left = if (cfg.logoTopRight) w - targetW - 42f else 42f
                    val top = 42f
                    val destRect = RectF(left, top, left + targetW, top + targetH)
                    canvas.drawBitmap(bmp, null, destRect, shapePaint)
                    return
                }

                if (cfg.isShorts) {
                    // Clean, subtle translucent pill watermark for 9:16 Shorts
                    val pillW = 260f
                    val pillH = 64f
                    val left = if (cfg.logoTopRight) w - pillW - 36f else 36f
                    val top = 42f

                    shapePaint.style = Paint.Style.FILL
                    shapePaint.color = Color.argb(175, 10, 10, 14)
                    canvas.drawRoundRect(left, top, left + pillW, top + pillH, 32f, 32f, shapePaint)

                    // Red live dot
                    shapePaint.color = Color.rgb(239, 68, 68)
                    canvas.drawCircle(left + 32f, top + pillH / 2f, 8f, shapePaint)

                    textPaint.textSize = 32f
                    textPaint.color = Color.WHITE
                    canvas.drawText("तीखी", left + 52f, top + 43f, textPaint)

                    textPaint.color = Color.rgb(251, 191, 36)
                    canvas.drawText("ख़बर", left + 138f, top + 43f, textPaint)
                } else {
                    // High-impact TV Channel Bug for 16:9
                    val boxW = 280f
                    val boxH = 68f
                    val left = if (cfg.logoTopRight) w - boxW - 48f else 48f
                    val top = 44f

                    shapePaint.style = Paint.Style.FILL
                    shapePaint.color = Color.rgb(220, 38, 38)
                    canvas.drawRoundRect(left, top, left + boxW / 2f, top + boxH, 10f, 10f, shapePaint)

                    shapePaint.color = Color.rgb(250, 204, 21)
                    canvas.drawRoundRect(left + boxW / 2f - 6f, top, left + boxW, top + boxH, 10f, 10f, shapePaint)

                    textPaint.textSize = 36f
                    textPaint.color = Color.WHITE
                    canvas.drawText("तीखी", left + 24f, top + 47f, textPaint)

                    textPaint.color = Color.rgb(69, 10, 10)
                    canvas.drawText("ख़बर", left + boxW / 2f + 18f, top + 47f, textPaint)

                    if (cfg.showDomainStrip) {
                        shapePaint.color = Color.rgb(9, 9, 11)
                        canvas.drawRect(left, top + boxH, left + boxW, top + boxH + 30f, shapePaint)
                        textPaint.textSize = 20f
                        textPaint.color = Color.rgb(250, 204, 21)
                        canvas.drawText("tikhikhabar.com", left + 54f, top + boxH + 22f, textPaint)
                    }
                }
            }

            private fun drawLandscapeFlashCard(canvas: Canvas, w: Float, h: Float, cfg: OverlayConfig) {
                val left = 48f
                val top = if (!cfg.logoTopRight) 165f else 44f
                val cardW = w * 0.50f
                val cardH = 148f

                shapePaint.color = Color.argb(220, 9, 9, 11)
                canvas.drawRoundRect(left, top, left + cardW, top + cardH, 16f, 16f, shapePaint)

                // Red accent bar
                shapePaint.color = Color.rgb(239, 68, 68)
                canvas.drawRect(left, top, left + 10f, top + cardH, shapePaint)

                // Location tag pill
                if (cfg.locationTag.isNotBlank()) {
                    shapePaint.color = Color.rgb(220, 38, 38)
                    canvas.drawRoundRect(left + 26f, top + 18f, left + 310f, top + 56f, 8f, 8f, shapePaint)
                    textPaint.textSize = 22f
                    textPaint.color = Color.WHITE
                    canvas.drawText(cfg.locationTag, left + 40f, top + 44f, textPaint)
                }

                // Multi-line Flash News text
                textPaint.textSize = 28f
                textPaint.color = Color.WHITE
                drawMultilineText(canvas, cfg.flashText, left + 26f, top + 68f, (cardW - 52f).toInt(), 2)
            }

            private fun drawShortsCompactTopPill(canvas: Canvas, w: Float, h: Float, cfg: OverlayConfig) {
                val pillW = 360f
                val pillH = 62f
                val left = if (cfg.logoTopRight) 36f else w - pillW - 36f
                val top = 42f

                shapePaint.color = Color.argb(175, 10, 10, 14)
                canvas.drawRoundRect(left, top, left + pillW, top + pillH, 31f, 31f, shapePaint)

                textPaint.textSize = 26f
                textPaint.color = Color.WHITE
                val tag = (cfg.locationTag.ifBlank { cfg.flashText }).take(22)
                canvas.drawText("📍 $tag", left + 24f, top + 40f, textPaint)
            }

            private fun drawLandscapeLowerThird(
                canvas: Canvas,
                w: Float,
                h: Float,
                timeSec: Float,
                cfg: OverlayConfig
            ) {
                val marginX = 40f
                val bannerW = w - (marginX * 2f)
                val tickerH = 58f
                val mainH = 128f
                val headerH = 50f
                val bottomMargin = 28f

                val tickerTop = h - bottomMargin - tickerH
                val mainTop = tickerTop - mainH
                val headerTop = mainTop - headerH

                // 1. Header Ribbon ("ब्रेकिंग न्यूज़")
                shapePaint.color = Color.rgb(250, 204, 21)
                canvas.drawRoundRect(marginX, headerTop, marginX + 310f, mainTop + 6f, 10f, 10f, shapePaint)
                textPaint.textSize = 30f
                textPaint.color = Color.rgb(69, 10, 10)
                canvas.drawText(cfg.bannerHeader, marginX + 24f, headerTop + 36f, textPaint)

                // 2. Main Red Headline Plate
                shapePaint.color = Color.rgb(153, 27, 27)
                canvas.drawRect(marginX, mainTop, marginX + bannerW, tickerTop, shapePaint)

                // Top gold border line
                shapePaint.color = Color.rgb(250, 204, 21)
                canvas.drawRect(marginX, mainTop, marginX + bannerW, mainTop + 5f, shapePaint)

                // Main Headline Text
                textPaint.textSize = 42f
                textPaint.color = Color.WHITE
                drawMultilineText(
                    canvas,
                    cfg.headlineText,
                    marginX + 32f,
                    mainTop + 20f,
                    (bannerW - 64f).toInt(),
                    2
                )

                // 3. Bottom Scrolling Ticker Bar
                shapePaint.color = Color.rgb(9, 9, 11)
                canvas.drawRect(marginX, tickerTop, marginX + bannerW, tickerTop + tickerH, shapePaint)

                val repeatedTicker = "${cfg.tickerText}   •   ${cfg.tickerText}   •   "
                textPaint.textSize = 28f
                textPaint.color = Color.rgb(250, 204, 21)
                val textWidth = textPaint.measureText(repeatedTicker).coerceAtLeast(1f)
                val scrollOffset = (timeSec * 220f) % (textWidth / 2f)

                canvas.save()
                canvas.clipRect(marginX + 145f, tickerTop, marginX + bannerW - 12f, tickerTop + tickerH)
                canvas.drawText(repeatedTicker, marginX + 155f - scrollOffset, tickerTop + 39f, textPaint)
                canvas.restore()

                // Red "अपडेट" badge on left of ticker
                shapePaint.color = Color.rgb(220, 38, 38)
                canvas.drawRect(marginX + 10f, tickerTop + 8f, marginX + 135f, tickerTop + tickerH - 8f, shapePaint)
                textPaint.textSize = 24f
                textPaint.color = Color.WHITE
                canvas.drawText("अपडेट", marginX + 34f, tickerTop + 38f, textPaint)
            }

            private fun drawShortsFloatingPill(canvas: Canvas, w: Float, h: Float, cfg: OverlayConfig) {
                val marginX = 44f
                val pillW = w - (marginX * 2f)
                val pillH = 225f
                val bottomOffsetPx = h * (cfg.shortsBottomOffsetPercent / 100f)
                val pillTop = h - bottomOffsetPx - pillH

                // Dark translucent rounded pill
                shapePaint.color = Color.argb(210, 9, 9, 11)
                canvas.drawRoundRect(marginX, pillTop, marginX + pillW, pillTop + pillH, 38f, 38f, shapePaint)

                // Subtle border
                shapePaint.style = Paint.Style.STROKE
                shapePaint.strokeWidth = 2.5f
                shapePaint.color = Color.argb(65, 255, 255, 255)
                canvas.drawRoundRect(marginX, pillTop, marginX + pillW, pillTop + pillH, 38f, 38f, shapePaint)
                shapePaint.style = Paint.Style.FILL

                // Red Header Pill Badge Inside
                shapePaint.color = Color.rgb(220, 38, 38)
                canvas.drawRoundRect(marginX + 28f, pillTop + 26f, marginX + 275f, pillTop + 76f, 25f, 25f, shapePaint)

                textPaint.textSize = 26f
                textPaint.color = Color.WHITE
                canvas.drawText(cfg.bannerHeader, marginX + 52f, pillTop + 60f, textPaint)

                if (cfg.showDomainStrip) {
                    textPaint.textSize = 26f
                    textPaint.color = Color.rgb(252, 211, 77)
                    canvas.drawText("tikhikhabar.com", marginX + pillW - 245f, pillTop + 58f, textPaint)
                }

                // Headline inside Shorts pill
                textPaint.textSize = 38f
                textPaint.color = Color.WHITE
                drawMultilineText(
                    canvas,
                    cfg.headlineText,
                    marginX + 32f,
                    pillTop + 96f,
                    (pillW - 64f).toInt(),
                    2
                )
            }

            private fun drawMultilineText(
                canvas: Canvas,
                text: String,
                x: Float,
                y: Float,
                maxWidth: Int,
                maxLines: Int
            ) {
                val layout = StaticLayout.Builder
                    .obtain(text, 0, text.length, textPaint, maxWidth.coerceAtLeast(100))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setMaxLines(maxLines)
                    .setIncludePad(false)
                    .build()

                canvas.save()
                canvas.translate(x, y)
                layout.draw(canvas)
                canvas.restore()
            }
        }

        val targetWidth = if (config.isShorts) 1080 else 1920
        val targetHeight = if (config.isShorts) 1920 else 1080

        val videoEffects = ImmutableList.of<Effect>(
            Presentation.createForWidthAndHeight(
                targetWidth,
                targetHeight,
                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            ),
            OverlayEffect(ImmutableList.of(canvasOverlay))
        )

        val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(inputVideoUri))
            .setEffects(Effects(ImmutableList.of(), videoEffects))
            .build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    val savedPath = saveToGallery(outputFile)
                    onSuccess(savedPath)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    onError(exportException.localizedMessage ?: "Video export failed")
                }
            })
            .build()

        transformer.start(editedMediaItem, outputFile.absolutePath)
    }

    private fun saveToGallery(file: File): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TikhiKhabar")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, values)
            if (itemUri != null) {
                resolver.openOutputStream(itemUri)?.use { outStream ->
                    file.inputStream().use { inStream -> inStream.copyTo(outStream) }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                return "Movies/TikhiKhabar/${file.name}"
            }
        }
        return file.absolutePath
    }
}