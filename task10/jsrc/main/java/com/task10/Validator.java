package com.task10;

import javax.mail.internet.InternetAddress;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.regex.Pattern;

public class Validator {
    // Alphanumeric + any of "$%^*-_", 12+ chars. Example: p12345T-048_Gru
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[a-zA-Z0-9$%^*\\-_]+$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static boolean invalidEmail(String email) {
        return email == null || throwsException(() -> new InternetAddress(email).validate());
    }

    public static boolean invalidPassword(String password) {
        return password == null || password.length() < 12 || !PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean invalidDate(String date) {
        return date == null || throwsException(() -> LocalDate.parse(date, DATE_FORMATTER));
    }

    public static boolean invalidTime(String time) {
        return time == null || throwsException(() -> LocalTime.parse(time, TIME_FORMATTER));
    }

    public static <T> boolean overlappingRanges(T a, T b, T c, T d, Comparator<T> comparator) {
        return comparator.compare(a, c) > 0 && comparator.compare(a, d) < 0 ||
                comparator.compare(b, c) > 0 && comparator.compare(b, d) < 0;
    }

    private static boolean throwsException(Action action) {
        try {
            action.perform();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    @FunctionalInterface
    private interface Action {
        void perform() throws Exception;
    }
}
