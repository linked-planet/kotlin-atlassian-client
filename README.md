# kotlin-atlassian-client

![Maven Central](https://img.shields.io/maven-central/v/com.linked-planet.client/kotlin-jira-client-api)
[![kotlin-atlassian-client - default](https://github.com/linked-planet/kotlin-atlassian-client/actions/workflows/default.yml/badge.svg)](https://github.com/linked-planet/kotlin-atlassian-client/actions/workflows/default.yml)

## Docs
This project provides the following libraries.
- [kotlin-jira-client](#kotlin-jira-client)

### kotlin-jira-client
Provides a Kotlin client for interaction with Atlassian Jira. It provides management functionality for

- comments,
- issues,
- transitions,
- users and
- projects.

See [kotlin-jira-client](kotlin-jira-client/README.md) for more details.

### Releasing

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
