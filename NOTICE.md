# NOTICE v2.4.1 Coder
---

## Changes:
* **API Path**

## Reminder:
**API Path** has been changed to `dev.codestuff.coder.api.CoderAPI`, some addons may break.

## Guide:
Implementing Coder API:

# `pom.xml (Maven) Java 21`:
```xml
<project xmlns="http://apache.org"
         xmlns:xsi="http://w3.org"
         xsi:schemaLocation="http://apache.org http://apache.org">
    <modelVersion>4.0.0</modelVersion>

    <groupId>yourcompany.yourname.yourbrand</groupId>
    <artifactId>my-awesome-coder-addon</artifactId>
    <version>1.0.0</version>

    <repositories>
        <repository>
            <id>modrinth</id>
            <name>Modrinth</name>
            <url>https://api.modrinth.com/maven</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>maven.modrinth</groupId>
            <artifactId>7QMwdTys</artifactId>
            <version>2OmHB5Qv</version>
        </dependency>
    </dependencies>
</project>

```

# `pom.xml (Maven) Java 25`:
```xml
<project xmlns="http://apache.org"
         xmlns:xsi="http://w3.org"
         xsi:schemaLocation="http://apache.org http://apache.org">
    <modelVersion>4.0.0</modelVersion>

    <!-- Replace these placeholder details with your project specifics -->
    <groupId>yourcompany.yourname.yourbrand</groupId>
    <artifactId>my-coder-addon</artifactId>
    <version>1.0.0</version> <!-- Suggested Version String 'series.group.drop' -->

    <repositories>
        <repository>
            <id>modrinth</id>
            <name>Modrinth</name>
            <url>https://api.modrinth.com/maven</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>maven.modrinth</groupId>
            <artifactId>7QMwdTys</artifactId>
            <version>L7jkxaIL</version>
        </dependency>
    </dependencies>
</project>

```

# `build.gradle (Groovy) Java 21`:
```groovy
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        // forRepositories(fg.repository) // Uncomment when using ForgeGradle
        filter {
            includeGroup "maven.modrinth"
        }
    }
}

// Standard Gradle dependency
dependencies {
    implementation "maven.modrinth:7QMwdTys:2OmHB5Qv"
}
```

# `build.gradle (Groovy) Java 25`:
```groovy
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        // forRepositories(fg.repository) // Uncomment when using ForgeGradle
        filter {
            includeGroup "maven.modrinth"
        }
    }
}

// Standard Gradle dependency
dependencies {
    implementation "maven.modrinth:7QMwdTys:L7jkxaIL"
}
```

# `build.gradle.kts (Kotlin) Java 21`:
```kotlin
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        // forRepositories(fg.repository) // Uncomment when using ForgeGradle
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    implementation("maven.modrinth:7QMwdTys:2OmHB5Qv")
}
```

# `build.gradle.kts (Kotlin) Java 25`:
```kotlin
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        // forRepositories(fg.repository) // Uncomment when using ForgeGradle
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

// Standard Gradle dependency
dependencies {
    implementation("maven.modrinth:7QMwdTys:L7jkxaIL")
}
```