package akniazev.ui

import akniazev.common.Controller
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * Dialog, prompting user for FTP location and credentials.
 * Disables the 'Connect' button if provided invalid input.
 *
 * @param frame parent frame
 * @param title title of the dialog
 * @param controller instance of [Controller] used by the application
 */
class ConnectFtpDialog(frame: Frame, title: String, controller: Controller) : JDialog(frame, title) {

    private val connectBtn = createButton("Connect").apply { isEnabled = false }
    private val closeBtn = createButton("Close")

    private val host = createTextField(20)
    private val port = createTextField(5)
    private val user = createTextField(20)
    private val password = JPasswordField(20).apply { font = REGULAR_FONT;  minimumSize = TEXT_FIELD_DIMENSION }

    private lateinit var validators: List<Validator<*>>

    init {
        size = Dimension(300, 300)
        contentPane.background = Color(173, 174, 192)
        setLocationRelativeTo(parent)

        layout = GridBagLayout()
        val constraints = GridBagConstraints()

        constraints.gridy = 0
        constraints.weightx = 1.toDouble()
        constraints.weighty = 1.toDouble()
        constraints.fill = GridBagConstraints.CENTER

        // First row
        constraints.gridx = 0
        add(createLabel("Host "), constraints)
        constraints.gridx++
        add(host, constraints)

        // Second row
        constraints.gridy++
        constraints.gridx = 0
        add(createLabel("Port "), constraints)
        constraints.gridx++
        add(port, constraints)

        // Third row
        constraints.gridy++
        constraints.gridx = 0
        add(createLabel("User "), constraints)
        constraints.gridx++
        add(user, constraints)

        // Fourth row
        constraints.gridy++
        constraints.gridx = 0
        add(createLabel("Password "), constraints)
        constraints.gridx++
        add(password, constraints)

        // Last row
        constraints.gridx = 0
        constraints.gridy++
        add(closeBtn, constraints)

        constraints.gridx++
        add(connectBtn, constraints)

        initPlaceholders()
        initValidation()

        connectBtn.addActionListener {
            controller.connectToFtp(host.text, port.text, user.text, String(password.password))
        }
        closeBtn.addActionListener { isVisible = false }

        connectBtn.name = "connectBtn"
        host.name = "host"
        port.name = "port"
        user.name = "user"
        password.name = "password"
    }

    private fun initPlaceholders() {
        val hostPlaceholder = "hostname or IP"
        val portPlaceholder = "1 - 65535"

        host.text = hostPlaceholder
        port.text = portPlaceholder

        host.foreground = Color.GRAY
        port.foreground = Color.GRAY

        host.addFocusListener(PlaceholderListener(hostPlaceholder))
        port.addFocusListener(PlaceholderListener(portPlaceholder))
    }

    private fun initValidation() {
        val ipRegex = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\$".toRegex()
        val hostRegex = "^[.0-9A-Za-z]+\\.[0-9A-Za-z]+\$".toRegex()

        val usernameValidator: Validator<JTextField> = Validator(user, true) { true }
        val hostValidator: Validator<JTextField> = Validator(host) {
            it.text.isNotEmpty()
                    && (it.text == "localhost" || ipRegex.matches(it.text) || hostRegex.matches(it.text))
        }
        val portValidator: Validator<JTextField> = Validator(port) {
            it.text.isNotEmpty()
                    && it.text.fold(true) { acc, char -> acc && char.isDigit()}
                    && it.text.toInt() <= 65535
        }
        val passwordValidator: Validator<JPasswordField> = Validator(password, true) {
            if (user.text.isEmpty()) it.password.isEmpty()
            else true
        }

        validators = listOf(hostValidator, portValidator, usernameValidator, passwordValidator)

        host.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                hostValidator.validate()
                updateButton()
            }
        })

        port.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                portValidator.validate()
                updateButton()
            }
        })

        user.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                usernameValidator.validate()
                passwordValidator.validate()
                updateButton()
            }
        })
        password.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                usernameValidator.validate()
                passwordValidator.validate()
                updateButton()
            }
        })
    }

    private fun updateButton() {
        connectBtn.isEnabled = validators.all { it.valid }
    }
}
