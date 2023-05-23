package com.microsoft.azure.synapse.ml.nbtest

import com.microsoft.azure.synapse.ml.build.BuildInfo
import com.microsoft.azure.synapse.ml.core.env.FileUtilities
import com.microsoft.azure.synapse.ml.core.env.PackageUtils._
import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.io.File


case class ExecutionConfig(TimeoutInMillis: Int)

case class SparkConfig(Region: String,
                       NumWorkers: Int,
                       AutoTerminationMinutes: Int,
                       TokenRoot: String,
                       AuthValueRoot: String,
                       FolderRoot: String)

case class NotebookConfig(Paths: Seq[String],
                          Exclusions: Option[Seq[String]]) {
  lazy val notebooks = {
    Paths.flatMap { notebooksDir =>
      val path = if (new File(notebooksDir).isAbsolute) notebooksDir else FileUtilities
        .join(BuildInfo.baseDirectory.getParent, notebooksDir).getCanonicalPath
      FileUtilities.recursiveListFiles(new File(path))
    }.filterNot(file => {
      val path = file.getAbsolutePath
      Exclusions.forall(exclusion => path.contains(exclusion))
    })
  }
}

case class ScenarioConfig(PoolName: String,
                          AdbRuntime: String,
                          ClusterNameRoot: String,
                          PyPiLibraries: Option[Seq[String]],
                          InitScripts: Option[Seq[String]],
                          Notebooks: NotebookConfig) {
  lazy val libraryString = (List(
    Map("maven" -> Map("coordinates" -> PackageMavenCoordinate, "repo" -> PackageRepository))
  ) ++ PyPiLibraries.getOrElse(List()).map { pkg =>
    Map("pypi" -> Map("package" -> pkg))
  }).toJson.compactPrint

  lazy val initScriptString = InitScripts.map { scriptPath =>
    Map("dbfs" -> Map("destination" -> scriptPath))
  }.toJson.compactPrint
}

case class DatabricksConfig(ExecutionConfig: ExecutionConfig,
                            SparkConfig: SparkConfig,
                            CPUConfig: ScenarioConfig,
                            GPUConfig: ScenarioConfig)

case class SynapseConfig(Notebooks: NotebookConfig)

case class NotebooksTestConfig(SynapseConfig: SynapseConfig,
                               SynapseExtensionsConfig: SynapseConfig,
                               DatabricksConfig: DatabricksConfig)

object ConfigurationLoader {
  private val DefaultConfigPath: String = BuildInfo.baseDirectory.getParent + "/notebooks/BaseTestConfiguration.json"
  val ConfigOverrideEnvVarName: String = "NOTEBOOK_TEST_CONFIG_OVERRIDE"

  def getConfig(): NotebooksTestConfig = {
    implicit def defaultHint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(PascalCase, PascalCase))

    val defaultConfig = ConfigSource.file(DefaultConfigPath)

    sys.env.get(ConfigOverrideEnvVarName) match {
      case Some(path) =>
        if (path.length > 0) {
          ConfigSource.file(path).optional.withFallback(defaultConfig).loadOrThrow[NotebooksTestConfig]
        } else {
          defaultConfig.loadOrThrow[NotebooksTestConfig]
        }
      case None =>
        defaultConfig.loadOrThrow[NotebooksTestConfig]
    }
  }
}
