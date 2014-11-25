package com.chillenious.common.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;

/**
 * Utilities to bridge between the old java.util.Date and new Java 8 date classes.
 * <p>
 * Typically, you should use the methods that take a clock argument so that you can vary the clock
 * e.g. when you work in test cases. But if you know for sure you don't have to do this, the
 * methods that use the default clock are convenient.
 */
public final class DateUtils {

    private static final Clock DEFAULT_CLOCK = Clock.systemUTC();

    private DateUtils() {
    }

    /**
     * @return passed in date converted to local date time using the provided clock
     */
    public static LocalDateTime localDateTime(Date date, Clock clock) {
        return LocalDateTime.ofInstant(date.toInstant(), clock.getZone());
    }

    /**
     * @return passed in date converted to local date time using the default clock
     */
    public static LocalDateTime localDateTime(Date date) {
        return localDateTime(date, DEFAULT_CLOCK);
    }

    /**
     * @return passed in date converted to local date using the provided clock
     */
    public static LocalDate localDate(Date date, Clock clock) {
        return localDateTime(date, clock).toLocalDate();
    }

    /**
     * @return passed in date converted to local date using the default clock
     */
    public static LocalDate localDate(Date date) {
        return localDate(date, DEFAULT_CLOCK);
    }

    /**
     * @return passed in local date time converted to date using the provided clock
     */
    public static Date date(LocalDateTime date, Clock clock) {
        return Date.from(date.atZone(clock.getZone()).toInstant());
    }

    /**
     * @return passed in local date time converted to date using the default clock
     */
    public static Date date(LocalDateTime date) {
        return date(date, DEFAULT_CLOCK);
    }

    /**
     * @return passed in local date converted to date using the provided clock
     */
    public static Date date(LocalDate date, Clock clock) {
        return Date.from(date.atStartOfDay().atZone(clock.getZone()).toInstant());
    }

    /**
     * @return passed in local date converted to date using the default clock
     */
    public static Date date(LocalDate date) {
        return date(date, DEFAULT_CLOCK);
    }

    /**
     * @return date object based on local date with the default clock
     */
    public static Date today() {
        return date(LocalDate.now(DEFAULT_CLOCK));
    }

    /**
     * @return date object based on local date with the provided clock
     */
    public static Date today(Clock clock) {
        return date(LocalDate.now(clock));
    }

    /**
     * Builder to help expressing some basic date calculations a bit nicer.
     */
    public static final class DateDiffBuilder {

        final Clock clock;

        final int sign;

        public DateDiffBuilder(Clock clock, int sign) {
            this.clock = clock;
            this.sign = sign;
        }

        public Date diff(long diff, TemporalUnit temporalUnit) {
            return DateUtils.date(LocalDate.now(clock).plus(diff * sign, temporalUnit));
        }

        public Date days(int days) {
            return diff(days, ChronoUnit.DAYS);
        }

        public Date weeks(int weeks) {
            return diff(weeks, ChronoUnit.WEEKS);
        }

        public Date months(int months) {
            return diff(months, ChronoUnit.MONTHS);
        }

        public Date years(int years) {
            return diff(years, ChronoUnit.YEARS);
        }
    }

    public static DateDiffBuilder todayMinus(Clock clock) {
        return new DateDiffBuilder(clock, -1);
    }

    public static DateDiffBuilder todayMinus() {
        return todayMinus(DEFAULT_CLOCK);
    }

    public static DateDiffBuilder todayPlus(Clock clock) {
        return new DateDiffBuilder(clock, 1);
    }

    public static DateDiffBuilder todayPlus() {
        return todayPlus(DEFAULT_CLOCK);
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now(DEFAULT_CLOCK);
    }

    public static ZonedDateTime now(Clock clock) {
        return ZonedDateTime.now(clock);
    }
}
