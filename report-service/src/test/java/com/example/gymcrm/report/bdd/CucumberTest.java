package com.example.gymcrm.report.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "classpath:features",
        glue = "com.example.gymcrm.report.bdd",
        plugin = {"pretty", "summary"},
        tags = "not @ignore")
public class CucumberTest {
}
