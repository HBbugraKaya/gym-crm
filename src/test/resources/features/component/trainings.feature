@component @trainings
Feature: Trainings
  Trainers can add and cancel sessions. Unknown resources and invalid input fail clearly.

  Background:
    Given a registered trainee
    And a registered trainer

  @positive
  Scenario: Trainer adds a training
    When the trainer adds a training
    Then the response status is 200

  @negative
  Scenario: Adding a training for an unknown trainee fails
    When the trainer adds a training for trainee "nobody.here"
    Then the response status is 404

  @negative
  Scenario: Invalid training duration is rejected
    When the trainer adds a training lasting 0 minutes
    Then the response status is 400

  @negative
  Scenario: Cancelling an unknown training fails
    When the trainer cancels training 999999
    Then the response status is 404
