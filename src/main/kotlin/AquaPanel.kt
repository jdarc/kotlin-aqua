import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.awt.image.PixelGrabber
import javax.imageio.ImageIO
import javax.swing.JPanel
import kotlin.math.*

class AquaPanel : JPanel() {
    private val texels = loadImagePixels()
    private val image = BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB_PRE)
    private val pixels = (image.raster.dataBuffer as DataBufferInt).data
    private val buffers = Array(2) { IntArray(pixels.size) }
    private var flag = 0

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.drawImage(image, 0, 0, width, height, this)
    }

    private fun handleMouseDown(e: MouseEvent) =
        sineBlob(buffers[flag], floor(e.x * 512.0 / width).toInt(), floor(e.y * 512.0 / height).toInt(), 24, 64)

    private fun blendWater() {
        val width = image.width
        val height = image.height
        val srcBuffer = buffers[flag]
        for (y in 0 until height) {
            val scan = y * width
            for (x in 0 until width) {
                val m1 = scan + max(0, x - 1)
                val m2 = scan + min(width - 1, x + 1)
                val m3 = max(0, y - 1) * width + x
                val m4 = min(height - 1, y + 1) * width + x
                val offsetX = srcBuffer[m1] - srcBuffer[m2]
                val offsetY = srcBuffer[m3] - srcBuffer[m4]
                val tu = (x + offsetX).coerceIn(0, 511)
                val tv = (y + offsetY).coerceIn(0, 511)
                val argb = texels[tv * width + tu]
                val red = ((255 and argb.shr(16)) - offsetX.shr(1)).coerceIn(0, 255).shl(16)
                val grn = ((255 and argb.shr(8)) - offsetX.shr(1)).coerceIn(0, 255).shl(8)
                val blu = ((255 and argb) - offsetX.shr(1)).coerceIn(0, 255)
                pixels[scan + x] = 0xFF.shl(24) or red or grn or blu
            }
        }
    }

    private fun processWater() {
        val width = image.width
        val dst = buffers[flag]
        val src = buffers[1 - flag]
        for (i in width + 1 until src.size - (width + 1)) {
            dst[i] = (src[i - 1] + src[i + 1] + src[i - width] + src[i + width] shr 1) - dst[i]
            dst[i] -= dst[i] shr 6
        }
        flag = 1 - flag
    }

    private fun sineBlob(dst: IntArray, x: Int, y: Int, radius: Int, height: Int) {
        var left = -radius
        var top = -radius
        var right = radius
        var bottom = radius
        if (x - radius < 1) left -= x - radius - 1
        if (y - radius < 1) top -= y - radius - 1
        if (x + radius > image.width - 1) right -= x + radius - image.width + 1
        if (y + radius > image.height - 1) bottom -= y + radius - image.height + 1
        for (cy in top until bottom) {
            for (cx in left until right) {
                val square = sqrt((cy * cy + cx * cx).toDouble())
                if (square < radius) dst[(y + cy) * image.width + x + cx] += (height * cos(square / radius)).toInt()
            }
        }
    }

    private fun loadImagePixels(): IntArray {
        val image = ImageIO.read(AquaPanel::class.java.classLoader.getResource("pool.jpg"))
        val pixels = IntArray(image.width * image.height)
        PixelGrabber(image, 0, 0, image.width, image.height, pixels, 0, image.width).grabPixels()
        return pixels
    }

    init {
        size = Dimension(800, 800)
        preferredSize = size

        Toolkit.getDefaultToolkit().systemEventQueue.push(object : EventQueue() {
            override fun dispatchEvent(event: AWTEvent) {
                super.dispatchEvent(event)
                if (peekEvent() == null) {
                    blendWater()
                    processWater()
                    repaint()
                }
            }
        })

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) = handleMouseDown(e)
        })

        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent) = handleMouseDown(e)
        })
    }
}
