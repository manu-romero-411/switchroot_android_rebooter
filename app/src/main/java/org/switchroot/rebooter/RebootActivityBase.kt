package org.switchroot.rebooter

import android.app.Activity
import android.os.Bundle

abstract class RebootActivityBase : Activity() {

    abstract val param1Index: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cmds = listOf(
            "echo self > /sys/devices/r2p/action",
            "echo $param1Index > /sys/devices/r2p/param1",
            "echo 1 > /sys/devices/r2p/param2",
            "reboot"
        )

        Thread {
            RootUtils.runAsRoot(cmds)
            finish()
        }.start()
    }
}
