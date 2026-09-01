@integration
Feature: Training events reach other services
  Creating or cancelling a training publishes a workload event. Deleting a trainee
  publishes a deletion report.

  Background:
    Given a registered trainee
    And a registered trainer

  @trainings
  Scenario: Adding a training publishes an ADD workload event
    When the trainer adds a training
    Then a workload event with action "ADD" is published for the trainer

  @trainings
  Scenario: Cancelling a training publishes a DELETE workload event
    When the trainer adds a training
    And the trainer cancels the last training
    Then a workload event with action "DELETE" is published for the trainer

  @trainees
  Scenario: Deleting a trainee publishes a deletion report
    When the trainee deletes their profile
    Then a trainee deletion report is published for the trainee
