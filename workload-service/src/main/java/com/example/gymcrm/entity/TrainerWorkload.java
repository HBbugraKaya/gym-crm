package com.example.gymcrm.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "trainer_workloads")
@CompoundIndex(name = "idx_trainer_name", def = "{'firstName': 1, 'lastName': 1}")
public record TrainerWorkload(
    @Id
    String username,

    @Field("first_name")
    String firstName,

    @Field("last_name")
    String lastName,

    @Field("status")
    boolean status,

    @Field("years")
    List<YearSummary> years
) {
    public TrainerWorkload {
        years = (years == null) ? List.of() : List.copyOf(years);
    }

    public TrainerWorkload adjust(String firstName, String lastName, boolean status, int year, int month, int delta) {
        List<YearSummary> next = new ArrayList<>(years);
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).year() == year) {
                next.set(i, next.get(i).adjust(month, delta));
                return new TrainerWorkload(username, firstName, lastName, status, next);
            }
        }
        if (delta > 0) {
            next.add(new YearSummary(year, List.of(new MonthSummary(month, delta))));
        }
        return new TrainerWorkload(username, firstName, lastName, status, next);
    }

    public int duration(int year, int month) {
        return years.stream()
                .filter(y -> y.year() == year)
                .flatMap(y -> y.months().stream())
                .filter(m -> m.month() == month)
                .mapToInt(MonthSummary::trainingsSummaryDuration)
                .findFirst()
                .orElse(0);
    }

    public record YearSummary(
        int year,
        @Field("months") List<MonthSummary> months
    ) {
        public YearSummary {
            months = (months == null) ? List.of() : List.copyOf(months);
        }

        YearSummary adjust(int month, int delta) {
            List<MonthSummary> next = new ArrayList<>(months);
            for (int i = 0; i < next.size(); i++) {
                if (next.get(i).month() == month) {
                    next.set(i, new MonthSummary(month, Math.max(0, next.get(i).trainingsSummaryDuration() + delta)));
                    return new YearSummary(year, next);
                }
            }
            if (delta > 0) {
                next.add(new MonthSummary(month, delta));
            }
            return new YearSummary(year, next);
        }
    }

    public record MonthSummary(
        int month,
        @Field("trainings_summary_duration") int trainingsSummaryDuration
    ) {}
}
