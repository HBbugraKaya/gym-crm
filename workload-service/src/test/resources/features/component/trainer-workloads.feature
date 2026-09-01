@component @trainer-workloads
Feature: Trainer workload HTTP API
  Authenticated clients can record and read monthly workload. Missing data and
  invalid input are rejected.

  @positive
  Scenario: Authenticated client records an ADD
    When an authenticated client records an ADD of 60 minutes for trainer "coach.one" in 2026-08
    Then the response status is 200
    When an authenticated client gets the monthly workload for "coach.one" in 2026-08
    Then the response status is 200
    And the monthly duration is 60

  @negative @nfr
  Scenario: Unauthenticated client is rejected
    When an unauthenticated client gets "/api/v1/trainer-workloads/coach.one/summary"
    Then the response status is 401

  @negative
  Scenario: Unknown trainer summary is not found
    When an authenticated client gets the summary for "missing.coach"
    Then the response status is 404

  @negative
  Scenario: Invalid month is rejected
    When an authenticated client gets the monthly workload for "coach.one" in 2026-13
    Then the response status is 400

  @negative
  Scenario: Deleting more minutes than recorded is rejected
    When an authenticated client records an ADD of 30 minutes for trainer "coach.two" in 2026-08
    And an authenticated client records a DELETE of 60 minutes for trainer "coach.two" in 2026-08
    Then the response status is 400
