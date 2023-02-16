# kotlin-jira-client

![Maven Central](https://img.shields.io/maven-central/v/com.linked-planet/kotlin-jira-client)
[![kotlin-jira-client - default](https://github.com/linked-planet/kotlin-jira-client/actions/workflows/default.yml/badge.svg)](https://github.com/linked-planet/kotlin-jira-client/actions/workflows/default.yml)

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
in [AbstractMainTest](../blob/master/kotlin-jira-client-test-base/src/main/kotlin/com/linkedplanet/kotlinjiraclient/AbstractMainTest.kt).

### SDK

Add the following dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>com.linked-planet</groupId>
    <artifactId>kotlin-jira-client-sdk</artifactId>
    <version>${jira.client.version}</version>
</dependency>
```

For client initialization take a look at
the [SDKClientTest](../blob/master/kotlin-jira-client-test-sdk/src/test/kotlin/it/SdkClientTest.kt).

### HTTP

The HTTP variant requires an implementation
of [kotlin-http-client](https://github.com/linked-planet/kotlin-http-client).

#### HTTP via Atlassian Applinks

The usage of `kotlin-http-client-atlas` is **limited to Atlassian applications** which have an application link to the
desired Jira instance.

Specify the following dependency for usage:

```xml

<dependency>
    <groupId>com.linked-planet</groupId>
    <artifactId>kotlin-jira-client-http</artifactId>
    <version>${jira.client.version}</version>
</dependency>
<dependency>
<groupId>com.linked-planet</groupId>
<artifactId>kotlin-http-client-atlas</artifactId>
<version>${http.client.version}</version>
</dependency>
```

For client initialization take a look at
the [ApplinkClientTest](../blob/master/kotlin-jira-client-test-applink/src/test/kotlin/it/ApplinkClientTest.kt).

#### HTTP via Ktor

`kotlin-http-client-ktor` uses Ktor for HTTP communication and can be used in every environment.

Specify the following dependency for usage:

```xml

<dependency>
    <groupId>com.linked-planet</groupId>
    <artifactId>kotlin-jira-client-http</artifactId>
    <version>${jira.client.version}</version>
</dependency>
<dependency>
<groupId>com.linked-planet</groupId>
<artifactId>kotlin-http-client-ktor</artifactId>
<version>${http.client.version}</version>
</dependency>
```

For client initialization take a look at
the [KtorClientTest](../blob/master/kotlin-jira-client-test-ktor/src/test/kotlin/KtorClientTest.kt).

## Releases

## Project structure

The project is structured using multiple Maven modules.

### Productive modules

- **kotlin-jira-client-api**: Provides the interfaces of the client. Always implement using classes from this package.
- **kotlin-jira-client-http**: Implements the client's interfaces using
  a [kotlin-http-client](https://github.com/linked-planet/kotlin-http-client).
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

#### Releasing

> Releases should be created from **master branch only**!

1. Transfer the desired state to the `master` branch and wait for a successful build.
2. Create a Github release via `Releases`/`Draft a new release`.
    1. `Tag`:
        1. Prefix v and version number (see [SemVer](https://semver.org/lang/de/)), example: `v1.0.0`
        2. Choose `Create new tag`
    2. `Release title`: Version from tag and short description of changes
    3. `Description`: Detailed description of changes, Generate release notes can help with this
    4. Click `Publish release` which will start the release build
3. After a successful release build, the artifacts are available on Maven central (roughly 15 minutes later).
4. Make a pull to retrieve the last changes of the `master` branch locally
