package org.switchroot.rebooter.ini

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.switchroot.rebooter.model.BootEntry
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

import java.util.concurrent.ConcurrentHashMap

class BootEntryRepository(private val context: Context) {

    private val dirCache = ConcurrentHashMap<String, DocumentFile>()

    suspend fun readEntries(sdRoot: Uri): ReadResult = withContext(Dispatchers.IO) {
        dirCache.clear()
        val bootloaderDir = DocumentFile.fromTreeUri(context, sdRoot)
            ?: return@withContext ReadResult(emptyList(), emptyList(), ErrorType.CANNOT_OPEN_FOLDER)

        if (!bootloaderDir.isDirectory) {
            return@withContext ReadResult(emptyList(), emptyList(), ErrorType.NO_BOOTLOADER_FOLDER)
        }

        // Leemos las dos fuentes en paralelo
        val iniFolderJob = async { readIniFolder(bootloaderDir) }
        val hekateIplJob = async { readHekateIpl(bootloaderDir) }

        ReadResult(iniFolderJob.await(), hekateIplJob.await(), null)
    }

    private suspend fun readIniFolder(bootloaderDir: DocumentFile): List<BootEntry.HekateEntry> = coroutineScope {
        val iniDir = bootloaderDir.findFile("ini")
        if (iniDir == null || !iniDir.isDirectory) return@coroutineScope emptyList()

        val files = iniDir.listFiles()
            .filter { it.isFile && it.name?.endsWith(".ini", ignoreCase = true) == true }
            .sortedBy { it.name?.lowercase() }

        // Procesamos cada archivo .ini en paralelo
        val deferredEntries = files.map { file ->
            async {
                val text = readText(file.uri) ?: return@async emptyList<Pair<DocumentFile, IniSection>>()
                HekateIniParser.parse(text)
                    .filter(HekateIniParser::isBootableEntry)
                    .map { file to it }
            }
        }

        val allSections = deferredEntries.awaitAll().flatten()
        
        allSections.mapIndexed { index, (file, section) ->
            val iconPath = section.properties["icon"]
            BootEntry.HekateEntry(
                label = section.name,
                iconToken = iconPath,
                iconUri = resolveIconUri(bootloaderDir, iconPath),
                sourceType = BootEntry.SourceType.INI_FOLDER,
                indexInSource = index + 1,
                sourceFileName = file.name.orEmpty()
            )
        }
    }

    private fun readHekateIpl(bootloaderDir: DocumentFile): List<BootEntry.HekateEntry> {
        val file = bootloaderDir.findFile("hekate_ipl.ini") ?: return emptyList()
        val text = readText(file.uri) ?: return emptyList()
        val sections = HekateIniParser.parse(text).filter(HekateIniParser::isBootableEntry)

        return sections.mapIndexed { i, section ->
            val iconPath = section.properties["icon"]
            BootEntry.HekateEntry(
                label = section.name,
                iconToken = iconPath,
                iconUri = resolveIconUri(bootloaderDir, iconPath),
                sourceType = BootEntry.SourceType.HEKATE_IPL,
                indexInSource = i + 1,
                sourceFileName = "hekate_ipl.ini"
            )
        }
    }

    private fun resolveIconUri(bootloaderDir: DocumentFile, iconPath: String?): Uri? {
        if (iconPath == null) return null
        val cleanPath = iconPath.trimStart('/')
        val parts = cleanPath.split('/')
        
        var current: DocumentFile = bootloaderDir
        val startIdx = if (parts.isNotEmpty() && parts[0].equals("bootloader", ignoreCase = true)) 1 else 0
        
        var currentPath = "bootloader"
        for (i in startIdx until parts.size) {
            val part = parts[i]
            currentPath += "/${part.lowercase()}"
            
            // Usamos caché para no repetir findFile en carpetas ya conocidas (como 'res')
            val found = dirCache[currentPath] ?: current.findFile(part) ?: return null
            dirCache[currentPath] = found
            
            if (i == parts.size - 1) {
                return if (found.isFile) found.uri else null
            }
            if (!found.isDirectory) return null
            current = found
        }
        return null
    }

    private fun readText(uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
    } catch (e: Exception) {
        null
    }

    enum class ErrorType { CANNOT_OPEN_FOLDER, NO_BOOTLOADER_FOLDER }

    data class ReadResult(
        val iniFolderEntries: List<BootEntry.HekateEntry>,
        val hekateIplEntries: List<BootEntry.HekateEntry>,
        val error: ErrorType?
    )
}
