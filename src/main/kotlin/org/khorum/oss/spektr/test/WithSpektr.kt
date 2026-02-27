package org.khorum.oss.spektr.test

import org.junit.jupiter.api.extension.ExtendWith
import org.khorum.oss.spektr.dsl.EndpointModule
import kotlin.reflect.KClass

/**
 * Annotate a test class to spin up a Spektr container via Testcontainers.
 *
 * Two usage modes:
 *
 * 1. **Inline modules** — define endpoint modules in your test source and reference them directly:
 *    ```kotlin
 *    @WithSpektr(modules = [MyEndpoints::class])
 *    class MyTest { ... }
 *    ```
 *    The extension will compile a temporary JAR from those classes and mount it.
 *
 * 2. **Pre-built JAR** — point at an already-built JAR directory:
 *    ```kotlin
 *    @WithSpektr(endpointJarsPath = "../docker/endpoint-jars")
 *    class MyTest { ... }
 *    ```
 *
 * [properties] — each entry gets set as a System property pointing at the container's base URL.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(SpektrExtension::class)
annotation class WithSpektr(
    val image: String = "spektr:local",
    /** Path to a directory of pre-built endpoint JARs to mount. */
    val endpointJarsPath: String = "",
    /** EndpointModule implementations to package on-the-fly and mount. */
    val modules: Array<KClass<out EndpointModule>> = [],
    val restEnabled: Boolean = true,
    val soapEnabled: Boolean = true,
    /** System properties that will be set to the container's base URL. */
    val properties: Array<String> = []
)
