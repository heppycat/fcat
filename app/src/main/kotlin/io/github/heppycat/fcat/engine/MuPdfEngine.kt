package io.github.heppycat.fcat.engine

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.SeekableInputStream
import com.artifex.mupdf.fitz.SeekableStream
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/** The engine the app uses. The one place to switch engines during the M1 bake-off. */
fun defaultPdfEngine(): PdfEngine = MuPdfEngine

/**
 * MuPDF 1.28 via its official Java binding. The binding gives each thread its own cloned context, but a
 * document must still not be used from two threads at once, hence one engine thread.
 */
internal object MuPdfEngine : PdfEngine {
    override val dispatcher =
        Executors.newSingleThreadExecutor { Thread(it, "fcat-mupdf") }.asCoroutineDispatcher()

    override fun open(file: ParcelFileDescriptor, password: String?): PdfDocument {
        try {
            val doc = Document.openDocument(FdSeekableStream(file), "application/pdf")
            try {
                if (doc.needsPassword()) {
                    if (password == null) throw PdfPasswordException("Password required")
                    if (!doc.authenticatePassword(password)) throw PdfPasswordException("Wrong password")
                }
                return MuPdfDocument(doc, file)
            } catch (e: Throwable) {
                doc.destroy()
                throw e
            }
        } catch (e: Throwable) {
            file.close()
            throw e
        }
    }
}

private class MuPdfDocument(private val doc: Document, private val file: ParcelFileDescriptor) : PdfDocument {
    override val pageCount: Int = doc.countPages()

    override fun pageSize(index: Int): PageSize = withPage(index) { page ->
        val b = page.bounds
        PageSize(b.x1 - b.x0, b.y1 - b.y0)
    }

    override fun renderRegion(index: Int, region: Rect, scale: Float, target: Bitmap) {
        require(target.config == Bitmap.Config.ARGB_8888 && target.isMutable) { "target must be mutable ARGB_8888" }
        require(target.width >= region.width() && target.height >= region.height()) { "target smaller than region" }
        withPage(index) { page ->
            val b = page.bounds
            // Page space → pixels of the whole page with its top-left corner at (0, 0). The device then
            // places the bitmap's (0, 0) at region's top-left, so only that region is drawn.
            val ctm = Matrix(scale, 0f, 0f, scale, -b.x0 * scale, -b.y0 * scale)
            val device = AndroidDrawDevice(target, region.left, region.top, 0, 0, region.width(), region.height())
            try {
                page.run(device, ctm, null)
                device.close()
            } finally {
                device.destroy()
            }
        }
    }

    override fun outline(): List<OutlineItem> = outlineItems(doc.loadOutline()) { item ->
        // A malformed link target should cost one entry its target, not the whole outline.
        try {
            doc.pageNumberFromLocation(doc.resolveLink(item))
        } catch (e: RuntimeException) {
            -1
        }
    }

    override fun pageText(index: Int): PageText = withPage(index) { page ->
        val b = page.bounds
        val text = page.toStructuredText()
        try {
            val lines = text.blocks.flatMap { block ->
                block.lines.map { line ->
                    val s = StringBuilder(line.chars.size)
                    for (c in line.chars) s.appendCodePoint(c.c)
                    val box = line.bbox
                    TextLine(s.toString(), box.x0 - b.x0, box.y0 - b.y0, box.x1 - b.x0, box.y1 - b.y0)
                }
            }
            PageText(lines)
        } finally {
            text.destroy()
        }
    }

    override fun close() {
        doc.destroy()
        file.close()
    }

    private inline fun <T> withPage(index: Int, block: (Page) -> T): T {
        if (index !in 0 until pageCount) throw IndexOutOfBoundsException("page $index of $pageCount")
        val page = doc.loadPage(index)
        try {
            return block(page)
        } finally {
            page.destroy()
        }
    }
}

internal fun outlineItems(nodes: Array<Outline?>?, pageOf: (Outline) -> Int): List<OutlineItem> =
    nodes.orEmpty().filterNotNull().map { OutlineItem(it.title.orEmpty(), pageOf(it), outlineItems(it.down, pageOf)) }

/** Reads the descriptor in place, so opening a large file does not copy it into memory. */
private class FdSeekableStream(file: ParcelFileDescriptor) : SeekableInputStream {
    // Not closed here: the stream does not own the descriptor, and closing the channel would close it.
    private val channel = FileInputStream(file.fileDescriptor).channel

    // MuPDF treats 0 as an error and a negative count as end of file, which is FileChannel's contract too.
    override fun read(buf: ByteArray): Int = channel.read(ByteBuffer.wrap(buf))

    override fun seek(offset: Long, whence: Int): Long {
        val target = seekTarget(offset, whence, channel.position(), channel.size())
        channel.position(target)
        return target
    }

    override fun position(): Long = channel.position()
}

internal fun seekTarget(offset: Long, whence: Int, position: Long, size: Long): Long {
    val base = when (whence) {
        SeekableStream.SEEK_SET -> 0L
        SeekableStream.SEEK_CUR -> position
        SeekableStream.SEEK_END -> size
        else -> throw IOException("Unknown whence $whence")
    }
    val target = base + offset
    if (target < 0) throw IOException("Seek to negative position $target")
    return target
}
