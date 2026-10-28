package io.github.heppycat.fcat.engine

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * PDFium (the bblanchon/pdfium-binaries build pinned in libs.versions.toml) through our own JNI in src/main/cpp.
 * "None of the PDFium APIs are thread-safe" (fpdfview.h), so every call for every document runs on one engine
 * thread.
 */
internal object PdfiumEngine : PdfEngine {
    override val dispatcher =
        Executors.newSingleThreadExecutor { Thread(it, "fcat-pdfium") }.asCoroutineDispatcher()

    // Only touched on the engine thread.
    private var initialized = false

    override fun open(file: ParcelFileDescriptor, password: String?): PdfDocument {
        try {
            if (!initialized) {
                PdfiumNative.init()
                initialized = true
            }
            val handle = PdfiumNative.open(file.fd, password)
            if (handle <= 0) throw openError(-handle, password)
            return PdfiumDocument(handle, file)
        } catch (e: Throwable) {
            file.close()
            throw e
        }
    }
}

private class PdfiumDocument(private val handle: Long, private val file: ParcelFileDescriptor) : PdfDocument {
    override val pageCount: Int = PdfiumNative.pageCount(handle)

    override fun pageSize(index: Int): PageSize {
        checkIndex(index)
        val out = FloatArray(2)
        if (!PdfiumNative.pageSize(handle, index, out)) throw IOException("Cannot read the size of page $index")
        return PageSize(out[0], out[1])
    }

    override fun renderRegion(index: Int, region: Rect, scale: Float, target: Bitmap) {
        require(target.config == Bitmap.Config.ARGB_8888 && target.isMutable) { "target must be mutable ARGB_8888" }
        require(target.width >= region.width() && target.height >= region.height()) { "target smaller than region" }
        val size = pageSize(index)
        withPage(index) { page ->
            val drawn = PdfiumNative.render(
                page, target, region.left, region.top, region.width(), region.height(),
                (size.width * scale).roundToInt(), (size.height * scale).roundToInt(),
            )
            if (!drawn) throw IOException("Cannot render page $index")
        }
    }

    override fun outline(): List<OutlineItem> = bookmarks(0L, HashSet())

    // Malformed outlines can link back to an earlier entry, so anything already visited ends the walk.
    private fun bookmarks(parent: Long, seen: MutableSet<Long>): List<OutlineItem> {
        val items = ArrayList<OutlineItem>()
        var bookmark = PdfiumNative.bookmarkFirstChild(handle, parent)
        while (bookmark != 0L && seen.add(bookmark)) {
            items += OutlineItem(
                PdfiumNative.bookmarkTitle(bookmark),
                PdfiumNative.bookmarkPageIndex(handle, bookmark),
                bookmarks(bookmark, seen),
            )
            bookmark = PdfiumNative.bookmarkNextSibling(handle, bookmark)
        }
        return items
    }

    override fun pageText(index: Int): PageText = withPage(index) { page ->
        val frame = FloatArray(5)
        if (!PdfiumNative.pageFrame(page, frame)) throw IOException("Cannot read the geometry of page $index")
        val chars = PdfiumNative.pageChars(page) ?: throw IOException("Cannot read the text of page $index")
        textLines(chars, PageFrame(frame[0], frame[1], frame[2], frame[3], frame[4].toInt()))
    }

    override fun close() {
        PdfiumNative.close(handle)
        file.close()
    }

    private fun checkIndex(index: Int) {
        if (index !in 0 until pageCount) throw IndexOutOfBoundsException("page $index of $pageCount")
    }

    private inline fun <T> withPage(index: Int, block: (Long) -> T): T {
        checkIndex(index)
        val page = PdfiumNative.loadPage(handle, index)
        if (page == 0L) throw IOException("Cannot load page $index")
        try {
            return block(page)
        } finally {
            PdfiumNative.closePage(page)
        }
    }
}

// FPDF_ERR_* in fpdfview.h.
private const val ERR_FILE = 2L
private const val ERR_FORMAT = 3L
private const val ERR_PASSWORD = 4L
private const val ERR_SECURITY = 5L

internal fun openError(code: Long, password: String?): Exception = when (code) {
    ERR_PASSWORD -> PdfPasswordException(if (password == null) "Password required" else "Wrong password")
    ERR_FILE -> IOException("Cannot read the file")
    ERR_FORMAT -> IOException("Not a PDF, or damaged")
    ERR_SECURITY -> IOException("Unsupported encryption")
    else -> IOException("PDFium error $code")
}

/**
 * The visible page box in PDF user space (y up) and the page's rotation in clockwise quarter turns. Maps user
 * space to points from the top-left corner of the page as displayed, which is what [TextLine] uses.
 */
internal class PageFrame(
    private val left: Float,
    private val bottom: Float,
    private val right: Float,
    private val top: Float,
    private val rotation: Int,
) {
    fun x(ux: Float, uy: Float): Float = when (rotation) {
        1 -> uy - bottom
        2 -> right - ux
        3 -> top - uy
        else -> ux - left
    }

    fun y(ux: Float, uy: Float): Float = when (rotation) {
        1 -> ux - left
        2 -> uy - bottom
        3 -> right - ux
        else -> top - uy
    }
}

/**
 * Splits PdfiumNative.pageChars output (six floats per character, see pdfium_jni.cpp) into lines at line breaks.
 * Characters PDFium generated, such as the spaces and line breaks it infers, count as text but have no box.
 */
internal fun textLines(chars: FloatArray, frame: PageFrame): PageText {
    val lines = ArrayList<TextLine>()
    val text = StringBuilder()
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY

    fun endLine() {
        if (text.isNotBlank() && left <= right) lines += TextLine(text.toString(), left, top, right, bottom)
        text.setLength(0)
        left = Float.POSITIVE_INFINITY
        top = Float.POSITIVE_INFINITY
        right = Float.NEGATIVE_INFINITY
        bottom = Float.NEGATIVE_INFINITY
    }

    for (i in 0 until chars.size / 6) {
        val o = i * 6
        val code = chars[o].toInt()
        if (code == '\r'.code || code == '\n'.code) {
            endLine()
            continue
        }
        text.appendCodePoint(if (code == 0) 0xFFFD else code)
        if (chars[o + 1] != 0f) continue
        // Map two opposite corners; rotation by quarter turns keeps the box axis-aligned.
        val x1 = frame.x(chars[o + 2], chars[o + 3])
        val y1 = frame.y(chars[o + 2], chars[o + 3])
        val x2 = frame.x(chars[o + 4], chars[o + 5])
        val y2 = frame.y(chars[o + 4], chars[o + 5])
        left = min(left, min(x1, x2))
        right = max(right, max(x1, x2))
        top = min(top, min(y1, y2))
        bottom = max(bottom, max(y1, y2))
    }
    endLine()
    return PageText(lines)
}

/** See pdfium_jni.cpp for each function's contract. Handles are native pointers; 0 means none. */
internal object PdfiumNative {
    init {
        System.loadLibrary("fcatpdfium")
    }

    @JvmStatic external fun init()

    /** A document handle, or minus an FPDF_ERR_* code. */
    @JvmStatic external fun open(fd: Int, password: String?): Long

    @JvmStatic external fun close(doc: Long)

    @JvmStatic external fun pageCount(doc: Long): Int

    @JvmStatic external fun pageSize(doc: Long, index: Int, out: FloatArray): Boolean

    @JvmStatic external fun loadPage(doc: Long, index: Int): Long

    @JvmStatic external fun closePage(page: Long)

    @JvmStatic external fun pageFrame(page: Long, out: FloatArray): Boolean

    @JvmStatic external fun render(
        page: Long,
        target: Bitmap,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        pageWidth: Int,
        pageHeight: Int,
    ): Boolean

    @JvmStatic external fun pageChars(page: Long): FloatArray?

    @JvmStatic external fun bookmarkFirstChild(doc: Long, parent: Long): Long

    @JvmStatic external fun bookmarkNextSibling(doc: Long, bookmark: Long): Long

    @JvmStatic external fun bookmarkTitle(bookmark: Long): String

    @JvmStatic external fun bookmarkPageIndex(doc: Long, bookmark: Long): Int
}
