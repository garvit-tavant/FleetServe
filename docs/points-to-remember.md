maintenance plan has now no relationship with skill and capability table,
bcz they are not built while iam building asset management class,
after building skill and capability table, map them with these tables


later complete duemaintenanceprojection, repo, and impl

mapping of asset with depot is done, but no controller is built according to it, so improve it

for technician in findSlotEngine
validFrom <= date
&&
(validTo == null || validTo >= date)

---

# SLA work remaining, to be built inside ExecutionService

The SLA calculation core is finished and tested (weekend, holiday and pause
cases all pass, INV-7 evidence requirement met). What is missing is the wiring:
nothing in the system currently writes the timestamps and pause rows the clocks
read. Until the items below exist, no clock ever pauses and no response is ever
recorded in production.

All of this belongs in ExecutionService because the triggers are work-order
state transitions.

## 1. Create the checkpoint row at intake (blocker for everything else)

`sla_checkpoint` has no row until someone inserts one, and every SLA write is an
`UPDATE`:

- `SlaCheckpointRepository.recordFirstResponse(...)`
- `SlaCheckpointRepository.updateResponseBreach(...)`
- `SlaCheckpointRepository.updateResolutionBreach(...)`

If the row is absent these update **zero rows and fail silently**. A breakdown
with no checkpoint row will never register a response and never register a
breach, and nothing will complain.

So breakdown intake must insert one `sla_checkpoint` row per `breakdown_request`
in the same transaction as the breakdown itself.

## 2. Work-order start stops the response clock

Spec line 1191: "Starting a job records a start timestamp and stops the 'time to
respond' service-level clock."

On `SCHEDULED -> IN_PROGRESS`:

    slaCheckpointRepository.recordFirstResponse(breakdownId, startedAt);

`recordFirstResponse` is guarded with `and c.respondedAt is null`, so a job that
is started, paused and restarted keeps the first response time. Do not replace
it on later transitions.

### Getting from a work order to its breakdown

There is no direct column. The path is:

    work_order.booking_id -> booking.breakdown_request_id

`booking.breakdown_request_id` is **NULL for PREVENTIVE bookings**
(`ck_booking_reference_by_kind`). Only CORRECTIVE work orders have a breakdown,
and therefore only they touch SLA. Guard for null rather than assuming.

## 3. AWAITING_PARTS opens and closes a pause

On entering `AWAITING_PARTS`, insert an `awaiting_raised` row:

    breakdown_request_id = <breakdown>
    raised_at            = <transition instant>
    resolved_at          = NULL
    pause_reason         = 'AWAITING_PARTS'      // PauseInterval.AWAITING_PARTS

On leaving `AWAITING_PARTS` (back to `IN_PROGRESS`, or to `CANCELLED`), close the
open row by setting `resolved_at`.

Constraints that will bite:

- `uk_awaiting_raised_open_reason` is a unique partial index on
  `(breakdown_request_id, pause_reason) WHERE resolved_at IS NULL`. Opening a
  second pause of the same reason while one is open throws. That is deliberate:
  it stops the same interval being subtracted twice. Close before reopening.
- `ck_awaiting_raised_resolved_after_raised` requires `resolved_at > raised_at`
  strictly. A pause opened and closed within the same clock tick violates it.
  Worth deciding whether to skip zero-length pauses rather than write them.

A pause left open is not a bug for the calculator: it is clipped to the
evaluation instant, so the clock simply stays held. But it will hold forever, so
cancellation paths must close pauses too.

## 4. Depot-unreachable pause

Decided in `docs/open-questions.md` entry 12: the clock pauses when the customer
depot is unreachable. Same table, same mechanism, `pause_reason =
'DEPOT_UNREACHABLE'` (`PauseInterval.DEPOT_UNREACHABLE`).

This one is **not** a work-order transition, so it needs its own trigger, most
likely an explicit action on the breakdown. Both reasons can be open at once;
`WorkingCalendarArithmetic.clipAndMergePauses` merges the overlap so the shared
interval is only subtracted once. There is a test covering exactly this.

## 5. Completion stops the resolution clock

On work-order `COMPLETED`, write `sla_checkpoint.resolved_at` and move the
breakdown to `RESOLVED`.

Ordering constraint that will reject bad writes:

    ck_sla_checkpoint_responded_before_resolved
    CHECK (resolved_at IS NULL
           OR (responded_at IS NOT NULL AND resolved_at > responded_at))

So `resolved_at` **cannot** be written unless `responded_at` already exists, and
it must be strictly later. If item 2 is skipped, every completion of a breakdown
work order will fail at the database. These two are a pair; build them together.

Note also that `breakdown_request.status` must reach `RESOLVED`, not
`COMPLETED`. `COMPLETED` is not in the breakdown status vocabulary
(`ck_breakdown_status` allows REPORTED, TRIAGED, BOOKED, IN_PROGRESS, RESOLVED,
CANCELLED) and the compliance report filters on `RESOLVED`.

## 6. Mean time to repair, and a real conflict in the specification

MTTR reads work-order timestamps, so it lands here too. Two requirements pull
against each other:

- Line 1222: "Both figures are produced by SQL aggregation using common table
  expressions or window functions. Java-side looping is an automatic fail."
- Line 1223: MTTR is computed "against the working calendar, consistently with
  the service-level rules in US-4.1... If the two are implemented independently
  they will diverge."

The calendar component is Java, and SQL aggregation cannot call it. This looks
like one of the planted conflicts in section 3.3, so it should be raised in
`docs/open-questions.md` with options rather than quietly resolved.

Suggested resolution: have the sweep persist computed working-minutes (and
absolute due-at instants) onto `sla_checkpoint`, then let SQL aggregate those
stored columns. That keeps one calendar implementation, satisfies the SQL
aggregation rule, and makes the dashboard queries indexable.

## 7. Dashboard endpoints

Not blocking Execution, but do not exclude them from the OpenAPI freeze.

- `SlaService.refreshAndListOpenClocks()` currently returns an unbounded list
  sorted in Java. Line 352: "an unpaginated list endpoint is a defect", and it
  will not hold the 500 ms budget in line 1465 because it evaluates every open
  breakdown per request. Persist due-at instants (see item 6) and sort/paginate
  in SQL.
- Roles already exist for the personas: `DEPOT_SUPERVISOR` for US-4.1 and
  `OPERATIONS_MANAGER` for US-4.2 (see V12). Line 340 requires `@PreAuthorize` on
  every endpoint plus a test proving 403 for an under-privileged token.

## 8. Unrelated bug that will block booking work immediately

`SLA/entity/Booking.java` maps `breakdownRequest` as

    @OneToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "breakdown_request_id", nullable = false)

but the column is nullable and **must** be null for PREVENTIVE bookings. Any
attempt to create a preventive booking through JPA will fail. Fix before
building the booking flows.

## Quick reference

| Trigger | Effect |
| --- | --- |
| Breakdown raised | insert `sla_checkpoint` row |
| Work order `IN_PROGRESS` (first time) | set `responded_at` |
| Enter `AWAITING_PARTS` | open pause, reason `AWAITING_PARTS` |
| Leave `AWAITING_PARTS` | close that pause |
| Depot unreachable on/off | open/close pause, reason `DEPOT_UNREACHABLE` |
| Work order `COMPLETED` | set `resolved_at`, breakdown to `RESOLVED` |
| Breakdown `CANCELLED` | close any open pause |

Useful entry points: `SlaClockService.evaluate(breakdownId)` for a single
snapshot, `SlaClockService.refreshOpenBreakdowns()` for the sweep (idempotent,
only writes when a flag actually changes), and
`WorkingCalendarArithmetic` plus `WorkshopCalendarProvider.load(...)` for MTTR
so it shares the US-4.1 calendar rather than growing a second copy.
