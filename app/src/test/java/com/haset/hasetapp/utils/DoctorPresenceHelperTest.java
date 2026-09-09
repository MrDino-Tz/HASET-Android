package com.haset.hasetapp.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Patient-side online gate used by DoctorAdapter + Instant Chat booking.
 */
public class DoctorPresenceHelperTest {

    @Test
    public void effectivelyOnline_requiresFreshHeartbeat() {
        long fresh = System.currentTimeMillis() - 30_000L;
        assertTrue(DoctorPresenceHelper.isEffectivelyOnline(true, "online", fresh));
        assertTrue(DoctorPresenceHelper.isEffectivelyOnline(true, "busy", fresh));
        assertTrue(DoctorPresenceHelper.isEffectivelyOnline(true, null, fresh));
    }

    @Test
    public void effectivelyOnline_falseWhenToggleOffOrStaleOrMissing() {
        long fresh = System.currentTimeMillis() - 30_000L;
        long stale = System.currentTimeMillis() - (Constants.DOCTOR_PRESENCE_TIMEOUT_MS + 5_000L);
        assertFalse(DoctorPresenceHelper.isEffectivelyOnline(false, "online", fresh));
        assertFalse(DoctorPresenceHelper.isEffectivelyOnline(true, "offline", fresh));
        assertFalse(DoctorPresenceHelper.isEffectivelyOnline(true, "online", 0L));
        assertFalse(DoctorPresenceHelper.isEffectivelyOnline(true, "online", stale));
    }

    @Test
    public void effectivelyOnline_allowsSmallClockSkew() {
        long ahead = System.currentTimeMillis() + 5_000L;
        assertTrue(DoctorPresenceHelper.isEffectivelyOnline(true, "online", ahead));
    }

    @Test
    public void effectivelyOnline_trueWithinTwoMinuteWindow() {
        long almostExpired = System.currentTimeMillis() - (Constants.DOCTOR_PRESENCE_TIMEOUT_MS - 1_000L);
        assertTrue(DoctorPresenceHelper.isEffectivelyOnline(true, "online", almostExpired));
    }
}
