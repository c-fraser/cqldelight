/*
Copyright 2023 c-fraser

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import com.diffplug.gradle.spotless.SpotlessExtension
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  alias(libs.plugins.spotless)
  alias(libs.plugins.detekt)
  alias(libs.plugins.nexus.publish)
  alias(libs.plugins.jreleaser)
  alias(libs.plugins.dependency.versions)
  alias(libs.plugins.kover)
  alias(libs.plugins.compatibility.validator)
  `java-gradle-plugin`
  `maven-publish`
  signing
}

allprojects {
  group = "io.github.c-fraser"
  version = "0.1.0"
}

gradlePlugin {
  plugins {
    create(rootProject.name) {
      id = "io.github.cfraser.cqldelight"
      displayName = "CQLDelight gradle plugin"
      description = "Generate executable statements from Cypher"
      implementationClass = "io.github.cfraser.${rootProject.name}.gradle.CQLDelightPlugin"
    }
  }

  isAutomatedPublishing = false
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17

  withSourcesJar()
}

repositories { mavenCentral() }

dependencies {
  implementation(gradleKotlinDsl())
  implementation(libs.jackson)
  implementation(libs.javapoet)
  implementation(libs.kotlin.cli)
  implementation(libs.neo4j.cypher.parser)
  implementation(libs.neo4j.java.driver)

  testImplementation(gradleTestKit())
  testImplementation(kotlin("test"))
  testImplementation(libs.junit.jupiter)
}

val kotlinSourceFiles = fileTree(rootProject.rootDir) { include("src/**/*.kt") }

configure<SpotlessExtension> {
  val ktfmtVersion = libs.versions.ktfmt.get()
  val licenseHeader =
      """
      /*
      Copyright 2023 c-fraser
      
      Licensed under the Apache License, Version 2.0 (the "License");
      you may not use this file except in compliance with the License.
      You may obtain a copy of the License at
      
          https://www.apache.org/licenses/LICENSE-2.0
      
      Unless required by applicable law or agreed to in writing, software
      distributed under the License is distributed on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      See the License for the specific language governing permissions and
      limitations under the License.
      */
      """
          .trimIndent()

  kotlin {
    ktfmt(ktfmtVersion)
    licenseHeader(licenseHeader)
    target(kotlinSourceFiles)
  }

  kotlinGradle {
    ktfmt(ktfmtVersion)
    licenseHeader(licenseHeader, "(import|buildscript|plugins|rootProject)")
    target(fileTree(rootProject.rootDir) { include("**/*.gradle.kts") })
  }
}

publishing {
  val javadocJar by
      tasks.registering(Jar::class) {
        val dokkaJavadoc by tasks.getting(DokkaTask::class)
        dependsOn(dokkaJavadoc)
        archiveClassifier.set("javadoc")
        from(dokkaJavadoc.outputDirectory.get())
      }

  publications {
    create<MavenPublication>("maven") {
      from(project.components["java"])
      artifact(javadocJar)
      pom {
        name.set(rootProject.name)
        description.set("${rootProject.name}-${project.version}")
        url.set("https://github.com/c-fraser/${rootProject.name}")
        inceptionYear.set("2023")

        issueManagement {
          system.set("GitHub")
          url.set("https://github.com/c-fraser/${rootProject.name}/issues")
        }

        licenses {
          license {
            name.set("The Apache Software License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
          }
        }

        developers {
          developer {
            id.set("c-fraser")
            name.set("Chris Fraser")
          }
        }

        scm {
          url.set("https://github.com/c-fraser/${rootProject.name}")
          connection.set("scm:git:git://github.com/c-fraser/${rootProject.name}.git")
          developerConnection.set("scm:git:ssh://git@github.com/c-fraser/${rootProject.name}.git")
        }
      }
    }
  }

  signing {
    publications.withType<MavenPublication>().all mavenPublication@{
      useInMemoryPgpKeys(System.getenv("GPG_SIGNING_KEY"), System.getenv("GPG_PASSWORD"))
      sign(this@mavenPublication)
    }
  }
}

configure<NexusPublishExtension> {
  repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
      username.set(System.getenv("SONATYPE_USERNAME"))
      password.set(System.getenv("SONATYPE_PASSWORD"))
    }
  }
}

configure<JReleaserExtension> {
  project {
    authors.set(listOf("c-fraser"))
    license.set("Apache-2.0")
    extraProperties.put("inceptionYear", "2023")
    description.set("Cypher DSL for Kotlin (JVM)")
    links { homepage.set("https://github.com/c-fraser/${rootProject.name}") }
  }

  release {
    github {
      repoOwner.set("c-fraser")
      overwrite.set(true)
      token.set(System.getenv("GITHUB_TOKEN").orEmpty())
      changelog {
        formatted.set(Active.ALWAYS)
        format.set("- {{commitShortHash}} {{commitTitle}}")
        contributors.enabled.set(false)
        for (status in listOf("added", "changed", "fixed", "removed")) {
          labeler {
            label.set(status)
            title.set(status)
          }
          category {
            title.set(status.capitalizeAsciiOnly())
            labels.set(listOf(status))
          }
        }
      }
    }
  }
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "${JavaVersion.VERSION_17}"
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
  }

  withType<Jar> {
    manifest { attributes("Automatic-Module-Name" to "io.github.cfraser.${rootProject.name}") }
  }

  test {
    dependsOn(spotlessApply)
    useJUnitPlatform()
    testLogging {
      showExceptions = true
      showStandardStreams = true
      events = setOf(TestLogEvent.FAILED, TestLogEvent.PASSED)
      exceptionFormat = TestExceptionFormat.FULL
    }
  }

  withType<Detekt> {
    jvmTarget = "${JavaVersion.VERSION_17}"
    buildUponDefaultConfig = true
    source = kotlinSourceFiles
  }

  spotlessApply { dependsOn(detektMain, detektTest) }
}
