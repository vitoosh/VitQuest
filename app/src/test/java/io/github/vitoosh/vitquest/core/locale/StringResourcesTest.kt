package io.github.vitoosh.vitquest.core.locale

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Every translatable English string must have an Indonesian translation and vice versa. */
class StringResourcesTest {

    private data class StringRes(val name: String, val translatable: Boolean, val placeholders: List<String>)

    private fun read(path: String): Map<String, StringRes> {
        val file = File(path)
        assertTrue("Missing $path", file.isFile)
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            .getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }.associate { element ->
            val name = element.getAttribute("name")
            name to StringRes(
                name = name,
                translatable = element.getAttribute("translatable") != "false",
                placeholders = PLACEHOLDER.findAll(element.textContent).map { it.value }.sorted().toList(),
            )
        }
    }

    @Test
    fun indonesianAndEnglishHaveTheSameKeys() {
        val english = read("src/main/res/values/strings.xml").filterValues { it.translatable }
        val indonesian = read("src/main/res/values-in/strings.xml")

        assertEquals(english.keys, indonesian.keys)
    }

    @Test
    fun pluralsExistInBothLanguages() {
        fun plurals(path: String): Set<String> {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
                .getElementsByTagName("plurals")
            return (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("name") }.toSet()
        }
        assertEquals(plurals("src/main/res/values/strings.xml"), plurals("src/main/res/values-in/strings.xml"))
    }

    @Test
    fun translationsKeepTheSamePlaceholders() {
        val english = read("src/main/res/values/strings.xml")
        val indonesian = read("src/main/res/values-in/strings.xml")

        indonesian.values.forEach { translated ->
            assertEquals(translated.name, english.getValue(translated.name).placeholders, translated.placeholders)
        }
    }

    private companion object {
        val PLACEHOLDER = Regex("""%\d+\$[sd]""")
    }
}
