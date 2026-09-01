@component @permissions @nfr
Feature: API permissions
  Protected endpoints require a JWT, and users can only act in their own role and profile.

  Scenario: Unauthenticated callers cannot list training types
    When an unauthenticated client gets "/api/v1/training-types"
    Then the response status is 401

  Scenario: A trainee cannot read another trainee profile
    Given two registered trainees
    When the first trainee requests the second trainee profile
    Then the response status is 403

  Scenario: A trainee cannot add a training
    Given a registered trainee
    And a registered trainer
    When the trainee adds a training
    Then the response status is 403
