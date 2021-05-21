package com.mambo.poetree.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Haiku(
    val title: String,
    val content: String,
    val poet: String
) : Parcelable {
}