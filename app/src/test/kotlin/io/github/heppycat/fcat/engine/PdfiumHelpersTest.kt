package io.github.heppycat.fcat.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PdfiumHelpersTest {
    // A 200 x 100 pt page box whose user-space origin is offset to (10, 20).
    private fun frame(rotation: Int) = PageFrame(left = 10f, bottom = 20f, right = 210f, top = 120f, rotation)

    private fun corner(frame: PageFrame, x: Float, y: Float) = frame.x(x, y) to frame.y(x, y)

    @Test
    fun `unrotated page maps the top-left corner to the origin, y down`() {
        val f = frame(0)
        assertEquals(0f to 0f, corner(f, 10f, 120f))
        assertEquals(200f to 100f, corner(f, 210f, 20f))
    }

    @Test
    fun `quarter turns move the matching corner of the box to the displayed top-left`() {
        // Clockwise rotation: 90 brings the bottom-left up, 180 the bottom-right, 270 the top-right.
        assertEquals(0f to 0f, corner(frame(1), 10f, 20f))
        assertEquals(0f to 0f, corner(frame(2), 210f, 20f))
        assertEquals(0f to 0f, corner(frame(3), 210f, 120f))
        // And the displayed page is 100 x 200 when rotated by a quarter turn.
        assertEquals(100f to 200f, corner(frame(1), 210f, 120f))
        assertEquals(200f to 100f, corner(frame(2), 10f, 120f))
        assertEquals(100f to 200f, corner(frame(3), 10f, 20f))
    }

    private fun char(c: Char, l: Float, b: Float, r: Float, t: Float, generated: Boolean = false) =
        floatArrayOf(c.code.toFloat(), if (generated) 1f else 0f, l, b, r, t)

    private fun chars(vararg cs: FloatArray) = cs.reduce(FloatArray::plus)

    @Test
    fun `splits lines at generated line breaks and unions character boxes`() {
        val text = textLines(
            chars(
                char('H', 10f, 100f, 20f, 115f),
                char('i', 20f, 100f, 25f, 118f),
                char(' ', 0f, 0f, 0f, 0f, generated = true),
                char('!', 30f, 99f, 35f, 115f),
                char('\r', 0f, 0f, 0f, 0f, generated = true),
                char('\n', 0f, 0f, 0f, 0f, generated = true),
                char('x', 10f, 60f, 18f, 70f),
            ),
            frame(0),
        )
        assertEquals(
            listOf(TextLine("Hi !", 0f, 2f, 25f, 21f), TextLine("x", 0f, 50f, 8f, 60f)),
            text.lines,
        )
    }

    @Test
    fun `drops lines that are blank or have no real character`() {
        val text = textLines(
            chars(
                char(' ', 0f, 0f, 0f, 0f, generated = true),
                char('\n', 0f, 0f, 0f, 0f, generated = true),
                char(' ', 10f, 60f, 18f, 70f),
            ),
            frame(0),
        )
        assertTrue(text.lines.isEmpty())
    }

    @Test
    fun `unknown characters become the replacement character`() {
        val text = textLines(floatArrayOf(0f, 0f, 10f, 100f, 20f, 115f), frame(0))
        assertEquals("�", text.lines.single().text)
    }

    @Test
    fun `open errors map to password or IO exceptions`() {
        assertEquals("Password required", openError(4, null).message)
        assertEquals("Wrong password", openError(4, "x").message)
        assertTrue(openError(4, null) is PdfPasswordException)
        assertTrue(openError(3, null) is IOException)
        assertTrue(openError(99, null) is IOException)
    }
}
