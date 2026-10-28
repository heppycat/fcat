package io.github.heppycat.fcat.reader

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import io.github.heppycat.fcat.engine.PdfEngine
import io.github.heppycat.fcat.engine.defaultPdfEngine
import kotlinx.coroutines.withContext

/**
 * Opens the file from [openFile] and renders its first page to fit [viewportWidthPx] within [maxPixels]. A
 * stopgap for M1a; the M2 reader replaces it with layout and tiles.
 *
 * [openFile] runs on the engine thread, right before the engine takes ownership of the descriptor, so a
 * cancellation in between cannot leak it.
 */
suspend fun renderFirstPage(
    openFile: () -> ParcelFileDescriptor,
    viewportWidthPx: Int,
    maxPixels: Long,
    engine: PdfEngine = defaultPdfEngine(),
): Bitmap = withContext(engine.dispatcher) {
    engine.open(openFile()).use { doc ->
        val fit = fitWidth(doc.pageSize(0), viewportWidthPx, maxPixels)
        val bitmap = createBitmap(fit.widthPx, fit.heightPx)
        doc.renderRegion(0, Rect(0, 0, fit.widthPx, fit.heightPx), fit.scale, bitmap)
        bitmap
    }
}
