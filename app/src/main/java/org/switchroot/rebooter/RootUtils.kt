package org.switchroot.rebooter

import java.io.DataOutputStream

object RootUtils {
    fun runAsRoot(commands: List<String>) {
        try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                for (cmd in commands) {
                    os.writeBytes(cmd)
                    os.writeBytes("\n")
                }
                os.writeBytes("exit\n")
                os.flush()
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
