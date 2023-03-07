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
package io.github.cfraser.cqldelight.gradle

import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.fail
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CQLDelightPluginTest {

  @Test
  fun `apply cqldelight and generate queries`(@TempDir directory: Path) {
    val result = directory.setupProject().execute("build")
    assertEquals(TaskOutcome.SUCCESS, result.outcome)
    assertEquals(
        CQLDelightPluginTest::class
            .java
            .classLoader
            .getResourceAsStream("MovieQueries.java")
            ?.reader()
            ?.readText(),
        directory
            .resolve(
                "build/generated/source/cqldelight/main/io/github/cfraser/cqldelight/MovieQueries.java")
            .readText())
  }

  private fun Path.setupProject(): Path = apply {
    resolve("build.gradle")
        .writeText(
            """|plugins {
            |   id 'java'
            |   id 'io.github.cfraser.cqldelight'
            |}
            |
            |repositories {
            |   mavenCentral()
            |}
            |
            |dependencies {
            |   implementation('org.neo4j.driver:neo4j-java-driver:5.6.0')
            |}
            |"""
                .trimMargin())
    CQLDelightPluginTest::class
        .java
        .classLoader
        .getResourceAsStream("movie.cypher")
        ?.copyTo(
            resolve("src/main/cypher").createDirectories().resolve("movie.cypher").outputStream())
  }

  private fun Path.execute(task: String, version: String = "7.6.1"): BuildTask =
      GradleRunner.create()
          .withPluginClasspath()
          .withProjectDir(toFile())
          .withArguments(task)
          .withGradleVersion(version)
          .withEnvironment(System.getenv())
          .forwardStdOutput(OutputStreamWriter(System.out))
          .forwardStdError(OutputStreamWriter(System.err))
          .build()
          .task(":$task")
          ?: fail("Failed to get task result")
}
