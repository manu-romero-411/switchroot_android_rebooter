package org.switchroot.rebooter.ini

data class IniSection(val name: String, val properties: Map<String, String>)

object HekateIniParser {

    private val SECTION_REGEX = Regex("^\\[(.+)]$")
    private const val COMMENT_CHARS = "#;"

    /**
     * Parsea texto ini estilo hekate en una lista ordenada de secciones,
     * respetando el orden en que aparecen en el fichero (hekate recorre
     * cada fichero de arriba a abajo, no alfabéticamente, para las
     * entradas *dentro* de un mismo fichero).
     */
    fun parse(text: String): List<IniSection> {
        val sections = mutableListOf<IniSection>()
        var currentName: String? = null
        var currentProps = linkedMapOf<String, String>()

        fun flush() {
            val name = currentName ?: return
            sections += IniSection(name, currentProps)
        }

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || (line.isNotEmpty() && COMMENT_CHARS.contains(line[0]))) {
                return@forEach
            }

            val sectionMatch = SECTION_REGEX.matchEntire(line)
            if (sectionMatch != null) {
                flush()
                currentName = sectionMatch.groupValues[1].trim()
                currentProps = linkedMapOf()
                return@forEach
            }

            val eq = line.indexOf('=')
            if (eq > 0 && currentName != null) {
                val key = line.substring(0, eq).trim().lowercase()
                val value = line.substring(eq + 1).trim()
                currentProps[key] = value
            }
        }
        flush()
        return sections
    }

    /**
     * true para secciones que hekate trata como *configuración* y no como
     * entradas de arranque (p.ej. el bloque global "[config]", o nombres que
     * hekate oculta del menú por empezar con "{}"). Esto reproduce el
     * filtrado de hekate lo bastante bien para ficheros .ini normales, pero
     * casos exóticos pueden necesitar ajustar esta función.
     */
    fun isBootableEntry(section: IniSection): Boolean {
        val name = section.name.trim()
        if (name.equals("config", ignoreCase = true)) return false
        if (name.startsWith("{}")) return false
        return true
    }
}
