package com.switchroot.rebootmanager.model

import android.net.Uri

sealed class BootEntry {

    /** Una entrada leída de un fichero .ini compatible con hekate. */
    data class HekateEntry(
        val label: String,
        val iconToken: String?,
        val iconUri: Uri?,
        val sourceType: SourceType,
        /** Índice 1-based dentro de su fuente, según el orden que usa hekate para r2p/param1. */
        val indexInSource: Int,
        val sourceFileName: String
    ) : BootEntry()

    /** Acciones estándar del sistema, ajenas a hekate. */
    data class SystemAction(val action: Action) : BootEntry() {
        enum class Action { RESTART, SHUTDOWN }
    }

    enum class SourceType {
        /** Entradas de ficheros dentro de /bootloader/ini/ (r2p/param2 = 1) */
        INI_FOLDER,
        /** Entradas de /bootloader/hekate_ipl.ini (r2p/param2 = 0) */
        HEKATE_IPL
    }
}
