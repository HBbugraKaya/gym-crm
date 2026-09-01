@integration @trainer-workloads
Feature: Workload JMS contract
  Valid workload events update the trainer summary. Invalid events go to the DLQ.

  Scenario: A valid ADD message updates monthly duration
    When a workload ADD message of 45 minutes arrives for trainer "queue.coach" on 2026-08-09
    Then the monthly duration for "queue.coach" in 2026-08 becomes 45

  Scenario: An invalid workload message is moved to the DLQ
    When an invalid workload message arrives
    Then the message is moved to the trainer workload DLQ
