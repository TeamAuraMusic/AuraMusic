package com.auramusic.app.tv

import android.content.Context
import android.content.Intent
import com.auramusic.app.TvMainActivity

internal const val TV_RECOMMENDATION_MEDIA_ID_EXTRA = "media_id"

internal fun tvRecommendationClickIntent(context: Context, mediaId: String): Intent =
    Intent(Intent.ACTION_VIEW)
        .setClass(context, TvMainActivity::class.java)
        .putExtra(TV_RECOMMENDATION_MEDIA_ID_EXTRA, mediaId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

internal fun tvRecommendationMediaId(intent: Intent): String? =
    intent.getStringExtra(TV_RECOMMENDATION_MEDIA_ID_EXTRA)
