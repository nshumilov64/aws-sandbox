package com.task11;

import org.junit.Assert;
import org.junit.Test;

import java.util.Comparator;
import java.util.function.Function;

public class ValidationTest {
    @Test
    public void shouldMarkReservationSlotValid() {
        ApiHandler.Reservation reservation = new ApiHandler.Reservation();
        reservation.setSlotTimeStart("12:00");
        reservation.setSlotTimeEnd("15:00");
        Assert.assertFalse(Validator.invalidTime(reservation.getSlotTimeStart()) ||
                Validator.invalidTime(reservation.getSlotTimeEnd()) ||
                reservation.getSlotTimeStart().compareTo(reservation.getSlotTimeEnd()) >= 0);
    }

    @Test
    public void shouldMarkReservationSlotsAsOverlapping() {
        ApiHandler.Reservation first = new ApiHandler.Reservation();
        first.setSlotTimeStart("12:00");
        first.setSlotTimeEnd("15:00");
        ApiHandler.Reservation second = new ApiHandler.Reservation();
        second.setSlotTimeStart("12:00");
        second.setSlotTimeEnd("15:00");
        Assert.assertTrue(Validator.overlappingRanges(first.getSlotTimeStart(), first.getSlotTimeEnd(),
                second.getSlotTimeStart(), second.getSlotTimeEnd(), Comparator.comparing(Function.identity())));
    }

    @Test
    public void shouldMarkReservationSlotsAsNonOverlapping() {
        ApiHandler.Reservation first = new ApiHandler.Reservation();
        first.setSlotTimeStart("12:00");
        first.setSlotTimeEnd("15:00");
        ApiHandler.Reservation second = new ApiHandler.Reservation();
        second.setSlotTimeStart("15:00");
        second.setSlotTimeEnd("17:00");
        Assert.assertFalse(Validator.overlappingRanges(first.getSlotTimeStart(), first.getSlotTimeEnd(),
                second.getSlotTimeStart(), second.getSlotTimeEnd(), Comparator.comparing(Function.identity())));
    }
}
