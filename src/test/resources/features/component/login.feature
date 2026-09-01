@component @login
Feature: Login
  Gym CRM issues a JWT for valid credentials and rejects invalid attempts.

  Background:
    Given a registered trainee

  @positive @nfr
  Scenario: Active trainee logs in
    When the trainee logs in with the correct password
    Then the response status is 200
    And the response contains an access token

  @negative @nfr
  Scenario: Wrong password is rejected
    When the trainee logs in with password "wrong-password"
    Then the response status is 401

  @negative @nfr
  Scenario: Account locks after three failed logins
    When the trainee logs in with password "wrong-password"
    And the trainee logs in with password "wrong-password"
    And the trainee logs in with password "wrong-password"
    And the trainee logs in with the correct password
    Then the response status is 423

  @negative @nfr
  Scenario: Inactive trainee cannot log in
    Given the trainee is deactivated
    When the trainee logs in with the correct password
    Then the response status is 401

  @negative
  Scenario: Login without a password is rejected
    When the trainee logs in with an empty password
    Then the response status is 400
