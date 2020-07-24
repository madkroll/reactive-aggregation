package com.madkroll.aggregation.tests.gatling

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

class AggregationGrowLoadFromLowToHigh extends Simulation {

  val usersPerSecondLow = 150;
  val usersPerSecondHigh = 700;

  val rpsLow = 50;
  val rpsMedium = 100;
  val rpsHigh = 300;
  val rpsMax = 500;

  val rampUpDuration: FiniteDuration = 1 minute;
  val scenarioDuration: FiniteDuration = 4 minutes;
  val totalDuration: FiniteDuration = 15 minutes;

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
          .inject(rampUsersPerSec(usersPerSecondLow) to(usersPerSecondHigh) during (totalDuration))
          .throttle(
            jumpToRps(rpsLow),
            reachRps(rpsMedium) in (rampUpDuration), holdFor(scenarioDuration),
            reachRps(rpsHigh) in (rampUpDuration), holdFor(scenarioDuration),
            reachRps(rpsMax) in (rampUpDuration), holdFor(scenarioDuration)
          )
  ).protocols(
    http
      .baseUrl("http://localhost:8080")
      .acceptHeader("application/json")
  )
}
