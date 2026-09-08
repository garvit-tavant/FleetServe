package com.example.backend.SLA.status;

import java.util.EnumSet;
import java.util.Set;

/**
 * The lifecycle of a breakdown request.
 *
 * <p>These names are the values allowed by {@code ck_breakdown_status}. Note the
 * two spellings that commonly trip people up: the terminal success state is
 * RESOLVED, not COMPLETED, and CANCELLED carries two Ls.
 */
public enum BreakdownStatus {

    /** Raised by a depot supervisor and not yet triaged. The initial state. */
    REPORTED,

    /** Assessed, with the required skill, capability and duration recorded. */
    TRIAGED,

    /** A workshop slot is held for it. */
    BOOKED,

    /** A technician has started the work. */
    IN_PROGRESS,

    /** Terminal: the work finished. */
    RESOLVED,

    /** Terminal: abandoned without the work being done. */
    CANCELLED;

    private static final Set<BreakdownStatus> TERMINAL =
            EnumSet.of(RESOLVED, CANCELLED);

    private static final Set<BreakdownStatus> BOOKABLE =
            EnumSet.of(REPORTED, TRIAGED);

    /** Terminal states no longer run a service-level clock. */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** Only an untriaged or triaged breakdown may still be given a slot. */
    public boolean isBookable() {
        return BOOKABLE.contains(this);
    }

    public static Set<BreakdownStatus> terminalStatuses() {
        return TERMINAL;
    }

    public static Set<BreakdownStatus> bookableStatuses() {
        return BOOKABLE;
    }
}
