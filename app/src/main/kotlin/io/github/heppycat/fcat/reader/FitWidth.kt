package io.github.heppycat.fcat.reader

import io.github.heppycat.fcat.engine.PageSize
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A page drawn at [scale] pixels per point is [widthPx] × [heightPx] pixels. */
data class PageFit(val scale: Float, val widthPx: Int, val heightPx: Int)

/**
 * Scales [page] so its width fills [viewportWidthPx], unless that needs more than about [maxPixels] pixels:
 * then the page is scaled down to fit that budget. The cap stands in for tiled rendering (M2), so an extreme
 * page shape cannot ask for a bitmap larger than memory allows.
 */
fun fitWidth(page: PageSize, viewportWidthPx: Int, maxPixels: Long): PageFit {
    require(page.width > 0f && page.height > 0f) { "empty page $page" }
    require(viewportWidthPx > 0 && maxPixels > 0) { "empty viewport or budget" }
    val widthScale = viewportWidthPx / page.width
    val budgetScale = sqrt(maxPixels / (page.width.toDouble() * page.height)).toFloat()
    val scale = min(widthScale, budgetScale)
    // Rounded, not truncated: viewport / width * width can land just below the viewport in float maths.
    return PageFit(
        scale = scale,
        widthPx = (page.width * scale).roundToInt().coerceIn(1, viewportWidthPx),
        heightPx = (page.height * scale).roundToInt().coerceAtLeast(1),
    )
}
