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
package io.github.cfraser.cqldelight

import java.io.File
import kotlin.properties.Delegates.notNull
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType

/** [StatementParser] parses the generatable statements from a *Cypher* file. */
internal object StatementParser {

  /** The prefix denoting a generatable statement. */
  private const val prefix = "//cqldelight:"

  /** The [Regex] to match/capture a generatable statement. */
  private val regex = Regex("$prefix(?<options>.*)(?:\\n|\\r\\n|\\r\\r)(?<text>[\\s\\S]*?);")

  /** Parse the generatable statements from the [source] *Cypher* [File]. */
  fun parse(source: File): Sequence<Statement> {
    val text = source.readText()
    val lines = text.lines()
    return text.let(regex::findAll).mapNotNull(::extractStatement).map { statement ->
      statement.apply {
        file = source.name
        lineNumber =
            checkNotNull(
                lines
                    .indexOfFirst { line -> "$prefix${options.queryName}" in line }
                    .takeUnless { it == -1 }) + 1
      }
    }
  }

  /** Extract the [Statement], or `null`, from the [MatchResult]. */
  @Suppress("ReturnCount")
  private fun extractStatement(result: MatchResult): Statement? {
    val options = result.groups["options"]?.value?.let(Options::parse) ?: return null
    val text = result.groups["text"]?.value ?: return null
    return Statement(options, text)
  }
}

/**
 * A [Statement], parsed from a *Cypher* file, that can be generated.
 *
 * @property options the statement generation options
 * @property text the *Cypher* statement text
 * @property file the [File.getName] the [Statement] was parsed from
 * @property lineNumber the line number of the statement in the [file]
 */
internal data class Statement(val options: Options, val text: String) {

  var file: String by notNull()
  var lineNumber: Int by notNull()
}

/** The code generation options. */
internal data class Options(val queryName: String) {

  companion object {

    /** Parse the [Options] from the [arguments]. */
    fun parse(arguments: String): Options {
      val parser = ArgParser("cqldelight")
      val queryName by parser.argument(ArgType.String, description = "The name of the query")
      parser.parse(arguments.split(' ').toTypedArray())
      return Options((queryName))
    }
  }
}
