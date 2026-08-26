package org.switchroot.rebooter

import android.app.Application
import com.topjohnwu.superuser.Shell

class RebootManagerApp : Application() {

    companion object {
        init {
            // Configuración global de libsu; debe fijarse antes de la primera
            // llamada a Shell.getShell()/Shell.cmd(). El prompt de root (Magisk,
            // KernelSU, etc.) lo dispara el propio Shell la primera vez que se usa.
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }
}
