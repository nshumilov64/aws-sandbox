package com.task10;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.regex.Pattern;

public class Validator {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[a-zA-Z0-9$%^*]+$");

    public static boolean validEmail(String email) {
        try {
            new InternetAddress(email).validate();
        } catch (AddressException e) {
            return false;
        }
        return true;
    }

    public static boolean validPassword(String password) {
        return password.length() >= 12 && PASSWORD_PATTERN.matcher(password).matches();
    }
}
