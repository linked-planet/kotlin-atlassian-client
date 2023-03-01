# kotlin-jira-client
![Maven Central](https://img.shields.io/maven-central/v/com.linked-planet.client/kotlin-jira-client-api)
![Jira 9.4.2](https://img.shields.io/badge/Jira-9.4.2-blue)

Provides a Kotlin client for interaction with Atlassian Jira. It provides management functionality for

- comments,
- issues,
- transitions,
- users and
- projects.

The client is provided in two different variants:

| Variant                  | **SDK**                               | **HTTP**                                      |
|--------------------------|---------------------------------------|-----------------------------------------------|
| Scope of application     | Jira-Plugin                           | Any                                           |
| Implementation           | Atlassian Jira SDK libraries          | Ktor or Atlassian Applink                     |
| Advantages/Disadvantages | + Fast <br> - Limited to Jira-Plugins | + Usable everywhere <br> - Comparatively slow |

## Usage

The plugin is available on Maven Central.
First you need to define a dependency to the desired variant and initialize your client as described in the sections
below.
Then you can simply use the client as it's shown
in [JiraClientTest](kotlin-jira-client-test-base/src/main/kotlin/com/linkedplanet/kotlinjiraclient/JiraClientTest.kt).

### SDK

|                          | **SDK**                               |
|--------------------------|---------------------------------------|
| Scope of application     | Jira-Plugin                           |
| Implementation           | Atlassian Jira SDK libraries          |
| Advantages/Disadvantages | + Fast <br> - Limited to Jira-Plugins |

Add the following dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>com.linked-planet.client</groupId>
    <artifactId>kotlin-jira-client-sdk</artifactId>
    <version>${jira.client.version}</version>
</dependency>
```

For client initialization take a look at the [JiraSdkClientTest](kotlin-jira-client-test-sdk/src/test/kotlin/it/JiraSdkClientTest.kt).

### HTTP

| Variant                  | **HTTP**                                      |
|--------------------------|-----------------------------------------------|
| Scope of application     | Any                                           |
| Implementation           | Ktor or Atlassian Applink                     |
| Advantages/Disadvantages | + Usable everywhere <br> - Comparatively slow |


The HTTP variant requires an implementation of the `kotlin-http-client`.

#### HTTP via Atlassian Applinks

The usage of `kotlin-http-client-atlas` is **limited to Atlassian applications** which have an application link to the
desired Jira instance.

Specify the following dependency for usage:

```xml

<dependency>
    <groupId>com.linked-planet.client</groupId>
    <artifactId>kotlin-jira-client-http</artifactId>
    <version>${jira.client.version}</version>
</dependency>
<dependency>
<groupId>com.linked-planet.client</groupId>
<artifactId>kotlin-http-client-atlas</artifactId>
<version>${http.client.version}</version>
</dependency>
```

For client initialization take a look at the [JiraApplinkClientTest](kotlin-jira-client-test-applink/src/test/kotlin/it/JiraApplinkClientTest.kt).

#### HTTP via Ktor

`kotlin-http-client-ktor` uses Ktor for HTTP communication and can be used in every environment.

Specify the following dependency for usage:

```xml

<dependency>
    <groupId>com.linked-planet.client</groupId>
    <artifactId>kotlin-jira-client-http</artifactId>
    <version>${jira.client.version}</version>
</dependency>
<dependency>
<groupId>com.linked-planet.client</groupId>
<artifactId>kotlin-http-client-ktor</artifactId>
<version>${http.client.version}</version>
</dependency>
```

For client initialization take a look at the [JiraKtorClientTest](kotlin-jira-client-test-ktor/src/test/kotlin/JiraKtorClientTest.kt).

## Project structure

The project is structured using multiple Maven modules.

### Productive modules

- **kotlin-jira-client-api**: Provides the interfaces of the client. Always implement using classes from this package.
- **kotlin-jira-client-http**: Implements the client's interfaces using a kotlin-http-client.
- **kotlin-jira-client-sdk**: Implements the client's interfaces using several Atlassian Jira SDK libraries.

### Test modules

- **kotlin-jira-client-test-base**: Defines several test cases to verify the client functionality. All those test cases
  are used in the following modules.
- **kotlin-jira-client-test-applink**: Tests the `kotlin-jira-client-http` with `kotlin-http-client-atlas` in a
  Confluence Plugin that is connected to Jira via an Applink
- **kotlin-jira-client-test-ktor**: Tests the `kotlin-jira-client-http` with `kotlin-http-client-ktor` using Ktor
- **kotlin-jira-client-test-ktor**: Tests the `kotlin-jira-client-sdk` in a Jira Plugin

### Development

#### Setting up Jira (from Scratch) for Testing

- Start Jira using the sdk__jira:debug configuration.
- Update Jira Base Url
- Add Licenses for Jira Service Management from pass. Add the different License for Jira Core.
- Install the "Jira Software Application"
- Create a new Scrum software development Project named "Test" (If it is not available, go back one step)
- Change Workflow to All->TO DO---Do it--->In PROGRESS---Did it--->DONE
- Reindex (System -> Re-Indexing)
- Add InsightObject as CustomField
    - Create Insight Object Schema ITest with Key "IT"
    - Create Object Type "Company"
    - Create a Company "Test GmbH" (must have "IT-1" as key)
