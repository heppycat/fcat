package io.github.heppycat.fcat.engine

import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.SeekableStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MuPdfHelpersTest {
    @Test
    fun `seek is relative to start, current position or end`() {
        assertEquals(7L, seekTarget(7, SeekableStream.SEEK_SET, position = 3, size = 10))
        assertEquals(5L, seekTarget(2, SeekableStream.SEEK_CUR, position = 3, size = 10))
        assertEquals(1L, seekTarget(-2, SeekableStream.SEEK_CUR, position = 3, size = 10))
        assertEquals(8L, seekTarget(-2, SeekableStream.SEEK_END, position = 3, size = 10))
    }

    @Test
    fun `seek past the end is allowed, the next read reports end of file`() {
        assertEquals(15L, seekTarget(5, SeekableStream.SEEK_END, position = 0, size = 10))
    }

    @Test(expected = IOException::class)
    fun `seek before the start fails`() {
        seekTarget(-11, SeekableStream.SEEK_END, position = 0, size = 10)
    }

    @Test(expected = IOException::class)
    fun `unknown whence fails`() {
        seekTarget(0, 42, position = 0, size = 10)
    }

    @Test
    fun `outline keeps order and nesting and resolves each entry`() {
        val tree = arrayOf<Outline?>(
            Outline("A", "#page=1", arrayOf(Outline("A.1", "#page=2", null))),
            Outline("B", "https://example.org/", null),
        )
        val pages = mapOf("#page=1" to 0, "#page=2" to 1)

        val items = outlineItems(tree) { pages[it.uri] ?: -1 }

        assertEquals(
            listOf(
                OutlineItem("A", 0, listOf(OutlineItem("A.1", 1, emptyList()))),
                OutlineItem("B", -1, emptyList()),
            ),
            items,
        )
    }

    @Test
    fun `missing outline, null entries and null titles are tolerated`() {
        assertTrue(outlineItems(null) { 0 }.isEmpty())
        val items = outlineItems(arrayOf(null, Outline(null, null, arrayOf(null)))) { -1 }
        assertEquals(listOf(OutlineItem("", -1, emptyList())), items)
    }
}
