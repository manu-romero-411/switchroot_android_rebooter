package com.switchroot.rebootmanager.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.DynamicColors
import com.switchroot.rebootmanager.Prefs
import com.switchroot.rebootmanager.R
import com.switchroot.rebootmanager.databinding.ActivityMainBinding
import com.switchroot.rebootmanager.ini.BootEntryRepository
import com.switchroot.rebootmanager.model.BootEntry
import com.switchroot.rebootmanager.root.RootActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private lateinit var repository: BootEntryRepository
    private lateinit var adapter: BootEntryAdapter

    private var iniFolderEntries: List<BootEntry.HekateEntry> = emptyList()
    private var hekateIplEntries: List<BootEntry.HekateEntry> = emptyList()

    // Selector de carpeta (Storage Access Framework). Se usa solo en el primer
    // arranque, o si la URI guardada ha dejado de ser válida.
    private val openSdRoot = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            // El usuario ha cancelado el selector: sin carpeta no hay nada que mostrar.
            finish()
            return@registerForActivityResult
        }
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        prefs.sdRootUri = uri
        loadEntries()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica la paleta dinámica "Material You" del sistema en Android 12+;
        // debe llamarse antes de super.onCreate()/setContentView().
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)

        prefs = Prefs(this)
        repository = BootEntryRepository(this)

        if (!isRunningOnSwitch()) {
            Toast.makeText(this, R.string.error_unsupported_device, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Configuración de ventana estilo popup/diálogo
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.6f)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupList()
        setupToggle()
        ensureRoot()

        val sdRoot = prefs.sdRootUri
        if (sdRoot == null) {
            // Primer arranque: pedir dónde está montada la partición principal de la SD.
            Toast.makeText(this, R.string.picker_prompt, Toast.LENGTH_LONG).show()
            openSdRoot.launch(null)
        } else {
            loadEntries()
        }
    }

    override fun onStart() {
        super.onStart()
        // Dimensionar la ventana: 85% ancho, y alto automático según contenido (wrap_content)
        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels * 0.85).toInt()
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
    }

    private fun isRunningOnSwitch(): Boolean =
        Build.MANUFACTURER.equals("NINTENDO", ignoreCase = true)

    private fun ensureRoot() {
        lifecycleScope.launch(Dispatchers.IO) {
            val granted = RootActions.hasRoot()
            if (!granted) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, R.string.error_root_required, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupList() {
        adapter = BootEntryAdapter { item -> onEntrySelected(item) }
        binding.bootEntryList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.bootEntryList.adapter = adapter
    }

    private fun setupToggle() {
        binding.toggleHekateIpl.isChecked = prefs.showHekateIplEntries
        binding.toggleHekateIpl.setOnCheckedChangeListener { _, isChecked ->
            prefs.showHekateIplEntries = isChecked
            renderList()
        }
    }

    private fun loadEntries() {
        val sdRoot = prefs.sdRootUri ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { repository.readEntries(sdRoot) }
            result.error?.let { error ->
                val messageRes = when (error) {
                    BootEntryRepository.ErrorType.CANNOT_OPEN_FOLDER -> R.string.error_open_folder_failed
                    BootEntryRepository.ErrorType.NO_BOOTLOADER_FOLDER -> R.string.error_no_bootloader_folder
                }
                Toast.makeText(this@MainActivity, messageRes, Toast.LENGTH_LONG).show()
            }
            iniFolderEntries = result.iniFolderEntries
            hekateIplEntries = result.hekateIplEntries
            renderList()
        }
    }

    private fun renderList() {
        val items = mutableListOf<BootEntry>()
        // Las opciones de sistema primero, como ha pedido el usuario
        items += BootEntry.SystemAction(BootEntry.SystemAction.Action.SHUTDOWN)
        items += BootEntry.SystemAction(BootEntry.SystemAction.Action.RESTART)
        
        // Filtro para ocultar entradas que contengan "android" o "lineage" (ignora mayúsculas)
        val filter = { entry: BootEntry.HekateEntry ->
            val label = entry.label.lowercase()
            !label.contains("android") && !label.contains("lineage")
        }

        items += iniFolderEntries.filter(filter)
        if (binding.toggleHekateIpl.isChecked) {
            items += hekateIplEntries.filter(filter)
        }

        adapter.submitList(items) {
            binding.bootEntryList.post {
                binding.bootEntryList.layoutManager?.findViewByPosition(0)?.requestFocus()
            }
        }
    }

    private fun onEntrySelected(entry: BootEntry) {
        lifecycleScope.launch(Dispatchers.IO) {
            val ok = when (entry) {
                is BootEntry.HekateEntry -> RootActions.rebootToEntry(entry)
                is BootEntry.SystemAction -> when (entry.action) {
                    BootEntry.SystemAction.Action.RESTART -> RootActions.gracefulReboot()
                    BootEntry.SystemAction.Action.SHUTDOWN -> RootActions.gracefulShutdown()
                }
            }
            if (!ok) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, R.string.error_action_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Navegación completa con mando: B cierra la app, A activa el elemento
    // enfocado. La cruceta/joystick ya funciona sola gracias al sistema de
    // foco estándar de Android sobre vistas focusable (RecyclerView + switch).
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> {
                    finish()
                    return true
                }

                KeyEvent.KEYCODE_BUTTON_A -> {
                    currentFocus?.performClick()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
