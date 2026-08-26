package com.example.gymcrm.workload.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Document(collection = "trainer_workloads")
@CompoundIndex(
        name = "trainer_name_idx",
        def = "{'trainerFirstName': 1, 'trainerLastName': 1}")
public class TrainerWorkload {
    @Id
    private String id;

    @Indexed(name = "trainer_username_idx", unique = true)
    private String trainerUsername;

    private String trainerFirstName;
    private String trainerLastName;

    @Field("isActive")
    private boolean active;

    private List<YearSummary> years = new ArrayList<>();

    protected TrainerWorkload() {
    }

    public TrainerWorkload(
            String trainerUsername,
            String trainerFirstName,
            String trainerLastName,
            boolean active) {
        this.id = usernameKey(trainerUsername);
        updateProfile(trainerUsername, trainerFirstName, trainerLastName, active);
    }

    public void updateProfile(
            String trainerUsername,
            String trainerFirstName,
            String trainerLastName,
            boolean active) {
        this.trainerUsername = trainerUsername;
        this.trainerFirstName = trainerFirstName;
        this.trainerLastName = trainerLastName;
        this.active = active;
    }

    public void addDuration(LocalDate trainingDate, int durationMinutes) {
        YearSummary yearSummary = years.stream()
                .filter(year -> year.year == trainingDate.getYear())
                .findFirst()
                .orElseGet(() -> {
                    YearSummary newYear = new YearSummary(trainingDate.getYear());
                    years.add(newYear);
                    return newYear;
                });

        yearSummary.addDuration(trainingDate.getMonthValue(), durationMinutes);
        years.sort(Comparator.comparingInt(YearSummary::getYear));
        yearSummary.months.sort(Comparator.comparingInt(MonthSummary::getMonth));
    }

    public void subtractDuration(LocalDate trainingDate, int durationMinutes) {
        YearSummary yearSummary = years.stream()
                .filter(year -> year.year == trainingDate.getYear())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Training year does not exist"));

        yearSummary.subtractDuration(trainingDate.getMonthValue(), durationMinutes);
        if (yearSummary.months.isEmpty()) {
            years.remove(yearSummary);
        }
    }

    public int durationFor(int year, int month) {
        return years.stream()
                .filter(yearSummary -> yearSummary.year == year)
                .findFirst()
                .map(yearSummary -> yearSummary.durationFor(month))
                .orElse(0);
    }

    public String getId() {
        return id;
    }

    public String getTrainerUsername() {
        return trainerUsername;
    }

    public String getTrainerFirstName() {
        return trainerFirstName;
    }

    public String getTrainerLastName() {
        return trainerLastName;
    }

    public boolean isActive() {
        return active;
    }

    public List<YearSummary> getYears() {
        return years;
    }

    private static String usernameKey(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public static class YearSummary {
        private int year;
        private List<MonthSummary> months = new ArrayList<>();

        protected YearSummary() {
        }

        private YearSummary(int year) {
            this.year = year;
        }

        private void addDuration(int month, int durationMinutes) {
            MonthSummary monthSummary = months.stream()
                    .filter(existingMonth -> existingMonth.month == month)
                    .findFirst()
                    .orElseGet(() -> {
                        MonthSummary newMonth = new MonthSummary(month);
                        months.add(newMonth);
                        return newMonth;
                    });
            monthSummary.trainingSummaryDuration += durationMinutes;
        }

        private void subtractDuration(int month, int durationMinutes) {
            MonthSummary monthSummary = months.stream()
                    .filter(existingMonth -> existingMonth.month == month)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Training month does not exist"));

            int remaining = monthSummary.trainingSummaryDuration - durationMinutes;
            if (remaining < 0) {
                throw new IllegalArgumentException("Cannot subtract more duration than recorded");
            }
            if (remaining == 0) {
                months.remove(monthSummary);
            } else {
                monthSummary.trainingSummaryDuration = remaining;
            }
        }

        private int durationFor(int month) {
            return months.stream()
                    .filter(monthSummary -> monthSummary.month == month)
                    .mapToInt(monthSummary -> monthSummary.trainingSummaryDuration)
                    .findFirst()
                    .orElse(0);
        }

        public int getYear() {
            return year;
        }

        public List<MonthSummary> getMonths() {
            return months;
        }
    }

    public static class MonthSummary {
        private int month;
        private int trainingSummaryDuration;

        protected MonthSummary() {
        }

        private MonthSummary(int month) {
            this.month = month;
        }

        public int getMonth() {
            return month;
        }

        public int getTrainingSummaryDuration() {
            return trainingSummaryDuration;
        }
    }
}
