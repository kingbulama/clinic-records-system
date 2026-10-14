package app.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boundary/edge-case tests for the temporal conflict predicate.
 * overlaps() itself is O(1); these tests run in constant time per case.
 */
class AppointmentServiceTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 20, 9, 0);
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 10, 20, 9, 30);

    @Test
    void identicalSlotsOverlap() {
        assertTrue(AppointmentService.overlaps(T0, T1, T0, T1));
    }

    @Test
    void partialOverlapAtStart() {
        // new 08:45-09:15 vs existing 09:00-09:30
        assertTrue(AppointmentService.overlaps(T0.minusMinutes(15), T0.plusMinutes(15), T0, T1));
    }

    @Test
    void partialOverlapAtEnd() {
        // new 09:15-09:45 vs existing 09:00-09:30
        assertTrue(AppointmentService.overlaps(T0.plusMinutes(15), T1.plusMinutes(15), T0, T1));
    }

    @Test
    void newSlotInsideExistingOverlaps() {
        assertTrue(AppointmentService.overlaps(T0.plusMinutes(5), T0.plusMinutes(20), T0, T1));
    }

    @Test
    void existingInsideNewSlotOverlaps() {
        assertTrue(AppointmentService.overlaps(T0.minusMinutes(30), T1.plusMinutes(30), T0, T1));
    }

    @Test
    void backToBackDoesNotOverlap() {
        // new ends exactly when existing starts — allowed (half-open intervals)
        assertFalse(AppointmentService.overlaps(T0.minusMinutes(30), T0, T0, T1));
        // new starts exactly when existing ends — allowed
        assertFalse(AppointmentService.overlaps(T1, T1.plusMinutes(30), T0, T1));
    }

    @Test
    void disjointSlotsDoNotOverlap() {
        assertFalse(AppointmentService.overlaps(T0.minusHours(2), T0.minusHours(1), T0, T1));
        assertFalse(AppointmentService.overlaps(T1.plusHours(1), T1.plusHours(2), T0, T1));
    }

    @Test
    void invalidRangeRejected() {
        assertEquals("End time must be after start time.",
                AppointmentService.validateSlot(T1, T0));
        assertEquals("End time must be after start time.",
                AppointmentService.validateSlot(T0, T0));
    }

    @Test
    void missingTimesRejected() {
        assertEquals("Start and end times are required.",
                AppointmentService.validateSlot(null, T1));
    }

    @Test
    void pastSlotsRejected() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        assertEquals("Cannot book appointments in the past.",
                AppointmentService.validateSlot(past, past.plusMinutes(30)));
    }

    @Test
    void validFutureSlotAccepted() {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        assertNull(AppointmentService.validateSlot(future, future.plusMinutes(30)));
    }

    @Test
    void onlyScheduledAppointmentsCanChangeStatus() {
        assertTrue(AppointmentService.canTransition("scheduled", "cancelled"));
        assertTrue(AppointmentService.canTransition("scheduled", "completed"));
        assertFalse(AppointmentService.canTransition("completed", "cancelled"));
        assertFalse(AppointmentService.canTransition("cancelled", "completed"));
        assertFalse(AppointmentService.canTransition("scheduled", "bogus"));
    }
}
