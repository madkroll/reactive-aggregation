package com.madkroll.aggregation.tests.gatling

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

class AggregationGrowLoadFromLowToHigh extends Simulation {

  /**
   * Setup:
   * RPS:
   *  - Low: 5
   *  - Medium: 50
   *  - High: 100
   *  - Max: 500
   * Max latency: 10 seconds
   * Max allowed error rate: 0
   * Test duration: 30 minutes
   **/

  val totalUsers = 100;

  val rpsLow = 5;
  val rpsMedium = 50;
  val rpsHigh = 100;
  val rpsMax = 500;

  val rampUpDuration: FiniteDuration = 10 seconds;
  val scenarioDuration: FiniteDuration = 30 seconds;
  val totalDuration: FiniteDuration = 30 seconds;

  val countries: Array[String] = Countries.countries

  def pickNRandomCountries(): String = {
    Seq.fill(Random.nextInt(12))(countries(Random.nextInt(countries.length))).mkString(",")
  }

  def pickNRandomNumbers(): String = {
    Seq.fill(Random.nextInt(12))(Random.nextInt(200)).mkString(",")
  }

  var randomQueryParams =
    Iterator.continually(
      Map(
        "pricing" -> pickNRandomCountries(),
        "track" -> pickNRandomNumbers(),
        "shipments" -> pickNRandomNumbers()
      )
    )

  val aggregationScenario: ScenarioBuilder =
    scenario("AggregationGrowLoadFromLowToHigh")
      .feed(randomQueryParams)
      .exec(
        http("aggregation")
          .get("/aggregation?pricing=${pricing}&track=${track}&shipments=${shipments}")
          .check(status.is(200))
      )

  setUp(
    aggregationScenario
//      .inject(atOnceUsers(5))
          .inject(rampUsers(totalUsers) during (totalDuration))
          .throttle(reachRps(rpsLow) in (rampUpDuration), holdFor(scenarioDuration))

  ).protocols(
    http
      .baseUrl("http://localhost:8080")
      .acceptHeader("application/json")
  )
}
