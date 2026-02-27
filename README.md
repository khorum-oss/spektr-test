<span style="display: flex; align-items: center; justify-content: center;">
    <img src="assets/spektr-logo-1.png" alt="alt text" width="120" />
    <h1 style="padding: 0; margin: 0; font-size: 76px">Spektr</h1>
</span>

A dynamic endpoint server that loads REST and SOAP endpoints from external JAR files at runtime.
Perfect for creating mock servers and test fixtures.

## Documentation

See the [docs folder](docs/README.md) for detailed documentation:
- [Examples Overview](docs/examples.md) - Guide to the example applications
- [Testing with Testcontainers](docs/testing.md) - Using Spektr as a mock service in tests
- [Docker Setup](docs/docker.md) - Running Spektr in Docker

## Features

- **Dynamic endpoint loading** - Load endpoints from JAR files without restarting
- **Hot reload** - Add or update endpoint JARs and reload via API
- **REST and SOAP support** - Define both REST and SOAP endpoints in the same module
- **Protocol toggles** - Enable or disable REST and SOAP independently via configuration
- **DSL-based configuration** - Define endpoints using a simple Kotlin DSL
- **ServiceLoader discovery** - Automatically discovers `EndpointModule` implementations

## Quick Start

### 1. Build the application

```shell
./gradlew :app:bootJar
```

### 2. Create an endpoint JAR

Implement the `EndpointModule` interface:

```kotlin
package com.example.endpoints

import org.khorum.oss.spektr.dsl.*

class MyEndpoints : EndpointModule {
    override fun EndpointRegistry.configure() {
        get("/api/hello/{name}") { request ->
            val name = request.pathVariables["name"]
            returnBody(mapOf("message" to "Hello, $name!"))
        }

        post("/api/users") { request ->
            // Use DynamicResponse for custom status codes
            DynamicResponse(status = 201, body = mapOf("created" to true))
        }

        delete("/api/users/{id}") { request ->
            returnStatus(204)
        }
    }

    override fun SoapEndpointRegistry.configureSoap() {
        operation("/ws/greeting", "SayHello") { request ->
            SoapResponse(
                body = """
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                      <soap:Body>
                        <SayHelloResponse>
                          <message>Hello from SOAP!</message>
                        </SayHelloResponse>
                      </soap:Body>
                    </soap:Envelope>
                """.trimIndent()
            )
        }
    }
}
```

Register it in `META-INF/services/org.khorum.oss.spektr.dsl.EndpointModule`:
```
com.example.endpoints.MyEndpoints
```

### 3. Run the server

```shell
java -jar app/build/libs/app.jar --endpoint-jars.dir=./my-jars
```

## Configuration

### Core Properties

| Property | Default | Description |
|----------|---------|-------------|
| `endpoint-jars.dir` | `./endpoint-jars` | Directory containing endpoint JAR files |
| `spektr.rest.enabled` | `true` | Enable or disable REST endpoint loading |
| `spektr.soap.enabled` | `true` | Enable or disable SOAP endpoint loading |

### Environment Variables

| Variable | Description |
|----------|-------------|
| `ENDPOINT_JARS_DIR` | Override the endpoint JARs directory |
| `SPEKTR_REST_ENABLED` | Enable/disable REST support (`true`/`false`) |
| `SPEKTR_SOAP_ENABLED` | Enable/disable SOAP support (`true`/`false`) |
| `JAVA_OPTS` | JVM options (when using Docker) |

### Protocol Configuration Examples

**REST only (disable SOAP):**
```yaml
spektr:
  soap:
    enabled: false
```

**SOAP only (disable REST):**
```yaml
spektr:
  rest:
    enabled: false
```

**Both enabled (default):**
```yaml
spektr:
  rest:
    enabled: true
  soap:
    enabled: true
```

### Custom Configuration

You can inject additional configuration using Spring Boot's standard mechanisms:

**Environment variable:**
```shell
SPRING_CONFIG_IMPORT=optional:file:./my-config.yaml
```

**Command line:**
```shell
java -jar app.jar --spring.config.import=optional:file:./my-config.yaml
```

**Multiple config files:**
```shell
java -jar app.jar --spring.config.additional-location=file:./custom.yaml
```

## Docker

### Build the image

```shell
docker build -t spektr .
```

### Run with default settings

```shell
docker run -p 8080:8080 spektr
```

### Run with endpoint JARs mounted

```shell
docker run -p 8080:8080 \
  -v /path/to/your/jars:/app/endpoint-jars \
  spektr
```

### Run with custom configuration

```shell
docker run -p 8080:8080 \
  -e SPRING_CONFIG_IMPORT=optional:file:/app/config/custom.yaml \
  -v /my/config:/app/config \
  -v /my/jars:/app/endpoint-jars \
  spektr
```

### Run with REST only

```shell
docker run -p 8080:8080 \
  -e SPEKTR_SOAP_ENABLED=false \
  spektr
```

### Run with custom JVM options

```shell
docker run -p 8080:8080 \
  -e JAVA_OPTS="-Xmx512m -Xms256m" \
  spektr
```

## Admin API

### Reload endpoints

Reload all endpoints from the configured JAR directory:

```shell
curl -X POST http://localhost:8080/admin/endpoints/reload
```

Response:
```json
{
  "endpointsLoaded": 5,
  "soapEndpointsLoaded": 3,
  "jarsProcessed": 2,
  "reloadTimeMs": 42
}
```

### Upload and reload

Upload a new JAR file and reload endpoints:

```shell
curl -X POST http://localhost:8080/admin/endpoints/upload \
  -F "jar=@my-endpoints.jar"
```

## REST DSL Reference

### HTTP Methods

```kotlin
get("/path") { request -> returnBody(data) }
post("/path") { request -> returnBody(data) }
put("/path") { request -> returnBody(data) }
patch("/path") { request -> returnBody(data) }
delete("/path") { request -> returnStatus(204) }
options("/path") { request -> returnBody(data) }
```

### Path Variables

```kotlin
get("/users/{id}") { request ->
    val id = request.pathVariables["id"]
    returnBody(mapOf("id" to id))
}
```

### Query Parameters

```kotlin
get("/users") { request ->
    val active = request.queryParams["active"]?.firstOrNull()?.toBoolean()
    val filtered = if (active == true) users.filter { it.active } else users
    returnBody(filtered)
}
```

### Request Properties

```kotlin
request.pathVariables   // Map<String, String> - path parameters
request.queryParams     // Map<String, List<String>> - query string
request.headers         // Map<String, List<String>> - HTTP headers
request.body            // String? - request body
```

### Response Helpers

Simple helpers for common responses:

```kotlin
// Return JSON body with 200 status
returnBody(mapOf("key" to "value"))

// Return specific status code (no body)
returnStatus(204)
```

### Full Response Control

For more control, use `DynamicResponse` directly:

```kotlin
DynamicResponse(
    status = 201,                           // HTTP status code
    body = mapOf("key" to "value"),         // Response body (auto-serialized to JSON)
    headers = mapOf("X-Custom" to "value")  // Response headers
)
```

### Error Scenarios

```kotlin
errorOn(
    method = HttpMethod.GET,
    path = "/api/error",
    status = 500,
    body = mapOf("error" to "Something went wrong")
)
```

### Working with JSON Bodies

For parsing JSON request bodies, use Jackson with the Kotlin module:

```kotlin
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue

private val mapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .build()

data class CreateUserRequest(val name: String, val email: String)

post("/api/users") { request ->
    val body = requireNotNull(request.body) { "Request body required" }
    val user: CreateUserRequest = mapper.readValue(body)
    // ... create user
    returnBody(user)
}
```

## SOAP DSL Reference

### Defining SOAP Operations

Override `configureSoap()` in your `EndpointModule` to define SOAP endpoints:

```kotlin
class MySoapEndpoints : EndpointModule {
    override fun EndpointRegistry.configure() {
        // REST endpoints (can be empty if SOAP-only)
    }

    override fun SoapEndpointRegistry.configureSoap() {
        operation("/ws/myservice", "MyAction") { request ->
            SoapResponse(body = "<MyResponse>...</MyResponse>")
        }
    }
}
```

### SOAP Operations

```kotlin
operation(path, soapAction) { request -> SoapResponse(...) }
```

- `path` - the URL path for the SOAP endpoint (e.g., `/ws/myservice`)
- `soapAction` - the SOAPAction header value to match
- `request` - contains headers, soapAction, and the raw XML body

### SOAP Request Properties

```kotlin
request.headers     // Map<String, List<String>> - HTTP headers
request.soapAction  // String - the SOAPAction value
request.body        // String? - raw SOAP XML body
```

### SOAP Response Options

```kotlin
SoapResponse(
    status = 200,                           // HTTP status code (default 200)
    body = "<soap:Envelope>...</soap:Envelope>",  // XML response body
    headers = mapOf("X-Custom" to "value")  // Response headers
)
```

### SOAP Faults

Use `soapFault()` to register a fault response for a specific action:

```kotlin
soapFault(
    path = "/ws/myservice",
    soapAction = "BadAction",
    faultCode = "soap:Client",
    faultString = "Operation not supported"
)
```

### Calling SOAP Endpoints

SOAP endpoints are invoked via POST with the `SOAPAction` header and `text/xml` content type:

```shell
curl -X POST http://localhost:8080/ws/myservice \
  -H "Content-Type: text/xml" \
  -H 'SOAPAction: "MyAction"' \
  -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
        <soap:Body>
          <MyRequest><param>value</param></MyRequest>
        </soap:Body>
      </soap:Envelope>'
```

### Mixed REST and SOAP Module

A single `EndpointModule` can define both REST and SOAP endpoints:

```kotlin
class MixedEndpoints : EndpointModule {
    override fun EndpointRegistry.configure() {
        get("/api/status") { _ ->
            DynamicResponse(body = mapOf("status" to "ok"))
        }
    }

    override fun SoapEndpointRegistry.configureSoap() {
        operation("/ws/status", "GetStatus") { _ ->
            SoapResponse(
                body = """
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                      <soap:Body>
                        <GetStatusResponse><status>ok</status></GetStatusResponse>
                      </soap:Body>
                    </soap:Envelope>
                """.trimIndent()
            )
        }
    }
}
```

## Logging

Spektr logs incoming requests, matched endpoints, and responses. Configure log levels in your `application.yaml`:

### INFO level (default) - shows matched endpoints and response status

```yaml
logging:
  level:
    org.khorum.oss.spektr.service: INFO
```

Example output:
```
Matched endpoint: GET /api/house/{id} -> /api/house/123
Path variables: {id=123}
Request body: 45 bytes
Returning response: status=200, body type=House
```

### DEBUG level - includes full request/response bodies

```yaml
logging:
  level:
    org.khorum.oss.spektr.service: DEBUG
```

## Development

### Run tests

```shell
./gradlew test
```

### Run locally with test profile

```shell
./gradlew :app:bootRun --args='--spring.profiles.active=test'
```

## License

MIT
