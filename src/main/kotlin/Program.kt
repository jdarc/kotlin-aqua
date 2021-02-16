import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities

object Program {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater {
            val frame = JFrame("Aqua")
            frame.layout = BorderLayout()
            frame.contentPane.add(AquaPanel(), BorderLayout.CENTER)
            frame.pack()
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
            frame.isResizable = false
        }
    }
}
