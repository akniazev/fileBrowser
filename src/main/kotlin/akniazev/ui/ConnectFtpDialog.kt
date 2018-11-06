package akniazev.ui

import akniazev.common.Controller
import akniazev.common.REGULAR_FONT
import akniazev.common.TEXT_FIELD_DIMENSION
import akniazev.controller.createButton
import akniazev.controller.createLabel
import akniazev.controller.createTextField
import akniazev.controller.notifyOnError
import java.awt.Dimension
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JDialog
import javax.swing.JPasswordField

class ConnectFtpDialog(frame: Frame, title: String, controller: Controller) : JDialog(frame, title) {

    private val connectBtn = createButton("Connect")
    private val closeBtn = createButton("Close")

    private val host = createTextField(20)
    private val port = createTextField(5)
    private val user = createTextField(20)
    private val password = JPasswordField(20).apply { font = REGULAR_FONT;  minimumSize = TEXT_FIELD_DIMENSION }


    init {
        size = Dimension(300, 300)
        setLocationRelativeTo(parent)

        layout = GridBagLayout()
        val constraints = GridBagConstraints()

        constraints.gridy = 0
        constraints.weightx = 1.toDouble()
        constraints.weighty = 1.toDouble()
        constraints.fill = GridBagConstraints.CENTER

        // First row
        constraints.gridx = 0
        add(createLabel("Host: "), constraints)
        constraints.gridx++
        add(host, constraints)

        // Second row
        constraints.gridy++
        constraints.gridx = 0
        add(createLabel("Port: "), constraints)
        constraints.gridx++
        add(port, constraints)

        // Third row
        constraints.gridy++
        constraints.gridx = 0
        add(createLabel("User: "), constraints)
        constraints.gridx++
        add(user, constraints)

        // Fourth row
        constraints.gridy++
        constraints.gridx = 0
        add(createLabel("Password: "), constraints)
        constraints.gridx++
        add(password, constraints)


        // Last row
        constraints.gridx = 0
        constraints.gridy++
        add(closeBtn, constraints)

        constraints.gridx++
        add(connectBtn, constraints)

        connectBtn.addActionListener {
            notifyOnError(this) {
                controller.connectToFtp(host.text, port.text, user.text, String(password.password)) // :(
                isVisible = false
            }
        }
        closeBtn.addActionListener { isVisible = false }
    }

}