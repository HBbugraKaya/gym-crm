@integration @reports
Feature: Trainee deletion report JMS contract
  Valid deletion events are stored. Invalid events go to the DLQ.

  Scenario: A valid deletion message is stored
    When a trainee deletion message arrives for "jms.runner"
    Then a deletion report exists for "jms.runner"

  Scenario: An invalid deletion message is moved to the DLQ
    When an invalid trainee deletion message arrives
    Then the message is moved to the trainee deletion report DLQ
