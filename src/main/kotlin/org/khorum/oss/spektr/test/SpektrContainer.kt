package org.khorum.oss.spektr.test

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.time.Duration

class SpektrContainer(
    imageName: String = "spektr:local",
    exposedPort: Int = 8080,
    val usesHttps: Boolean = false
) : GenericContainer<SpektrContainer>(DockerImageName.parse(imageName)) {

    init {
        withExposedPorts(exposedPort)
        waitingFor(
            Wait.forHttp("/actuator/health")
                .forPort(8080)
                .withStartupTimeout(Duration.ofSeconds(60))
        )
    }

    fun withEndpointJarsDir(hostPath: String): SpektrContainer = apply {
        withCopyFileToContainer(
            MountableFile.forHostPath(hostPath),
            "/app/endpoint-jars"
        )
    }

    fun withEndpointJarFile(hostFilePath: String): SpektrContainer = apply {
        withCopyFileToContainer(
            MountableFile.forHostPath(hostFilePath),
            "/app/endpoint-jars/${hostFilePath.substringAfterLast("/")}"
        )
    }

    fun withRestEnabled(enabled: Boolean = true): SpektrContainer = apply {
        withEnv("SPEKTR_REST_ENABLED", enabled.toString())
    }

    fun withSoapEnabled(enabled: Boolean = true): SpektrContainer = apply {
        withEnv("SPEKTR_SOAP_ENABLED", enabled.toString())
    }

    val baseUrl: String
        get() = "${protocol()}://$host:$firstMappedPort"

    private fun protocol(): String = if (usesHttps) "https" else "http"
}
