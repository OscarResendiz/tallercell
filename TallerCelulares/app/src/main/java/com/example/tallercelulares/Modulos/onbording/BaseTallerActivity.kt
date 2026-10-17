package com.example.tallercelulares.Modulos.onbording

import android.graphics.Color
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SearchView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tallercelulares.ConfiguracionInicial.RepositorioConfiguracionInicial
import com.example.tallercelulares.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/** Reutiliza el marco visual y los controles de formulario entre el acceso y el asistente. */
abstract class BaseTallerActivity : AppCompatActivity() {
    protected lateinit var root: android.widget.FrameLayout
    protected val fieldValues = linkedMapOf<String, String>()
    protected val editFields = mutableMapOf<String, EditText>()
    protected val fieldLayouts = mutableMapOf<String, TextInputLayout>()

    protected val DARK = Color.rgb(20, 32, 48)
    protected val MUTED = Color.rgb(91, 105, 122)
    protected val TEAL = Color.rgb(0, 117, 112)
    protected val ERROR = Color.rgb(176, 42, 55)

    // Centraliza el tema de seguridad y los insets para que ambas pantallas se comporten igual.
    protected fun initializeScreen() {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    protected fun addInput(
        container: LinearLayout,
        key: String,
        label: String,
        default: String,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        password: Boolean = false
    ) {
        val layout = TextInputLayout(this).apply {
            hint = label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxStrokeColor(TEAL)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            if (password) endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }
        val editText = TextInputEditText(layout.context).apply {
            this.inputType = inputType
            setText(fieldValues[key] ?: default)
            setTextColor(DARK)
            textSize = 16f
            if (password) {
                transformationMethod = PasswordTransformationMethod.getInstance()
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                isLongClickable = false
            }
            if (key == "nombre" || key == "direccion") {
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
            }
        }
        layout.addView(editText)
        container.addView(layout)
        editFields[key] = editText
        fieldLayouts[key] = layout
    }

    protected fun addSelectionDropdown(
        container: LinearLayout,
        key: String,
        label: String,
        selectedValue: String,
        options: List<SelectionOption>
    ) {
        val selectedOption = options.firstOrNull { it.value == fieldValues[key] }
            ?: options.firstOrNull { it.value == selectedValue }
            ?: options.firstOrNull()
        val layout = TextInputLayout(this).apply {
            hint = label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxStrokeColor(TEAL)
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            setEndIconDrawable(android.R.drawable.arrow_down_float)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
        val selectedText = TextInputEditText(this).apply {
            setText(selectedOption?.label.orEmpty())
            tag = selectedOption?.value
            setTextColor(DARK)
            textSize = 16f
            isFocusable = false
            isClickable = true
            showSoftInputOnFocus = false
            contentDescription = "$label: ${selectedOption?.label.orEmpty()}. Toca para elegir otra opción."
            setOnClickListener { showSelectionDialog(layout, key, label, options, this) }
        }
        layout.addView(selectedText)
        layout.setEndIconOnClickListener {
            showSelectionDialog(layout, key, label, options, selectedText)
        }
        selectedOption?.let { fieldValues[key] = it.value }
        container.addView(layout)
        editFields[key] = selectedText
        fieldLayouts[key] = layout
    }

    protected fun showSelectionDialog(
        fieldLayout: TextInputLayout,
        fieldKey: String,
        title: String,
        options: List<SelectionOption>,
        selectedText: TextInputEditText
    ) {
        if (options.isEmpty()) {
            fieldLayout.error = "No hay opciones disponibles en el catálogo local."
            return
        }

        val searchView = SearchView(this).apply {
            queryHint = "Buscar en ${options.size} opciones"
            isIconified = false
            clearFocus()
        }
        val listView = ListView(this).apply {
            dividerHeight = dp(1)
            setPadding(0, dp(4), 0, dp(4))
        }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            options.toMutableList()
        )
        listView.adapter = adapter

        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), dp(8))
            addView(
                searchView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                listView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(420)
                )
            )
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogContent)
            .setNegativeButton("Cerrar", null)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val option = adapter.getItem(position)
            if (option != null) {
                selectedText.setText(option.label)
                selectedText.contentDescription =
                    "$title: ${option.label}. Toca para elegir otra opción."
                selectedText.tag = option.value
                fieldValues[fieldKey] = option.value
                fieldLayout.error = null
                dialog.dismiss()
            }
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText.orEmpty())
                return true
            }
        })
        dialog.setOnShowListener {
            val selectedIndex = options.indexOfFirst { it.value == fieldValues[fieldKey] }
            if (selectedIndex >= 0) listView.setSelection(selectedIndex)
        }
        dialog.show()
    }

    protected fun render(
        title: String,
        subtitle: String,
        step: String?,
        primary: String?,
        secondary: String? = null,
        onPrimary: (() -> Unit)? = null,
        onSecondary: (() -> Unit)? = null,
        content: LinearLayout.() -> Unit
    ) {
        root.removeAllViews()
        editFields.clear()
        fieldLayouts.clear()
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(16))
        }
        addText(header, getString(R.string.brand_name), color = TEAL, size = 12f, bold = true)
        addSpace(header, 8)
        addText(header, title, color = DARK, size = 26f, bold = true)
        addSpace(header, 6)
        addText(header, subtitle, color = MUTED, size = 15f)
        step?.let {
            addSpace(header, 12)
            addText(header, it, color = TEAL, size = 12f, bold = true)
        }
        screen.addView(header)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(16))
        }
        body.content()
        scrollView.addView(body)
        screen.addView(
            scrollView,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        if (primary != null || secondary != null) {
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(4))
            }
            if (secondary != null) {
                actions.addView(
                    MaterialButton(this).apply {
                        text = secondary
                        isAllCaps = false
                        setTextColor(TEAL)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                        setOnClickListener { onSecondary?.invoke() }
                    },
                    LinearLayout.LayoutParams(0, dp(52), 1f)
                )
            }
            if (primary != null) {
                actions.addView(
                    MaterialButton(this).apply {
                        text = primary
                        isAllCaps = false
                        setTextColor(Color.WHITE)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(TEAL)
                        cornerRadius = dp(14)
                        setOnClickListener { onPrimary?.invoke() }
                    },
                    LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        if (secondary == null) 1f else 1.4f
                    )
                )
            }
            screen.addView(actions)
        }
        root.addView(screen)
    }

    protected fun addCard(container: LinearLayout, title: String, body: String) {
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.rgb(229, 233, 239)
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
        }
        addText(content, title, color = DARK, size = 16f, bold = true)
        addSpace(content, 6)
        addText(content, body, color = MUTED, size = 14f)
        card.addView(content)
        container.addView(card)
    }

    protected fun addSectionTitle(container: LinearLayout, title: String) {
        val text = TextView(this).apply {
            this.text = title
            textSize = 17f
            setTextColor(DARK)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(10), 0, dp(10))
        }
        container.addView(text)
    }

    protected fun addText(
        container: LinearLayout,
        value: String,
        color: Int,
        size: Float = 14f,
        bold: Boolean = false
    ) {
        val text = TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        container.addView(text)
    }

    protected fun addSpace(container: LinearLayout, height: Int) {
        container.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    protected fun showMessage(message: String) {
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show()
    }

    protected fun methodLabel(method: String): String = when (method) {
        RepositorioConfiguracionInicial.METODO_CONTRASENA -> getString(R.string.password_option)
        RepositorioConfiguracionInicial.METODO_CREDENCIAL_ANDROID -> getString(R.string.device_option)
        RepositorioConfiguracionInicial.METODO_BIOMETRIA_ANDROID -> getString(R.string.biometric_option)
        else -> "Sin método"
    }

    protected fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

data class SelectionOption(val value: String, val label: String) {
    override fun toString(): String = label
}
