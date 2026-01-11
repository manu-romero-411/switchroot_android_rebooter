# Switchroot Android Rebooter

An utility intended for modded Switch running Android. It adds three icons to your Android launcher:

* Reboot to Original Stock Firmware (SysMMC)
* Reboot to Atmosphère (EmuMMC)
* Reboot to Ubuntu

Each icon calls **`root`** (so Magisk or similar is needed) to write some variables in `/sys` path:

* `/sys/devices/r2p/action`: can be `self`, `bootloader` or `normal`. We use `self` here.
* `/sys/devices/r2p/param1`: needs the INI entry number. They are sorted alphabetically, keep it in mind when putting your INIs in `/bootloader/ini`.
* `/sys/devices/r2p/param2`: if set to 1, the system will look on the `/bootloader/ini` dir rather than the `/bootloader/hekate_ipl.ini` file.

Refer to the [Switchroot Wiki](https://wiki.switchroot.org/wiki/android/android-11/11-r-ini-guide) for more detailed info.

After setting all values, the app will trigger a reboot.

## My setup (in which the app is based on)

The implemented layout in my app is based on my own configuration in Hekate's `/bootloader/ini` entries. They can be rearranged in `app/src/main/java/org/switchroot/rebooter/Reboot####Activity.kt`. For example:

```kotlin
package org.switchroot.rebooter

class RebootAtmosActivity : RebootActivityBase() {
    override val param1Index: Int = 2 // Change this Int to the index that your Atmosphère INI has (alphabetically ordered in /bootloader/ini)
}
```

These are examples of my INI entries for more precise reference:

### `android.ini` (entry 1 - not implemented)

```inifile
[Android]
l4t=1
boot_prefixes=switchroot/android/
id=SWANDR
r2p_action=bootloader
...
```

### `atmos_emummc.ini` (entry 2)

```inifile
[SwitchOS Atmosphere]
fss0=atmosphere/package3
kip1=atmosphere/kip_patches/*
emummcforce=1
usb3force=1
...
```

### `stock_sysmmc.ini` (entry 3)

```inifile
[SwitchOS Stock]
ofw=1
...
```

### `ubuntu.ini` (entry 4)

```inifile
[Ubuntu]
l4t=1
boot_prefixes=/switchroot/ubuntu-noble/
id=SWR-NOB
rootlabel_retries=100
rootdev=sda2
r2p_action=bootloader
...
```
