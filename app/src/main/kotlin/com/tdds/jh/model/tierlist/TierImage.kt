package com.tdds.jh.model.tierlist

import android.net.Uri

data class TierImage(
    val id: String,
    val tierLabel: String,
    val uri: Uri,
    val name: String = "",
    val badgeUri: Uri? = null,
    val badgeUri2: Uri? = null,
    val badgeUri3: Uri? = null,
    val cropPositionX: Float = 0.5f,
    val cropPositionY: Float = 0.5f,
    val cropScale: Float = 1.0f,
    val isCropped: Boolean = false,
    val originalUri: Uri? = null,
    val cropRatio: Float = 0f,
    val useCustomCrop: Boolean = false,
    val customCropWidth: Int = 0,
    val customCropHeight: Int = 0
)
