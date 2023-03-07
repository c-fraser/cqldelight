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

import io.github.cfraser.cqldelight.QueryGenerator
import io.github.cfraser.cqldelight.StatementParser
import java.io.File
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.Path
import kotlin.properties.Delegates.notNull
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Namer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.GenerateIdeaModule
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.util.GradleVersion
import org.gradle.util.internal.VersionNumber

/**
 * [CQLDelightPlugin] is a [Plugin] that registers the [GenerateQueriesTask] for the `main` and
 * `test` [SourceSet] to enable the generation of executable queries from *Cypher* files.
 */
@Suppress("unused")
class CQLDelightPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.verifyRequirements()
    target.extensions
        .getByType<SourceSetContainer>()
        .run { listOfNotNull(get(MAIN_SOURCE_SET_NAME), get(TEST_SOURCE_SET_NAME)) }
        .forEach {
          val cypherSourceSet = target.createCypherSourceSet(it)
          val task = target.registerTask(it, cypherSourceSet).get()
          target.configureIntelliJ(
              it, cypherSourceSet, target.objects.fileCollection().from(task.generatedFiles))
        }
  }

  /** Verify the [Project] satisfies the requirements to apply the [CQLDelightPlugin]. */
  private fun Project.verifyRequirements() {
    require(JavaVersion.current() >= JavaVersion.VERSION_17) {
      "Cannot apply ${this::class}, Java 17 or greater is required"
    }
    require(GradleVersion.current() >= GradleVersion.version("7.0")) {
      "Cannot apply ${this::class}, Gradle version 7.0 or greater is required"
    }
    require(plugins.hasPlugin(JavaPlugin::class) || plugins.hasPlugin(JavaLibraryPlugin::class)) {
      "Cannot apply ${this::class}, a Java plugin must be applied"
    }
    afterEvaluate { project ->
      require(
          project.configurations["runtimeClasspath"]!!.allDependencies.any { dependency ->
            dependency.group == "org.neo4j.driver" &&
                dependency.name == "neo4j-java-driver" &&
                @Suppress("MagicNumber") (VersionNumber.parse(dependency.version).major >= 5)
          }) {
            "Cannot apply ${this::class}, Neo4j driver 5.x or greater is required"
          }
    }
  }

  /**
   * Create a [SourceDirectorySet] for the input *Cypher* files that extends from the [sourceSet].
   */
  private fun Project.createCypherSourceSet(sourceSet: SourceSet): SourceDirectorySet =
      objects.sourceDirectorySet("cypher", "${sourceSet.name.capitalize()} Cypher source").also {
        it.srcDir("src/${sourceSet.name}/cypher")
        it.include("**/*.cypher")
        sourceSet.extensions.add("cypher", it)
      }

  /**
   * Register the [GenerateQueriesTask] for the [cypherSourceSet] and include the
   * [GenerateQueriesTask.generatedFiles] in the [sourceSet].
   */
  private fun Project.registerTask(
      sourceSet: SourceSet,
      cypherSourceSet: FileCollection
  ): TaskProvider<GenerateQueriesTask> =
      tasks
          .register<GenerateQueriesTask>(
              sourceSet.name
                  .takeUnless { it == MAIN_SOURCE_SET_NAME }
                  ?.capitalize()
                  .run { "generate${orEmpty()}Queries" }) {
                cypherFiles = cypherSourceSet
                generatedFiles =
                    Path("${project.buildDir}/generated/source/cqldelight/${sourceSet.name}")
              }
          .apply { sourceSet.java.srcDirs(map { it.generatedFiles }) }

  /**
   * If the [IdeaPlugin] is applied to the [Project], then configure the IDE's sources to include
   * the [cypherSourceSet] and [generatedFiles] in the [sourceSet].
   */
  private fun Project.configureIntelliJ(
      sourceSet: SourceSet,
      cypherSourceSet: SourceDirectorySet,
      generatedFiles: FileCollection
  ) {
    val isTest = sourceSet.name == TEST_SOURCE_SET_NAME
    fun addSourceFile(source: File, isGenerated: Boolean) {
      val model = extensions.findByType<IdeaModel>()
      if (isTest)
          if (GradleVersion.current() >= GradleVersion.version("7.4"))
              model?.module?.testSources?.from(source)
          else @Suppress("DEPRECATION") model?.module?.testSourceDirs?.add(source)
      else model?.module?.sourceDirs?.add(source)
      if (isGenerated) model?.module?.generatedSourceDirs?.add(source)
      tasks.withType<GenerateIdeaModule>().configureEach { generateIdeaModule ->
        generateIdeaModule.doFirst { source.mkdirs() }
      }
    }
    plugins.withType<IdeaPlugin> {
      cypherSourceSet.srcDirs.forEach { addSourceFile(it, false) }
      generatedFiles.forEach { addSourceFile(it, true) }
    }
  }
}

/**
 * [GenerateQueriesTask] is a [org.gradle.api.Task] implementation to generate executable *Java*
 * queries from annotated *Cypher* statements in files.
 */
@CacheableTask
@Suppress("UnnecessaryAbstractClass")
abstract class GenerateQueriesTask : DefaultTask() {

  /** The [FileCollection] of *Cypher* [InputFiles] to generate the queries for. */
  @get:SkipWhenEmpty
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:IgnoreEmptyDirectories
  @get:InputFiles
  var cypherFiles: FileCollection by notNull()

  /** The [Path] of the [OutputDirectory] for the generated *Java* source files. */
  @get:OutputDirectory var generatedFiles: Path by notNull()

  /** The package name for the [generatedFiles]. */
  @Input
  val packageName: Property<String> =
      project.objects.property<String>().convention("io.github.cfraser.cqldelight")

  /** The [Namer] to name the generated *Java* classes from the [cypherFiles]. */
  @Internal
  val classNamer: Property<Namer<File>> =
      project.objects.property<Namer<File>>().convention {
        "${it.nameWithoutExtension.capitalize()}Queries"
      }

  /** Generate the executable *Java* queries from the statements in the [cypherFiles]. */
  @TaskAction
  fun generate() {
    val generator = QueryGenerator(generatedFiles, packageName.get())
    cypherFiles.files
        .asSequence()
        .filterNotNull()
        .filter { it.isFile }
        .sortedBy { it.nameWithoutExtension }
        .map { classNamer.get().determineName(it) to StatementParser.parse(it) }
        .forEach { (className, statements) -> generator.generate(className, statements) }
  }
}

/** Capitalize *this* [String]. */
private fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}
