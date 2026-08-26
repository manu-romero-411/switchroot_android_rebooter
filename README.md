# Switchroot Android Rebooter

A utility for modded Nintendo Switch consoles running Android (Switchroot). It provides a fast, modern interface to reboot directly into specific Hekate boot entries, standard system actions, or other operating systems.

## Key Features

*   Automatically scans `<SWITCH SD>/bootloader/ini/*.ini` and `<SWITCH SD>/bootloader/hekate_ipl.ini` for bootable entries.
*   Uses a popup-like UI with Material You dynamic colors (Android 12+) and background dimming.
*   Icons are loaded from the same locations that the Hekate inis point to, generally in `.bmp` format.
*   Fully navigable with gamepads (tested on my Nintendo Switch Lite integrated gamepad).
*   Label support for **English** and **Spanish**.

NOTE: The program automatically hides entries containing `"android"` or `"lineage"`. Feel free to fork/modify this project and rebuild if you want them back.

## How it Works (Technical Details)

The app utilizes the **Reboot2Payload (R2P)** kernel driver to communicate with the bootloader. It requires **root access** (Magisk or KernelSU) to write to the following sysfs nodes:

*   **`/sys/devices/r2p/action`**: Defines the reboot target. The app sets this to `self` for specific entries or `bootloader` for the Hekate menu.
*   **`/sys/devices/r2p/param1`**: The index of the entry. The app calculates this dynamically following Hekate's logic: alphabetical order of files in `/bootloader/ini/` and top-to-bottom order within each file.
*   **`/sys/devices/r2p/param2`**: Set to `1` to look in the `/bootloader/ini/` folder, or `0` for the main `hekate_ipl.ini` file.

After configuring these nodes, the app triggers a graceful system reboot using `svc power reboot`.

Refer to the [Switchroot Wiki](https://wiki.switchroot.org/wiki/android/android-11/11-r-ini-guide) for more detailed info on R2P.

## Setup

1.  **Root Access**: Ensure your Switchroot installation has Magisk or KernelSU. It's no difficult to set up, refer to Magisk documentation for details (recovery is entered on Switch by pressing `VOL+` while selecting the Android entry in Hekate, then keep the key pressed until the recovery appears).
2.  **First Launch**: The app will ask you to select the `bootloader` folder on your SD card using the Android Storage Access Framework (SAF). This permission is persisted, so you only need to do it once. Yo have to select the folder correctly, or else this app won't work fine.
3.  **Hekate IPL**: You can toggle the visibility of `hekate_ipl.ini` entries using the switch at the top right.

## Requirements

*   Nintendo Switch hardware.
*   Switchroot Android - tested on Android 15 (latest build as of v2.0 tag commit date), but should work on Android 11 as well.
*   Root privileges.

## Development

This project is built with Kotlin and follows modern Android best practices (Coroutines, ViewBinding, Material 3).

```bash
./gradlew assembleDebug
```
