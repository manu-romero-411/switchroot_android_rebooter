package com.switchroot.rebootmanager.ini

import com.switchroot.rebootmanager.R

object IconResolver {

    // Mapeo, a ojo, de los tokens icon= más habituales en hekate_ipl.ini
    // de la comunidad a glifos genéricos incluidos en la app. Estos NO son
    // los iconos reales del sistema (evitamos reproducir logos registrados);
    // son sustitutos abstractos y se pueden reemplazar por tus propios
    // drawables en res/drawable si quieres iconografía más fiel.
    private val KNOWN_TOKENS = mapOf(
        "l4t" to R.drawable.ic_boot_linux,
        "linux" to R.drawable.ic_boot_linux,
        "android" to R.drawable.ic_boot_android,
        "lakka" to R.drawable.ic_boot_gamepad,
        "retroarch" to R.drawable.ic_boot_gamepad,
        "hos" to R.drawable.ic_boot_switch,
        "switch" to R.drawable.ic_boot_switch,
        "payload" to R.drawable.ic_boot_payload,
        "ums" to R.drawable.ic_boot_usb
    )

    /**
     * TODO: hekate también permite icon=ruta/a/archivo.bmp apuntando a un bitmap
     * en la propia tarjeta SD. Cargar ese bitmap real vía SAF sería una mejora
     * futura; por ahora, cualquier valor no reconocido cae al icono genérico.
     */
    fun resolve(iconToken: String?): Int {
        if (iconToken == null) return R.drawable.ic_boot_generic
        val key = iconToken.substringBeforeLast('.').lowercase().trim()
        return KNOWN_TOKENS[key] ?: R.drawable.ic_boot_generic
    }
}
