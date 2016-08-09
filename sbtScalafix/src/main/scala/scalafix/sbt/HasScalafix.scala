/* Modified version of
https://github.com/sbt/sbt-scalariform/blob/61a0b7b75441b458e4ff3c6c30ed87d087a2e569/src/main/scala/com/typesafe/sbt/SbtScalariform.scala

Original licence:

Copyright 2011-2012 Typesafe Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package scalafix.sbt

import scala.language.reflectiveCalls

import sbt.File
import sbt.FileFilter
import sbt.Keys.TaskStreams
import sbt.ProjectRef

import scala.collection.immutable.Seq

case class HasScalafix(reflective: ScalafixLike,
                       configFile: Option[File],
                       streams: TaskStreams,
                       sourceDirectories: Seq[File],
                       includeFilter: FileFilter,
                       excludeFilter: FileFilter,
                       ref: ProjectRef) {
  import sbt.{Future => _, _}

  def log(label: String, logger: Logger)(message: String)(count: String) =
    logger.info(message.format(count, label))

  val logFun = log(Reference.display(ref), streams.log) _

  val files =
    sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get.toSet

  def writeFormattedContentsToFiles(): Unit = {
    inc.Analysis.counted("Scala source", "", "s", files.size).foreach(logFun("Fixed %s %s ..."))
    files.foreach(handleFile(writeFixed))
  }

  private def writeFixed(result: ScalafixResult): Unit = {
    if (result.fixedContents != result.originalContents) {
      IO.write(result.file, result.fixedContents)
    }
  }

  private final case class ScalafixResult(file: File,
                                          originalContents: String,
                                          fixedContents: String)

  private def handleFile(callback: ScalafixResult => Unit)(file: File): Unit = {
    val contents = IO.read(file)
    val fixed = reflective.fix(contents, file.getPath)
    callback(ScalafixResult(file, contents, fixed))
  }
}
