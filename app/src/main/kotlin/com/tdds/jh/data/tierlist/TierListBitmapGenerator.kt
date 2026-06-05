package com.tdds.jh.data.tierlist

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
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.tdds.jh.R
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.screens.tierlist.logic.utils.ColorUtils
import com.tdds.jh.screens.tierlist.logic.utils.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

private var cachedCustomTypeface: Typeface? = null
private val fontLock = Any()

fun getTypeface(context: Context, disableCustomFont: Boolean = false): Typeface? {
    return if (disableCustomFont) {
        Typeface.DEFAULT
    } else {
        synchronized(fontLock) {
            if (cachedCustomTypeface == null) {
                cachedCustomTypeface = try {
                    ResourcesCompat.getFont(context, R.font.custom_font)
                } catch (_: Exception) {
                    null
                }
            }
            cachedCustomTypeface
        }
    }
}

fun clearFontCache() {
    synchronized(fontLock) {
        cachedCustomTypeface = null
    }
}

fun drawWrappedTierLabel(
    canvas: Canvas,
    label: String,
    labelRect: RectF,
    bgColor: androidx.compose.ui.graphics.Color,
    context: Context,
    disableCustomFont: Boolean = false
) {
    val maxLineWidth = labelRect.width() * 0.9f
    val availableHeight = labelRect.height()

    val tempPaint = Paint().apply {
        textSize = 72f
        typeface = getTypeface(context, disableCustomFont)
    }

    val lines = mutableListOf<String>()
    var currentLine = ""

    for (char in label) {
        val testLine = currentLine + char
        val testWidth = tempPaint.measureText(testLine)
        if (testWidth > maxLineWidth && currentLine.isNotEmpty()) {
            lines.add(currentLine)
            currentLine = char.toString()
        } else {
            currentLine = testLine
        }
    }
    if (currentLine.isNotEmpty()) {
        lines.add(currentLine)
    }

    val targetTextSize = if (lines.size == 1) {
        minOf(72f, availableHeight * 0.8f)
    } else {
        availableHeight / (lines.size * 1.1f)
    }

    val textPaint = Paint().apply {
        color = if (ColorUtils.isDarkColor(bgColor)) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#2C3E50")
        textSize = targetTextSize.coerceAtMost(72f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        typeface = getTypeface(context, disableCustomFont)
    }

    val fontMetrics = textPaint.fontMetrics
    val lineHeight = (fontMetrics.descent - fontMetrics.ascent)
    val totalTextHeight = lines.size * lineHeight

    val startY = labelRect.centerY() - totalTextHeight / 2f + lineHeight / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f

    lines.forEachIndexed { index, line ->
        val y = startY + index * lineHeight
        canvas.drawText(line, labelRect.centerX(), y, textPaint)
    }
}

fun drawBadgeToCanvas(
    context: Context,
    canvas: Canvas,
    badgeUri: Uri,
    drawX: Float,
    drawY: Float,
    badgeSize: Int,
    reusablePaint: Paint? = null
) {
    var badgeBitmap: Bitmap? = null
    var roundedBadgeBitmap: Bitmap? = null
    var inputStream: InputStream? = null
    
    try {
        inputStream = context.contentResolver.openInputStream(badgeUri)
        if (inputStream != null) {
            badgeBitmap = BitmapFactory.decodeStream(inputStream)
            if (badgeBitmap != null) {
                roundedBadgeBitmap = Bitmap.createBitmap(badgeSize, badgeSize, Bitmap.Config.ARGB_8888)
                val badgeCanvas = Canvas(roundedBadgeBitmap)
                val badgePaint = reusablePaint ?: Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                    isDither = true
                }
                badgePaint.isAntiAlias = true
                badgePaint.isFilterBitmap = true
                badgePaint.isDither = true
                
                val badgeRect = RectF(0f, 0f, badgeSize.toFloat(), badgeSize.toFloat())
                val badgeCornerRadius = badgeSize * 0.1f
                badgeCanvas.drawRoundRect(badgeRect, badgeCornerRadius, badgeCornerRadius, badgePaint)
                badgePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                val badgeSrcRect = Rect(0, 0, badgeBitmap.width, badgeBitmap.height)
                val badgeDstRect = RectF(0f, 0f, badgeSize.toFloat(), badgeSize.toFloat())
                badgeCanvas.drawBitmap(badgeBitmap, badgeSrcRect, badgeDstRect, badgePaint)
                badgePaint.xfermode = null
                
                canvas.drawBitmap(roundedBadgeBitmap, drawX, drawY, null)
            }
        }
    } catch (_: Exception) {
    } finally {
        inputStream?.close()
        badgeBitmap?.recycle()
        roundedBadgeBitmap?.recycle()
    }
}

suspend fun generateTierListBitmap(
    context: Context,
    tiers: List<TierItem>,
    tierImages: List<TierImage>,
    title: String = context.getString(R.string.tier_list_default_title),
    authorName: String = "",
    isDarkTheme: Boolean = false,
    externalBadgeEnabled: Boolean = false,
    disableCustomFont: Boolean = false,
    nameBelowImage: Boolean = false
): Bitmap {
    return withContext(Dispatchers.IO) {
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

        val imageAreaStartX = cardPadding + padding + labelWidth + padding

        val maxBadgeCountInRow = tiers.map { tier ->
            val tierImageList = tierImages.filter { it.tierLabel == tier.label }
            val imageCount = tierImageList.size
            val imagesInRow = minOf(imageCount, imagesPerRow)
            if (externalBadgeEnabled) {
                tierImageList.take(imagesInRow).count { imageData ->
                    imageData.badgeUri != null || imageData.badgeUri2 != null || imageData.badgeUri3 != null
                }
            } else 0
        }.maxOrNull() ?: 0

        val imageAreaWidth = padding * 2 + imagesPerRow * imageSize.toInt() + (imagesPerRow - 1) * itemSpacing + maxBadgeCountInRow * badgeSize

        val nameHeightExtra = if (nameBelowImage) 40f else 0f

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

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColorStart = if (isDarkTheme) "#121212" else "#F5F7FA"
        val bgColorEnd = if (isDarkTheme) "#1E1E1E" else "#E4E8EC"
        val titleColor = if (isDarkTheme) "#FFFFFF" else "#2C3E50"
        val cardBgColor = if (isDarkTheme) "#2D2D2D" else "#FFFFFF"
        val imageAreaBgColor = if (isDarkTheme) "#1A1A1A" else "#F8F9FA"
        val imageBorderColor = if (isDarkTheme) "#3D3D3D" else "#E0E0E0"
        val authorColor = if (isDarkTheme) "#AAAAAA" else "#7F8C8D"
        val timeColor = if (isDarkTheme) "#888888" else "#95A5A6"

        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                android.graphics.Color.parseColor(bgColorStart),
                android.graphics.Color.parseColor(bgColorEnd),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val titlePaint = Paint().apply {
            color = android.graphics.Color.parseColor(titleColor)
            textSize = 170f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            typeface = getTypeface(context, disableCustomFont)
            setShadowLayer(4f, 2f, 2f, android.graphics.Color.parseColor(if (isDarkTheme) "#60000000" else "#40000000"))
        }
        canvas.drawText(title, width / 2f, 200f, titlePaint)
        titlePaint.setShadowLayer(0f, 0f, 0f, 0)

        var yOffset = titleHeight.toFloat()

        val cardRect = RectF(
            cardPadding,
            yOffset - 20f,
            width - cardPadding,
            yOffset + totalContentHeight + 20f
        )

        val shadowPaint = Paint().apply {
            color = android.graphics.Color.parseColor(if (isDarkTheme) "#80000000" else "#40000000")
            maskFilter = android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(
            cardRect.left + shadowOffset,
            cardRect.top + shadowOffset,
            cardRect.right + shadowOffset,
            cardRect.bottom + shadowOffset,
            cornerRadius,
            cornerRadius,
            shadowPaint
        )

        val cardBgPaint = Paint().apply {
            color = android.graphics.Color.parseColor(cardBgColor)
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardBgPaint)

        val tierPaint = Paint()
        val imageAreaPaint = Paint().apply {
            color = android.graphics.Color.parseColor(imageAreaBgColor)
        }
        val imageBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor(imageBorderColor)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val roundPaint = Paint().apply {
            isAntiAlias = true
        }
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
        val authorPaint = Paint().apply {
            textSize = 84f
            textAlign = Paint.Align.LEFT
            typeface = getTypeface(context, disableCustomFont)
            isFakeBoldText = true
        }
        val timePaint = Paint().apply {
            textSize = 84f
            textAlign = Paint.Align.RIGHT
            isFakeBoldText = true
            typeface = getTypeface(context, disableCustomFont)
        }

        tiers.forEachIndexed { index, tier ->
            val tierImageList = tierImages.filter { it.tierLabel == tier.label }
            val currentRowHeight = tierHeights[index]

            tierPaint.color = tier.color.toArgb()
            val labelRect = RectF(
                cardPadding + padding,
                yOffset,
                cardPadding + padding + labelWidth,
                yOffset + currentRowHeight
            )
            canvas.drawRoundRect(labelRect, 16f, 16f, tierPaint)

            drawWrappedTierLabel(canvas, tier.label, labelRect, tier.color, context, disableCustomFont)

            val imageAreaRect = RectF(
                cardPadding + padding + labelWidth + padding,
                yOffset,
                width - cardPadding - padding,
                yOffset + currentRowHeight
            )
            canvas.drawRoundRect(imageAreaRect, 12f, 12f, imageAreaPaint)

            val rowHeights = mutableListOf<Int>()
            val tierRowCount = maxOf(1, (tierImageList.size + imagesPerRow - 1) / imagesPerRow)
            for (rowIndex in 0 until tierRowCount) {
                val startIdx = rowIndex * imagesPerRow
                val endIdx = minOf(startIdx + imagesPerRow, tierImageList.size)
                val rowImages = tierImageList.subList(startIdx, endIdx)
                val hasNamedImage = rowImages.any { it.name.isNotBlank() }
                val rowHeightLocal = imageSize.toInt() + if (nameBelowImage && hasNamedImage) nameHeightExtra.toInt() else 0
                rowHeights.add(rowHeightLocal)
            }
            
            val startY = (yOffset + padding).toInt()
            var currentRow = 0
            var currentX = imageAreaStartX + padding
            var imagesInCurrentRow = 0
            var currentY = startY

            tierImageList.forEach { tierImage ->
                var imageBitmap: Bitmap? = null
                var scaledBitmap: Bitmap? = null
                var roundedBitmap: Bitmap? = null

                try {
                    context.contentResolver.openInputStream(tierImage.uri)?.use { inputStream ->
                        imageBitmap = BitmapFactory.decodeStream(inputStream)

                        if (imageBitmap != null) {
                            if (imagesInCurrentRow >= imagesPerRow) {
                                currentY += rowHeights[currentRow] + padding.toInt()
                                currentRow++
                                currentX = imageAreaStartX + padding
                                imagesInCurrentRow = 0
                            }

                            val finalDrawX = currentX
                            val finalDrawY = currentY

                            val bitmap = imageBitmap
                            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                            val scaledWidth: Int
                            val scaledHeight: Int
                            val maxImageSize = imageSize.toInt()
                            if (aspectRatio > 1) {
                                scaledWidth = maxImageSize
                                scaledHeight = (maxImageSize / aspectRatio).toInt()
                            } else {
                                scaledHeight = maxImageSize
                                scaledWidth = (maxImageSize * aspectRatio).toInt()
                            }

                            scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

                            roundedBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                            val roundedCanvas = Canvas(roundedBitmap)
                            val roundRect = RectF(0f, 0f, scaledWidth.toFloat(), scaledHeight.toFloat())
                            roundedCanvas.drawRoundRect(roundRect, 12f, 12f, roundPaint)
                            roundPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                            roundedCanvas.drawBitmap(scaledBitmap, 0f, 0f, roundPaint)
                            roundPaint.xfermode = null

                            val centerX = finalDrawX + imageSize / 2f
                            val centerY = finalDrawY + imageSize / 2f
                            val bitmapDrawX = centerX - scaledWidth / 2f
                            val bitmapDrawY = centerY - scaledHeight / 2f
                            canvas.drawBitmap(roundedBitmap, bitmapDrawX, bitmapDrawY, null)

                            val borderRect = RectF(
                                bitmapDrawX,
                                bitmapDrawY,
                                bitmapDrawX + scaledWidth,
                                bitmapDrawY + scaledHeight
                            )
                            canvas.drawRoundRect(borderRect, 12f, 12f, imageBorderPaint)

                            if (tierImage.badgeUri != null || tierImage.badgeUri2 != null || tierImage.badgeUri3 != null) {
                                val badgeSizeLocal = (imageSize * 0.22).toInt()
                                val badgeSpacing = badgeSizeLocal * 0.05f

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

                                listOfNotNull(
                                    tierImage.badgeUri?.let { it to 0 },
                                    tierImage.badgeUri2?.let { it to 1 },
                                    tierImage.badgeUri3?.let { it to 2 }
                                ).forEach { (uri, idx) ->
                                    drawBadgeToCanvas(
                                        context, canvas, uri, badgeDrawX,
                                        badgeDrawYStart + (badgeSizeLocal + badgeSpacing) * idx,
                                        badgeSizeLocal,
                                        badgePaint
                                    )
                                }
                            }

                            if (tierImage.name.isNotBlank()) {
                                nameBgPaint.color = android.graphics.Color.parseColor(if (isDarkTheme) "#CC000000" else "#CCFFFFFF")
                                nameTextPaint.color = android.graphics.Color.parseColor(if (isDarkTheme) "#FFFFFF" else "#000000")
                                
                                val displayName = TextUtils.truncateImageName(tierImage.name)
                                val textBounds = Rect()
                                nameTextPaint.getTextBounds(displayName, 0, displayName.length, textBounds)
                                val textHeight = textBounds.height()
                                val paddingY = 4f

                                if (nameBelowImage) {
                                    val nameBgHeight = textHeight + paddingY * 2
                                    val nameCornerRadius = nameBgHeight * 0.1f
                                    val nameTop = finalDrawY + imageSize
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

                            val hasBadge = tierImage.badgeUri != null || tierImage.badgeUri2 != null || tierImage.badgeUri3 != null
                            val imageOccupiedWidth = imageSize + itemSpacing + if (externalBadgeEnabled && hasBadge) badgeSize else 0
                            currentX += imageOccupiedWidth
                            imagesInCurrentRow++
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    imageBitmap?.recycle()
                    scaledBitmap?.recycle()
                    roundedBitmap?.recycle()
                }
            }

            yOffset += currentRowHeight + padding
        }

        if (authorName.isNotBlank()) {
            authorPaint.color = android.graphics.Color.parseColor(authorColor)
            val authorLabel = context.getString(R.string.tier_author, authorName)
            canvas.drawText(
                authorLabel,
                cardPadding + padding,
                height - bottomPadding / 2f - 20f,
                authorPaint
            )
        }

        val timeFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val currentTime = timeFormat.format(java.util.Date())
        timePaint.color = android.graphics.Color.parseColor(timeColor)
        canvas.drawText(
            currentTime,
            width - cardPadding - padding,
            height - bottomPadding / 2f - 20f,
            timePaint
        )

        bitmap
    }
}
