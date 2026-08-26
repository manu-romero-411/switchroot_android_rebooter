package com.switchroot.rebootmanager

import android.content.Context
import android.net.Uri

/**
 * Envoltorio simple sobre SharedPreferences. Guarda:
 *  - la URI (SAF) de la raíz de la tarjeta SD elegida en el primer arranque
 *  - si se deben mostrar también las entradas de hekate_ipl.ini (toggle superior)
 */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var sdRootUri: Uri?
        get() = sp.getString(KEY_SD_ROOT_URI, null)?.let(Uri::parse)
        set(value) = sp.edit().putString(KEY_SD_ROOT_URI, value?.toString()).apply()

    var showHekateIplEntries: Boolean
        get() = sp.getBoolean(KEY_SHOW_HEKATE_IPL, false)
        set(value) = sp.edit().putBoolean(KEY_SHOW_HEKATE_IPL, value).apply()

    companion object {
        private const val PREFS_NAME = "switch_reboot_manager_prefs"
        private const val KEY_SD_ROOT_URI = "sd_root_uri"
        private const val KEY_SHOW_HEKATE_IPL = "show_hekate_ipl_entries"
    }
}
