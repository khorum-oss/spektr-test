package org.khorum.oss.spektr.test

import org.khorum.oss.spektr.dsl.EndpointModule
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipFile
import kotlin.reflect.KClass

object SpektrModuleJarBuilder {

    private const val SERVICE_FILE = "META-INF/services/org.khorum.oss.spektr.dsl.EndpointModule"

    private val SKIP_PATH_SEGMENTS = listOf(
        "/.gradle/",
        "/gradle/caches/",
        "/.m2/",
        "/jdk/",
        "/jre/"
    )

    fun buildJar(modules: List<KClass<out EndpointModule>>): File {
        val jarFile = File.createTempFile("spektr-modules-", ".jar")
        jarFile.deleteOnExit()

        val projectRoots = collectProjectClasspathRoots(modules)

        val manifest = Manifest()
        manifest.mainAttributes.putValue("Manifest-Version", "1.0")

        JarOutputStream(jarFile.outputStream(), manifest).use { jar ->
            val addedEntries = mutableSetOf<String>()

            projectRoots.forEach { root ->
                when {
                    root.isDirectory -> addClassesFromDir(root, root, jar, addedEntries)
                    root.isFile && root.name.endsWith(".jar") -> addClassesFromJar(root, jar, addedEntries)
                }
            }

            val serviceContent = modules.joinToString("\n") { it.java.name }
            if (addedEntries.add(SERVICE_FILE)) {
                jar.putNextEntry(JarEntry(SERVICE_FILE))
                jar.write(serviceContent.toByteArray(Charsets.UTF_8))
                jar.closeEntry()
            }
        }

        return jarFile
    }

    private fun collectProjectClasspathRoots(modules: List<KClass<out EndpointModule>>): List<File> {
        val roots = mutableSetOf<File>()

        // Guaranteed to find each module's exact location regardless of classloader type
        modules.forEach { kClass ->
            val location = kClass.java.protectionDomain?.codeSource?.location
            if (location != null) {
                roots.add(File(location.toURI()))
            }
        }

        // java.class.path is populated by Gradle and JUnit — catches transitive project deps
        System.getProperty("java.class.path")
            ?.split(File.pathSeparator)
            ?.map { File(it) }
            ?.filter { file ->
                val path = file.absolutePath
                SKIP_PATH_SEGMENTS.none { path.contains(it) } && file.exists()
            }
            ?.forEach { roots.add(it) }

        return roots.toList()
    }

    private fun addClassesFromDir(
        root: File,
        current: File,
        jar: JarOutputStream,
        added: MutableSet<String>
    ) {
        current.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                addClassesFromDir(root, file, jar, added)
            } else {
                val entryName = file.relativeTo(root).path.replace(File.separatorChar, '/')
                if (!entryName.startsWith("META-INF/services") && added.add(entryName)) {
                    jar.putNextEntry(JarEntry(entryName))
                    file.inputStream().use { it.copyTo(jar) }
                    jar.closeEntry()
                }
            }
        }
    }

    private fun addClassesFromJar(
        sourceJar: File,
        jar: JarOutputStream,
        added: MutableSet<String>
    ) {
        ZipFile(sourceJar).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory }
                .filter { !it.name.startsWith("META-INF/services") }
                .filter { it.name != "META-INF/MANIFEST.MF" }  // already written by JarOutputStream constructor
                .forEach { entry ->
                    if (added.add(entry.name)) {
                        jar.putNextEntry(JarEntry(entry.name))
                        zip.getInputStream(entry).use { it.copyTo(jar) }
                        jar.closeEntry()
                    }
                }
        }
    }
}
