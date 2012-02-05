package com.gu

import sbt._
import org.scalatools.testing.{Event => TEvent, Result => TResult}

import Keys._

object TeamCityTestReporting extends Plugin {
  override def settings = Seq(
    testListeners ++= TeamCityTestListener.ifRunningUnderTeamCity
  )
}

class TeamCityTestListener extends TestReportListener {
  /** called for each class or equivalent grouping */
  def startGroup(name: String) {
    // we can't report to teamcity that a test group has started here,
    // because even if parallel test execution is enabled there may be multiple
    // projects running tests at the same time. So if you tell TC that a test
    // group has started, the tests from different projects will get mixed up.
  }

  /** called before each test method or equivalent */
  def startTest(testName: String) {
    teamcityReport("testStarted", "name" -> testName)
  }

  /** called for each test method or equivalent */
  def testEvent(event: TestEvent) { }

  /** called after each test method or equivalent */
  def endTest(event: TestEvent) {
    for (e: TEvent <- event.detail) {
      e.result match {
        case TResult.Success => // nothing extra to report
        case TResult.Error | TResult.Failure =>
          teamcityReport("testFailed",
            "name" -> e.testName,
            "details" -> (e.error.toString +
              "\n" + e.error.getStackTrace.mkString("\n at ", "\n at ", "")))
        case TResult.Skipped =>
          teamcityReport("testIgnored", "name" -> e.testName)
      }

      teamcityReport("testFinished", "name" -> e.testName)

    }
  }


  /** called if there was an error during test */
  def endGroup(name: String, t: Throwable) { }
  /** called if test completed */
  def endGroup(name: String, result: TestResult.Value) { }


  // http://confluence.jetbrains.net/display/TCD65/Build+Script+Interaction+with+TeamCity
  def tidy(s: String) = s
    .replace("|", "||")
    .replace("'", "|'")
    .replace("\n", "|n")
    .replace("\r", "|r")
    .replace("\u0085", "|x")
    .replace("\u2028", "|l")
    .replace("\u2029", "|p")
    .replace("[", "|[")
    .replace("]", "|]")

  private def teamcityReport(messageName: String, attributes: (String, String)*) {
    println("##teamcity[" + messageName + " " + attributes.map {
      case (k, v) => k + "='" + tidy(v) + "'"
    }.mkString(" ") + "]")
  }
}

object TeamCityTestListener {
  // teamcity se
  private lazy val teamCityProjectName = Option(System.getenv("TEAMCITY_PROJECT_NAME"))
  lazy val ifRunningUnderTeamCity = teamCityProjectName.map(ignore => new TeamCityTestListener).toSeq
}



