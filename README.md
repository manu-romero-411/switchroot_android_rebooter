# Switchroot Android Rebooter

A utility for modded Nintendo Switch consoles running Android (Switchroot). It provides a fast, modern interface to reboot directly into specific Hekate boot entries, standard system actions, or other operating systems.

## Key Features

*   **Dynamic Discovery**: Automatically scans `/bootloader/ini/*.ini` and `/bootloader/hekate_ipl.ini` for bootable entries. No hardcoding required.
*   **Modern Popup UI**: A sleek, centered dialog interface with Material You dynamic colors (Android 12+) and background dimming.
*   **Real Icons Support**: Loads custom `.bmp` icons directly from your SD card (e.g., from `bootloader/res/`) just like Hekate does.
*   **Performance Optimized**: Uses parallel processing and metadata caching to ensure an almost instantaneous startup, even with many entries.
*   **Gamepad Ready**: Fully navigable with Joy-Cons or Pro Controllers. Supports D-pad/Joystick navigation, **A** to select, and **B** to exit.
*   **Localized**: Full support for **English** and **Spanish**.
*   **Smart Filtering**: Automatically hides entries containing "Android" or "Lineage" to keep the list focused on external boot targets.

## How it Works (Technical Details)

The app utilizes the **Reboot2Payload (R2P)** kernel driver to communicate with the bootloader. It requires **root access** (Magisk or KernelSU) to write to the following sysfs nodes:

*   **`/sys/devices/r2p/action`**: Defines the reboot target. The app sets this to `self` for specific entries or `bootloader` for the Hekate menu.
*   **`/sys/devices/r2p/param1`**: The index of the entry. The app calculates this dynamically following Hekate's logic: alphabetical order of files in `/bootloader/ini/` and top-to-bottom order within each file.
*   **`/sys/devices/r2p/param2`**: Set to `1` to look in the `/bootloader/ini/` folder, or `0` for the main `hekate_ipl.ini` file.

After configuring these nodes, the app triggers a graceful system reboot using `svc power reboot`.

Refer to the [Switchroot Wiki](https://wiki.switchroot.org/wiki/android/android-11/11-r-ini-guide) for more detailed info on R2P.

## Setup

1.  **Root Access**: Ensure your Switchroot installation has Magisk or KernelSU.
2.  **First Launch**: The app will ask you to select the `bootloader` folder on your SD card using the Android Storage Access Framework (SAF). This permission is persisted, so you only need to do it once.
3.  **Hekate IPL**: You can toggle the visibility of `hekate_ipl.ini` entries using the switch at the top right.

## Requirements

*   Nintendo Switch hardware.
*   Switchroot Android (Android 10+ recommended).
*   Root privileges.

## Development

This project is built with Kotlin and follows modern Android best practices (Coroutines, ViewBinding, Material 3).

```bash
./gradlew assembleDebug
```
