package com.tdds.jh.data.tierlist.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import com.tdds.jh.R
import com.tdds.jh.data.tierlist.drawBadgeToCanvas
import com.tdds.jh.data.tierlist.drawWrappedTierLabel
import com.tdds.jh.data.tierlist.getTypeface
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.model.tierlist.video.frame.FrameState
import com.tdds.jh.model.tierlist.video.frame.FrameStateComputer
import com.tdds.jh.model.tierlist.video.timeline.Timeline
import com.tdds.jh.screens.tierlist.logic.utils.ColorUtils
import com.tdds.jh.screens.tierlist.logic.utils.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * 帧位图渲染器
 *
 * 根据时间线当前时间渲染一帧 Bitmap，支持逐步显示图片、名字与小图标。
 */
class BitmapFrameRenderer(private val context: Context) {

    /**
     * 渲染指定时间点的帧
     */
    suspend fun renderFrame(
        tiers: List<TierItem>,
        tierImages: List<TierImage>,
        config: VideoGenerationConfig,
        timeline: Timeline,
        currentTime: Float,
        isDarkTheme: Boolean = false,
        disableCustomFont: Boolean = false,
        externalBadgeEnabled: Boolean = false,
        nameBelowImage: Boolean = false,
        title: String = context.getString(R.string.tier_list_default_title),
        authorName: String = ""
    ): Bitmap = withContext(Dispatchers.IO) {
        val frameStates = FrameStateComputer.computeFrameStates(timeline, currentTime, config)
        val layout = computeLayout(tiers, tierImages, externalBadgeEnabled, nameBelowImage)

        val sourceBitmap = renderSourceBitmap(
            layout = layout,
            tiers = tiers,
            tierImages = tierImages,
            frameStates = frameStates,
            isDarkTheme = isDarkTheme,
            disableCustomFont = disableCustomFont,
            externalBadgeEnabled = externalBadgeEnabled,
            nameBelowImage = nameBelowImage,
            title = title,
            authorName = authorName
        )

        scaleToOutput(sourceBitmap, config.outputWidth, config.outputHeight)
    }

    private fun computeLayout(
        tiers: List<TierItem>,
        tierImages: List<TierImage>,
        externalBadgeEnabled: Boolean,
        nameBelowImage: Boolean
    ): FrameLayout {
        val rowHeight = 360
        val titleHeight = 320
        val bottomPadding = 140
        val cardPadding = 70f
        val cornerRadius = 42f
        val shadowOffset = 10f
        val labelWidth = 320f
        val padding = (rowHeight * 0.08).toInt().toFloat()
        val imageSize = rowHeight - padding * 2
        val badgeSize = (imageSize * 0.22).toInt()
        val imagesPerRow = 12
        val itemSpacing = (rowHeight * 0.15).toInt()
        val nameHeightExtra = if (nameBelowImage) 40f else 0f

        val imageAreaStartX = cardPadding + padding + labelWidth + padding

        val maxBadgeCountInRow = tiers.maxOfOrNull { tier ->
            val tierImageList = tierImages.filter { it.tierLabel == tier.label }
            if (externalBadgeEnabled) {
                val firstRowImages = tierImageList.take(imagesPerRow)
                firstRowImages.count { it.badgeUri != null || it.badgeUri2 != null || it.badgeUri3 != null }
            } else 0
        } ?: 0

        val imageAreaWidth = padding * 2f +
            imagesPerRow * imageSize +
            (imagesPerRow - 1) * itemSpacing +
            maxBadgeCountInRow * badgeSize

        val tierHeights = tiers.map { tier ->
            val tierImageList = tierImages.filter { it.tierLabel == tier.label }
            val imageCount = tierImageList.size
            val rowCount = maxOf(1, (imageCount + imagesPerRow - 1) / imagesPerRow)

            var totalImageContentHeight = (padding * 2).toInt()
            for (rowIndex in 0 until rowCount) {
                val startIdx = rowIndex * imagesPerRow
                val endIdx = minOf(startIdx + imagesPerRow, imageCount)
                val rowImages = tierImageList.subList(startIdx, endIdx)
                val hasNamedImage = rowImages.any { it.name.isNotBlank() }
                val rowHeightLocal = imageSize.toInt() + if (nameBelowImage && hasNamedImage) nameHeightExtra.toInt() else 0
                totalImageContentHeight += rowHeightLocal
                if (rowIndex < rowCount - 1) {
                    totalImageContentHeight += padding.toInt()
                }
            }

            val labelLineCount = TextUtils.calculateLabelLineCount(tier.label, labelWidth, 72f)
            val labelTextHeight = if (labelLineCount > 1) {
                val lineHeight = 72f * 1.2f
                (labelLineCount * lineHeight + padding * 2).toInt()
            } else {
                rowHeight
            }
            maxOf(labelTextHeight, totalImageContentHeight)
        }

        val totalContentHeight = tierHeights.sum() + ((tiers.size - 1) * padding).toInt()
        val width = (cardPadding + padding + labelWidth + padding + imageAreaWidth + padding + cardPadding).toInt()
        val height = totalContentHeight + titleHeight + bottomPadding + 2 * cardPadding.toInt()

        return FrameLayout(
            width = width,
            height = height,
            rowHeight = rowHeight,
            titleHeight = titleHeight,
            bottomPadding = bottomPadding,
            cardPadding = cardPadding,
            cornerRadius = cornerRadius,
            shadowOffset = shadowOffset,
            labelWidth = labelWidth,
            padding = padding,
            imageSize = imageSize,
            badgeSize = badgeSize,
            imagesPerRow = imagesPerRow,
            itemSpacing = itemSpacing,
            nameHeightExtra = nameHeightExtra,
            imageAreaStartX = imageAreaStartX,
            imageAreaWidth = imageAreaWidth,
            tierHeights = tierHeights,
            totalContentHeight = totalContentHeight
        )
    }

    private fun renderSourceBitmap(
        layout: FrameLayout,
        tiers: List<TierItem>,
        tierImages: List<TierImage>,
        frameStates: Map<String, FrameState>,
        isDarkTheme: Boolean,
        disableCustomFont: Boolean,
        externalBadgeEnabled: Boolean,
        nameBelowImage: Boolean,
        title: String,
        authorName: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas, layout, isDarkTheme)
        drawTitle(canvas, layout, title, isDarkTheme, disableCustomFont)
        drawCard(canvas, layout, isDarkTheme)

        var yOffset = layout.titleHeight.toFloat()
        val imageAreaPaint = Paint().apply {
            color = android.graphics.Color.parseColor(if (isDarkTheme) "#1A1A1A" else "#F8F9FA")
        }
        val roundPaint = Paint().apply { isAntiAlias = true }
        val badgePaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
        val nameBgPaint = Paint()
        val nameTextPaint = Paint().apply {
            textSize = 30f
            textAlign = Paint.Align.CENTER
            typeface = getTypeface(context, disableCustomFont)
        }

        tiers.forEachIndexed { index, tier ->
            val tierImageList = tierImages.filter { it.tierLabel == tier.label }
            val currentRowHeight = layout.tierHeights[index]

            val tierPaint = Paint().apply { color = tier.color.toArgb() }
            val labelRect = RectF(
                layout.cardPadding + layout.padding,
                yOffset,
                layout.cardPadding + layout.padding + layout.labelWidth,
                yOffset + currentRowHeight
            )
            canvas.drawRoundRect(labelRect, 16f, 16f, tierPaint)
            drawWrappedTierLabel(canvas, tier.label, labelRect, tier.color, context, disableCustomFont)

            val imageAreaRect = RectF(
                layout.cardPadding + layout.padding + layout.labelWidth + layout.padding,
                yOffset,
                layout.width - layout.cardPadding - layout.padding,
                yOffset + currentRowHeight
            )
            canvas.drawRoundRect(imageAreaRect, 12f, 12f, imageAreaPaint)

            val rowHeights = mutableListOf<Int>()
            val tierRowCount = maxOf(1, (tierImageList.size + layout.imagesPerRow - 1) / layout.imagesPerRow)
            for (rowIndex in 0 until tierRowCount) {
                val startIdx = rowIndex * layout.imagesPerRow
                val endIdx = minOf(startIdx + layout.imagesPerRow, tierImageList.size)
                val rowImages = tierImageList.subList(startIdx, endIdx)
                val hasNamedImage = rowImages.any { it.name.isNotBlank() }
                val rowHeightLocal = layout.imageSize.toInt() +
                    if (nameBelowImage && hasNamedImage) layout.nameHeightExtra.toInt() else 0
                rowHeights.add(rowHeightLocal)
            }

            val startY = (yOffset + layout.padding).toInt()
            var currentRow = 0
            var currentX = layout.imageAreaStartX + layout.padding
            var imagesInCurrentRow = 0
            var currentY = startY.toFloat()

            tierImageList.forEach { tierImage ->
                if (imagesInCurrentRow >= layout.imagesPerRow) {
                    currentY += rowHeights.getOrElse(currentRow) { layout.imageSize.toInt() } + layout.padding.toInt()
                    currentRow = min(currentRow + 1, rowHeights.size - 1)
                    currentX = layout.imageAreaStartX + layout.padding
                    imagesInCurrentRow = 0
                }

                val state = frameStates[tierImage.id] ?: FrameState()
                if (state.placed) {
                    drawTierImage(
                        canvas = canvas,
                        tierImage = tierImage,
                        frameState = state,
                        layout = layout,
                        currentX = currentX,
                        currentY = currentY,
                        imageAreaPaint = imageAreaPaint,
                        roundPaint = roundPaint,
                        badgePaint = badgePaint,
                        nameBgPaint = nameBgPaint,
                        nameTextPaint = nameTextPaint,
                        isDarkTheme = isDarkTheme,
                        externalBadgeEnabled = externalBadgeEnabled,
                        nameBelowImage = nameBelowImage
                    )
                }

                val hasBadge = tierImage.badgeUri != null || tierImage.badgeUri2 != null || tierImage.badgeUri3 != null
                val imageOccupiedWidth = layout.imageSize + layout.itemSpacing +
                    if (externalBadgeEnabled && hasBadge) layout.badgeSize else 0
                currentX += imageOccupiedWidth
                imagesInCurrentRow++
            }

            yOffset += currentRowHeight + layout.padding
        }

        drawFooter(canvas, layout, authorName, isDarkTheme, disableCustomFont)
        return bitmap
    }

    private fun drawBackground(canvas: Canvas, layout: FrameLayout, isDarkTheme: Boolean) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, layout.width.toFloat(), layout.height.toFloat(),
                android.graphics.Color.parseColor(if (isDarkTheme) "#121212" else "#F5F7FA"),
                android.graphics.Color.parseColor(if (isDarkTheme) "#1E1E1E" else "#E4E8EC"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, layout.width.toFloat(), layout.height.toFloat(), bgPaint)
    }

    private fun drawTitle(canvas: Canvas, layout: FrameLayout, title: String, isDarkTheme: Boolean, disableCustomFont: Boolean) {
        val titlePaint = Paint().apply {
            color = android.graphics.Color.parseColor(if (isDarkTheme) "#FFFFFF" else "#2C3E50")
            textSize = 170f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            typeface = getTypeface(context, disableCustomFont)
            setShadowLayer(4f, 2f, 2f, android.graphics.Color.parseColor(if (isDarkTheme) "#60000000" else "#40000000"))
        }
        canvas.drawText(title, layout.width / 2f, 200f, titlePaint)
    }

    private fun drawCard(canvas: Canvas, layout: FrameLayout, isDarkTheme: Boolean) {
        val cardRect = RectF(
            layout.cardPadding,
            layout.titleHeight - 20f,
            layout.width - layout.cardPadding,
            layout.titleHeight + layout.totalContentHeight + 20f
        )
        val shadowPaint = Paint().apply {
            color = android.graphics.Color.parseColor(if (isDarkTheme) "#80000000" else "#40000000")
            maskFilter = android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(
            cardRect.left + layout.shadowOffset,
            cardRect.top + layout.shadowOffset,
            cardRect.right + layout.shadowOffset,
            cardRect.bottom + layout.shadowOffset,
            layout.cornerRadius,
            layout.cornerRadius,
            shadowPaint
        )
        val cardBgPaint = Paint().apply {
            color = android.graphics.Color.parseColor(if (isDarkTheme) "#2D2D2D" else "#FFFFFF")
        }
        canvas.drawRoundRect(cardRect, layout.cornerRadius, layout.cornerRadius, cardBgPaint)
    }

    private fun drawFooter(canvas: Canvas, layout: FrameLayout, authorName: String, isDarkTheme: Boolean, disableCustomFont: Boolean) {
        val authorPaint = Paint().apply {
            textSize = 84f
            textAlign = Paint.Align.LEFT
            typeface = getTypeface(context, disableCustomFont)
            isFakeBoldText = true
            color = android.graphics.Color.parseColor(if (isDarkTheme) "#AAAAAA" else "#7F8C8D")
        }
        val timePaint = Paint().apply {
            textSize = 84f
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = true
            typeface = getTypeface(context, disableCustomFont)
            color = android.graphics.Color.parseColor(if (isDarkTheme) "#888888" else "#95A5A6")
        }

        if (authorName.isNotBlank()) {
            val authorLabel = context.getString(R.string.tier_author, authorName)
            canvas.drawText(
                authorLabel,
                layout.cardPadding + layout.padding,
                layout.height - layout.bottomPadding / 2f - 20f,
                authorPaint
            )
        }

        val timeFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val currentTime = timeFormat.format(java.util.Date())
        canvas.drawText(
            currentTime,
            layout.width - layout.cardPadding - layout.padding,
            layout.height - layout.bottomPadding / 2f - 20f,
            timePaint
        )
    }

    private fun drawTierImage(
        canvas: Canvas,
        tierImage: TierImage,
        frameState: FrameState,
        layout: FrameLayout,
        currentX: Float,
        currentY: Float,
        imageAreaPaint: Paint,
        roundPaint: Paint,
        badgePaint: Paint,
        nameBgPaint: Paint,
        nameTextPaint: Paint,
        isDarkTheme: Boolean,
        externalBadgeEnabled: Boolean,
        nameBelowImage: Boolean
    ) {
        var imageBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        var roundedBitmap: Bitmap? = null

        try {
            context.contentResolver.openInputStream(tierImage.uri)?.use { inputStream ->
                imageBitmap = BitmapFactory.decodeStream(inputStream)
                if (imageBitmap == null) return@use

                val bitmap = imageBitmap!!
                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val maxImageSize = layout.imageSize.toInt()
                val scaledWidth: Int
                val scaledHeight: Int
                if (aspectRatio > 1) {
                    scaledWidth = maxImageSize
                    scaledHeight = (maxImageSize / aspectRatio).toInt()
                } else {
                    scaledHeight = maxImageSize
                    scaledWidth = (maxImageSize * aspectRatio).toInt()
                }

                scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                roundedBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                val roundedCanvas = Canvas(roundedBitmap!!)
                val roundRect = RectF(0f, 0f, scaledWidth.toFloat(), scaledHeight.toFloat())
                roundedCanvas.drawRoundRect(roundRect, 12f, 12f, roundPaint)
                roundPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                roundedCanvas.drawBitmap(scaledBitmap!!, 0f, 0f, roundPaint)
                roundPaint.xfermode = null

                val centerX = currentX + layout.imageSize / 2f
                val centerY = currentY + layout.imageSize / 2f
                val bitmapDrawX = centerX - scaledWidth / 2f
                val bitmapDrawY = centerY - scaledHeight / 2f
                canvas.drawBitmap(roundedBitmap!!, bitmapDrawX, bitmapDrawY, null)

                drawBadges(
                    canvas, tierImage, frameState,
                    layout, bitmapDrawX, bitmapDrawY, scaledWidth, scaledHeight,
                    badgePaint, externalBadgeEnabled
                )

                if (frameState.nameVisibleChars > 0 && tierImage.name.isNotBlank()) {
                    drawName(
                        canvas, tierImage, frameState,
                        layout, centerX, bitmapDrawX, bitmapDrawY,
                        scaledWidth, scaledHeight,
                        nameBgPaint, nameTextPaint, isDarkTheme, nameBelowImage
                    )
                }
            }
        } catch (_: Exception) {
        } finally {
            imageBitmap?.recycle()
            scaledBitmap?.recycle()
            roundedBitmap?.recycle()
        }
    }

    private fun drawBadges(
        canvas: Canvas,
        tierImage: TierImage,
        frameState: FrameState,
        layout: FrameLayout,
        bitmapDrawX: Float,
        bitmapDrawY: Float,
        scaledWidth: Int,
        scaledHeight: Int,
        badgePaint: Paint,
        externalBadgeEnabled: Boolean
    ) {
        if (frameState.visibleBadgeSlots.isEmpty()) return

        val badgeSizeLocal = (layout.imageSize * 0.22).toInt()
        val badgeSpacing = badgeSizeLocal * 0.05f

        val badgeUris = listOfNotNull(
            tierImage.badgeUri?.let { it to 0 },
            tierImage.badgeUri2?.let { it to 1 },
            tierImage.badgeUri3?.let { it to 2 }
        ).filter { frameState.visibleBadgeSlots.contains(it.second) }

        val (badgeDrawX, badgeDrawYStart) = if (externalBadgeEnabled) {
            val badgeGap = 2f
            Pair(bitmapDrawX + scaledWidth + badgeGap, bitmapDrawY)
        } else {
            val badgeMargin = badgeSizeLocal * 0.1f
            Pair(
                bitmapDrawX + scaledWidth - badgeSizeLocal - badgeMargin,
                bitmapDrawY + badgeMargin
            )
        }

        badgeUris.forEachIndexed { index, (uri, _) ->
            drawBadgeToCanvas(
                context, canvas, uri, badgeDrawX,
                badgeDrawYStart + (badgeSizeLocal + badgeSpacing) * index,
                badgeSizeLocal,
                badgePaint
            )
        }
    }

    private fun drawName(
        canvas: Canvas,
        tierImage: TierImage,
        frameState: FrameState,
        layout: FrameLayout,
        centerX: Float,
        bitmapDrawX: Float,
        bitmapDrawY: Float,
        scaledWidth: Int,
        scaledHeight: Int,
        nameBgPaint: Paint,
        nameTextPaint: Paint,
        isDarkTheme: Boolean,
        nameBelowImage: Boolean
    ) {
        nameBgPaint.color = android.graphics.Color.parseColor(if (isDarkTheme) "#CC000000" else "#CCFFFFFF")
        nameTextPaint.color = android.graphics.Color.parseColor(if (isDarkTheme) "#FFFFFF" else "#000000")

        val visibleName = tierImage.name.take(frameState.nameVisibleChars)
        val displayName = TextUtils.truncateImageName(visibleName)
        val textBounds = Rect()
        nameTextPaint.getTextBounds(displayName, 0, displayName.length, textBounds)
        val textHeight = textBounds.height()
        val paddingY = 4f

        if (nameBelowImage) {
            val nameBgHeight = textHeight + paddingY * 2
            val nameCornerRadius = nameBgHeight * 0.1f
            val nameTop = bitmapDrawY + layout.imageSize
            val bgRect = RectF(
                bitmapDrawX,
                nameTop,
                bitmapDrawX + scaledWidth,
                nameTop + nameBgHeight
            )
            canvas.drawRoundRect(bgRect, nameCornerRadius, nameCornerRadius, nameBgPaint)
            val fontMetrics = nameTextPaint.fontMetrics
            val textOffset = (fontMetrics.descent - fontMetrics.ascent) / 2f - fontMetrics.descent
            val nameY = nameTop + nameBgHeight / 2f + textOffset
            canvas.drawText(displayName, centerX, nameY, nameTextPaint)
        } else {
            val bgRect = RectF(
                bitmapDrawX,
                bitmapDrawY + scaledHeight - textHeight - paddingY * 2 - 4f,
                bitmapDrawX + scaledWidth,
                bitmapDrawY + scaledHeight
            )
            canvas.drawRect(bgRect, nameBgPaint)
            val nameY = bitmapDrawY + scaledHeight - paddingY - 4f
            canvas.drawText(displayName, centerX, nameY, nameTextPaint)
        }
    }

    private fun scaleToOutput(source: Bitmap, outputWidth: Int, outputHeight: Int): Bitmap {
        if (source.width == outputWidth && source.height == outputHeight) {
            return source
        }
        val scale = min(outputWidth / source.width.toFloat(), outputHeight / source.height.toFloat())
        val scaledWidth = (source.width * scale).toInt()
        val scaledHeight = (source.height * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)

        val outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val outputCanvas = Canvas(outputBitmap)
        val bgPaint = Paint().apply { color = android.graphics.Color.BLACK }
        outputCanvas.drawRect(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat(), bgPaint)
        outputCanvas.drawBitmap(
            scaledBitmap,
            (outputWidth - scaledWidth) / 2f,
            (outputHeight - scaledHeight) / 2f,
            null
        )
        scaledBitmap.recycle()
        if (source != scaledBitmap) source.recycle()
        return outputBitmap
    }

    private data class FrameLayout(
        val width: Int,
        val height: Int,
        val rowHeight: Int,
        val titleHeight: Int,
        val bottomPadding: Int,
        val cardPadding: Float,
        val cornerRadius: Float,
        val shadowOffset: Float,
        val labelWidth: Float,
        val padding: Float,
        val imageSize: Float,
        val badgeSize: Int,
        val imagesPerRow: Int,
        val itemSpacing: Int,
        val nameHeightExtra: Float,
        val imageAreaStartX: Float,
        val imageAreaWidth: Float,
        val tierHeights: List<Int>,
        val totalContentHeight: Int
    )
}
