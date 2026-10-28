package io.github.heppycat.fcat.engine

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The seam for the M1 engine bake-off (docs/decisions/0002-rendering-engine.md). Nothing outside this
 * package may depend on a specific engine's types.
 *
 * All calls here and on [PdfDocument] block and touch native code. Make them only from [dispatcher].
 */
interface PdfEngine {
    /** The engine-owned thread that every call on this engine and its documents must run on. */
    val dispatcher: CoroutineDispatcher

    /**
     * Opens a document. The returned document owns [file] and closes it in [PdfDocument.close], also when
     * opening fails.
     *
     * @throws PdfPasswordException if the document is encrypted and [password] is missing or wrong.
     * @throws java.io.IOException or [RuntimeException] if the file cannot be read or parsed.
     */
    fun open(file: ParcelFileDescriptor, password: String? = null): PdfDocument
}

interface PdfDocument : AutoCloseable {
    val pageCount: Int

    /** Size of page [index] in points (1/72 inch), with the page's rotation applied. */
    fun pageSize(index: Int): PageSize

    /**
     * Renders part of page [index] into the top-left corner of [target], replacing what is there.
     *
     * [region] is in pixels of the whole page drawn at [scale] pixels per point, so the full page is
     * `pageSize * scale` pixels. [target] must be mutable, [Bitmap.Config.ARGB_8888], and at least as big
     * as [region].
     */
    fun renderRegion(index: Int, region: Rect, scale: Float, target: Bitmap)

    /** The document outline (table of contents), or an empty list if it has none. */
    fun outline(): List<OutlineItem>

    /** The text of page [index], line by line, in reading order as the engine determines it. */
    fun pageText(index: Int): PageText
}

class PdfPasswordException(message: String) : Exception(message)

data class PageSize(val width: Float, val height: Float)

/** [pageIndex] is -1 when the entry has no target in this document, for example an external link. */
data class OutlineItem(val title: String, val pageIndex: Int, val children: List<OutlineItem>)

data class PageText(val lines: List<TextLine>)

/** A line of text and its bounding box in points, origin at the page's top-left corner. */
data class TextLine(val text: String, val left: Float, val top: Float, val right: Float, val bottom: Float)
