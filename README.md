# kotlin-atlassian-client
![Maven Central](https://img.shields.io/maven-central/v/com.linked-planet.client/kotlin-jira-client-api)
[![kotlin-atlassian-client - default](https://github.com/linked-planet/kotlin-atlassian-client/actions/workflows/default.yml/badge.svg)](https://github.com/linked-planet/kotlin-atlassian-client/actions/workflows/default.yml)
![Kotlin 1.8.0](https://img.shields.io/badge/Kotlin-1.8.0-blue)
![Jira 9.4.2](https://img.shields.io/badge/Jira-9.4.2-blue)
![Confluence 7.19.5](https://img.shields.io/badge/Confluence-7.19.5-lightblue)
> *kotlin-atlassian-client* merges the libraries of the repositories *kotlin-http-client*, *kotlin-jira-client* and *kotlin-insight-client* originally implemented by @betacore.

## Docs
Provides several Kotlin clients for the interaction with Atlassian Jira and Insight:
- [kotlin-jira-client](#kotlin-jira-client)
- [kotlin-insight-client](#kotlin-insight-client)
- [kotlin-http-client](#kotlin-http-client)

### kotlin-jira-client
Provides a Kotlin client for interaction with Atlassian Jira (supported version can be seen on top). It provides management functionality for

- comments,
- issues,
- transitions,
- users and
- projects.

See [kotlin-jira-client](kotlin-jira-client/README.md) for more details.

### kotlin-insight-client

Provides a Kotlin client for interaction with Atlassian Assets / Insight.
It provides access to

- InsightObjects
- Attachments
- History
- Object types
- Schemas

See [kotlin-jira-client](kotlin-insight-client/README.md) for more details.

### kotlin-http-client

Provides an interface for several basic http operations which might be required by the other libraries.

## Development

#### Local setup & run configs

This project ships with several IntelliJ run configs for local building and testing.

All run configs **require two Java properties** otherwise they will fail:
- `jira.service.management.license`: A string containing a valid Jira license
- `confluence.license`: A string containing a valid Confluence license

##### Properties via IDE
The **recommended way** to set those properties is via the IDE project settings. 

IntelliJ:
1. Navigate to *File | Settings | Build, Execution, Deployment | Build Tools | Maven | Runner*
2. Configure the two properties

##### Properties via `.mvn/maven.config`

> :warning: Warning: Don't commit your changes to `.mvn/maven.config`!

As an alternative one might configure the properties in `.mvn/maven.config` but remember that this file is under version control. 
**Don't commit your licenses.** 

#### Releasing

> Releases should be created from **dev branch only**!

1. Transfer the desired state to the `dev` branch and wait for a successful build.
2. Create a Github release via `Releases`/`Draft a new release`.
    1. `Tag`:
        1. Prefix v and version number (see [SemVer](https://semver.org/lang/de/)), example: `v1.0.0`
        2. Choose `Create new tag`
    2. `Release title`: Version from tag and short description of changes
    3. `Description`: Detailed description of changes, Generate release notes can help with this
    4. Click `Publish release` which will start the release build
3. After a successful release build, the artifacts are available on Maven central (roughly 15 minutes later).
4. Make a pull to retrieve the last changes of the `dev` branch locally
