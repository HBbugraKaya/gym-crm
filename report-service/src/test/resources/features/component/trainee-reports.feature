@component @reports @permissions @nfr
Feature: Trainee deletion report permissions
  Only a trainee JWT can record a deletion. The GET endpoint is not available to
  application roles.

  Scenario: Trainee role can record a deletion
    When a client with role "ROLE_TRAINEE" records a deletion for "runner.one"
    Then the response status is 200

  Scenario: Trainer role cannot record a deletion
    When a client with role "ROLE_TRAINER" records a deletion for "runner.one"
    Then the response status is 403

  Scenario: Unauthenticated client cannot record a deletion
    When an unauthenticated client records a deletion for "runner.one"
    Then the response status is 401

  Scenario: Authenticated clients cannot read deletion reports
    When a client with role "ROLE_TRAINEE" lists deletion reports
    Then the response status is 403

  @negative
  Scenario: Invalid deletion payload is rejected
    When a client with role "ROLE_TRAINEE" records a deletion with an empty username
    Then the response status is 400
