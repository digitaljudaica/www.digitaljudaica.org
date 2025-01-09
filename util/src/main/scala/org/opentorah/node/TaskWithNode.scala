package org.opentorah.node

import org.gradle.api.logging.LogLevel
import org.gradle.api.{Project, Task}
import org.gradle.api.provider.{ListProperty, Property}
import org.gradle.api.tasks.Input
import org.opentorah.build.GradleBuildContextCore
import org.opentorah.build.Gradle.*
import java.io.File

trait TaskWithNode extends Task:
  @Input def getVersion: Property[String]
  @Input def getModules: ListProperty[String]
  // To avoid invoking Task.getProject at execution time, some things are at task creation:
  private val gradleUserHomeDir: File = getProject.getGradle.getGradleUserHomeDir
  private val projectDir: File = getProject.getProjectDir

  getProject.afterEvaluate((project: Project) =>
    val nodeExtension: NodeExtension = NodeExtension.get(project)
    getVersion.set(nodeExtension.getVersion)
    getModules.set(nodeExtension.getModules)
  )
  
  def node: Node = NodeDependency
    .getInstalled(
      version = getVersion.toOption, 
      context = GradleBuildContextCore(gradleUserHomeDir, getLogger)
    )
    .node(nodeModulesParent = projectDir)

  def node(arguments: String): Unit = node(arguments, LogLevel.LIFECYCLE)
  def node(arguments: String, logLevel: LogLevel): Unit = node.node(arguments, getLogger.log(logLevel, _))

  def npm(arguments: String): Unit = npm(arguments, LogLevel.LIFECYCLE)
  def npm(arguments: String, logLevel: LogLevel): Unit = node.npm(arguments, getLogger.log(logLevel, _))

  def setUpNodeProject(requiredModules: List[String]): Unit =
    val isProjectSetUp: Boolean = File(projectDir, "package.json").exists

    // Initialize Node project
    if !isProjectSetUp then npm(arguments = "init private")

    // Install Node modules
    node.mkNodeModules()
    npm(
      arguments = "install " + (requiredModules ++ getModules.toList).mkString(" "),
      logLevel = if isProjectSetUp then LogLevel.INFO else LogLevel.LIFECYCLE
    )
