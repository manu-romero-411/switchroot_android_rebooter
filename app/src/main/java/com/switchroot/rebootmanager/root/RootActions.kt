package com.switchroot.rebootmanager.root

import com.switchroot.rebootmanager.model.BootEntry
import com.topjohnwu.superuser.Shell

/**
 * Ver https://wiki.switchroot.org/wiki/android/android-11/11-r-ini-guide
 * para el detalle de los controles sysfs de r2p (reboot-to-payload).
 */
object RootActions {

    /** Fuerza la petición/comprobación de root; bloqueante, llamar fuera del hilo principal. */
    fun hasRoot(): Boolean = Shell.getShell().isRoot

    /** Reinicia a una entrada concreta de un ini de hekate mediante los controles r2p. */
    fun rebootToEntry(entry: BootEntry.HekateEntry): Boolean {
        val param2 = if (entry.sourceType == BootEntry.SourceType.INI_FOLDER) 1 else 0
        // En muchas versiones de Switchroot, escribir en sysfs solo configura el target,
        // pero NO dispara el reinicio. Por eso después llamamos a gracefulReboot().
        Shell.cmd(
            "echo ${entry.indexInSource} > /sys/devices/r2p/param1",
            "echo $param2 > /sys/devices/r2p/param2",
            "echo self > /sys/devices/r2p/action"
        ).exec()
        return gracefulReboot()
    }

    /** Reinicia directamente al menú de hekate. */
    fun rebootToHekateMenu(): Boolean {
        Shell.cmd("echo bootloader > /sys/devices/r2p/action").exec()
        return gracefulReboot()
    }

    /**
     * Reinicio estándar y elegante: usa el mismo camino que Ajustes ("svc power"
     * llama al PowerManagerService real), con "reboot" crudo como último recurso.
     */
    fun gracefulReboot(): Boolean {
        if (Shell.cmd("svc power reboot switchroot_reboot_manager").exec().isSuccess) return true
        return Shell.cmd("reboot").exec().isSuccess
    }

    /** Apagado estándar y elegante, mismo criterio que gracefulReboot(). */
    fun gracefulShutdown(): Boolean {
        if (Shell.cmd("svc power shutdown").exec().isSuccess) return true
        return Shell.cmd("reboot -p").exec().isSuccess
    }
}
