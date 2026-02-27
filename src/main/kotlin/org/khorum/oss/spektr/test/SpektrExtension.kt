package org.khorum.oss.spektr.test

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.util.concurrent.ConcurrentHashMap

class SpektrExtension : BeforeAllCallback, ParameterResolver {

    companion object {
        // Key: a string that uniquely identifies the container configuration
        private val containers = ConcurrentHashMap<String, SpektrContainer>()
    }

    override fun beforeAll(context: ExtensionContext) {
        val annotation = context.requiredTestClass.getAnnotation(WithSpektr::class.java) ?: return

        val cacheKey = buildCacheKey(annotation)

        val container = containers.computeIfAbsent(cacheKey) {
            SpektrContainer(annotation.image).apply {
                configureJars(annotation)
                withRestEnabled(annotation.restEnabled)
                withSoapEnabled(annotation.soapEnabled)
                start()
            }
        }

        System.setProperty("spektr.base-url", container.baseUrl)
        annotation.properties.forEach { prop ->
            System.setProperty(prop, container.baseUrl)
        }
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean = parameterContext.parameter.type == SpektrContainer::class.java

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any? {
        val annotation = extensionContext.requiredTestClass.getAnnotation(WithSpektr::class.java)
        return containers[buildCacheKey(annotation ?: return null)]
    }

    private fun SpektrContainer.configureJars(annotation: WithSpektr) {
        when {
            annotation.modules.isNotEmpty() -> {
                val jar = SpektrModuleJarBuilder.buildJar(annotation.modules.toList())
                withEndpointJarFile(jar.absolutePath)
            }
            annotation.endpointJarsPath.isNotBlank() -> {
                withEndpointJarsDir(annotation.endpointJarsPath)
            }
        }
    }

    private fun buildCacheKey(annotation: WithSpektr): String {
        val modulesKey = annotation.modules.joinToString(",") { it.java.name }
        return "${annotation.image}|${annotation.endpointJarsPath}|$modulesKey|" +
            "${annotation.restEnabled}|${annotation.soapEnabled}"
    }
}
