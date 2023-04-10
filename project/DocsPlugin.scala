import BuildUtils.runCmd
import sbt.Keys.baseDirectory
import sbt.{AutoPlugin, Plugins, Setting, TaskKey, ThisBuild}
import java.io.File
import sbt.complete.DefaultParsers.spaceDelimited

//noinspection ScalaStyle
object DocsPlugin extends AutoPlugin {
  override def requires: Plugins = sbt.Plugins.empty

  override def trigger = allRequirements

  override lazy val projectSettings: Seq[Setting[_]] = Seq()

  object autoImport {
    val docsPrintHelpTask = TaskKey[Unit]("docsPrintHelp", "prints help for docs commands")
    val generateDocsTask = TaskKey[Unit]("generateDocs", "generates docs from notebooks in the repo without publishing for local development")
    val publishDocsTask = TaskKey[Unit]("publishDocs", "formats notebooks and and publishes docs")
    val argsTest = TaskKey[Unit]("argsTest", "test for parsing args")
  }

  val pythonModuleName = "documentprojection"

  def runDocumentProjectionCommand(projectRoot: File, args: Seq[String]): Unit = {
    runCmd(cmd=f"python -m $pythonModuleName".split(" ") ++ args, wd=new File(projectRoot, "docs/python"))
  }

  import autoImport._
  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    generateDocsTask := {
      val root = (ThisBuild / baseDirectory).value
      runDocumentProjectionCommand(root, Seq("--format"))
    },
    publishDocsTask := {
      val root = (ThisBuild / baseDirectory).value
      runDocumentProjectionCommand(root, Seq("--publish"))
    },
    docsPrintHelpTask := {
      val root = (ThisBuild / baseDirectory).value
      runDocumentProjectionCommand(root, Seq("--help"))
    },
  )
}
