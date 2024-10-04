package com.task10;

import org.junit.Assert;
import org.junit.Test;

public class ValidationTest {
    @Test
    public void shouldReportReservationAsValid() {
        ApiHandler.Reservation reservation = new ApiHandler.Reservation();
        reservation.setSlotTimeStart("12:00");
        reservation.setSlotTimeEnd("15:00");
        Assert.assertFalse(Validator.invalidTime(reservation.getSlotTimeStart()) ||
                Validator.invalidTime(reservation.getSlotTimeEnd()) ||
                reservation.getSlotTimeStart().compareTo(reservation.getSlotTimeEnd()) >= 0);
    }
}
