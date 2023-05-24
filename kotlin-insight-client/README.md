# kotlin-insight-client

Provides a Kotlin client for interaction with Atlassian Assets / Insight.
It provides access to

- InsightObjects
- Attachments
- History
- Object types
- Schemas

The client is provided in two different variants:

| Variant                  | **SDK**                               | **HTTP**                                      |
|--------------------------|---------------------------------------|-----------------------------------------------|
| Scope of application     | Insight-Plugin                        | Any                                           |
| Implementation           | Atlassian Insight SDK libraries       | Ktor or Atlassian Applink                     |
| Advantages/Disadvantages | + Fast <br> - Limited to Jira-Plugins | + Usable everywhere <br> - Comparatively slow |

## Usage

The plugin is available on Maven Central.
First you need to define a dependency to the desired variant and initialize your client as described in the sections
below.
Then you can simply use the client as it's shown
in [InsightClientTest](kotlin-insight-client-test-base/src/main/kotlin/com/linkedplanet/kotlininsightclient/InsightClientTest.kt).

### SDK

Add the following dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>com.linked-planet.client</groupId>
    <artifactId>kotlin-insight-client-sdk</artifactId>
    <version>${insight.client.version}</version>
</dependency>
```

For client initialization take a look at the [InsightSdkClientTest](kotlin-insight-client-test-sdk/src/test/kotlin/it/InsightSdkClientTest.kt).

### HTTP

The HTTP variant requires an implementation of the `kotlin-http-client`.

#### HTTP via Atlassian Applinks

The usage of `kotlin-http-client-atlas` is **limited to Atlassian applications** which have an application link to the
desired Jira instance. Authentication is automatically handled through the link.

Specify the following dependency for usage:

```xml

<dependency>
    <groupId>com.linked-planet.client</groupId>
    <artifactId>kotlin-insight-client-http</artifactId>
    <version>${insight.client.version}</version>
</dependency>
<dependency>
<groupId>com.linked-planet.client</groupId>
<artifactId>kotlin-http-client-atlas</artifactId>
<version>${http.client.version}</version>
</dependency>
```

For client initialization take a look at the [InsightApplinkClientTest](kotlin-insight-client-test-applink/src/test/kotlin/it/InsightApplinkClientTest.kt).

#### HTTP via Ktor

`kotlin-http-client-ktor` uses Ktor for HTTP communication and can be used in every environment.

Specify the following dependency for usage:

```xml

<dependency>
    <groupId>com.linked-planet.client</groupId>
    <artifactId>kotlin-insight-client-http</artifactId>
    <version>${insight.client.version}</version>
</dependency>
<dependency>
<groupId>com.linked-planet.client</groupId>
<artifactId>kotlin-http-client-ktor</artifactId>
<version>${http.client.version}</version>
</dependency>
```

For client initialization take a look at the [InsightKtorClientTest](kotlin-insight-client-test-ktor/src/test/kotlin/InsightKtorClientTest.kt).

## Project structure

The project is structured using multiple Maven modules.

### Productive modules

- **kotlin-insight-client-api**: Provides the interfaces of the client. Always implement using classes from this package.
- **kotlin-insight-client-http**: Implements the client's interfaces using a kotlin-http-client.
- **kotlin-insight-client-sdk**: Implements the client's interfaces using several Atlassian Insight SDK libraries.

### Test modules

- **kotlin-insight-client-test-base**: Defines several test cases to verify the client functionality. All those test cases
  are used in the following modules.
- **kotlin-insight-client-test-applink**: Tests the `kotlin-insight-client-http` with `kotlin-http-client-atlas` in a
  Confluence Plugin that is connected to Insight via an Applink
- **kotlin-insight-client-test-ktor**: Tests the `kotlin-insight-client-http` with `kotlin-http-client-ktor` using Ktor
- **kotlin-insight-client-test-sdk**: Tests the `kotlin-insight-client-sdk` in a Jira Plugin

### Development

#### Setting up Jira (from Scratch) for Testing

see jira-insight-client readme
