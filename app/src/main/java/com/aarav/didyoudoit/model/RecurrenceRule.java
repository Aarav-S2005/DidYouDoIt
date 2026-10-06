package com.aarav.didyoudoit.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Detailed specification of a task's recurrence pattern.
 * Implements FR-01, FR-03.
 */
public class RecurrenceRule {

    private final RecurrenceType type;
    private final LocalTime defaultTime;
    private final DayOfWeek dayOfWeek; // used when type == WEEKLY

    public RecurrenceRule(RecurrenceType type, LocalTime defaultTime, DayOfWeek dayOfWeek) {
        this.type = Objects.requireNonNullElse(type, RecurrenceType.NONE);
        this.defaultTime = Objects.requireNonNullElse(defaultTime, LocalTime.of(12, 0));
        this.dayOfWeek = dayOfWeek;
    }

    public static RecurrenceRule none() {
        return new RecurrenceRule(RecurrenceType.NONE, LocalTime.of(12, 0), null);
    }

    public static RecurrenceRule daily(LocalTime time) {
        return new RecurrenceRule(RecurrenceType.DAILY, time, null);
    }

    public static RecurrenceRule weekdays(LocalTime time) {
        return new RecurrenceRule(RecurrenceType.WEEKDAYS, time, null);
    }

    public static RecurrenceRule weekends(LocalTime time) {
        return new RecurrenceRule(RecurrenceType.WEEKENDS, time, null);
    }

    public static RecurrenceRule weekly(LocalTime time, DayOfWeek dayOfWeek) {
        return new RecurrenceRule(RecurrenceType.WEEKLY, time, dayOfWeek);
    }

    public RecurrenceType getType() {
        return type;
    }

    public LocalTime getDefaultTime() {
        return defaultTime;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public boolean appliesToDate(LocalDate date) {
        if (date == null || type == RecurrenceType.NONE) {
            return false;
        }
        DayOfWeek dow = date.getDayOfWeek();
        return switch (type) {
            case DAILY -> true;
            case WEEKDAYS -> dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            case WEEKENDS -> dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
            case WEEKLY -> dayOfWeek == null || dow == dayOfWeek;
            case NONE -> false;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecurrenceRule that)) return false;
        return type == that.type &&
                Objects.equals(defaultTime, that.defaultTime) &&
                dayOfWeek == that.dayOfWeek;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, defaultTime, dayOfWeek);
    }

    @Override
    public String toString() {
        return "RecurrenceRule{" +
                "type=" + type +
                ", defaultTime=" + defaultTime +
                ", dayOfWeek=" + dayOfWeek +
                '}';
    }
}
