1. How should an incorrectly entered odometer reading
be corrected while preserving audit history?

2. Can a new odometer reading equal
the current latest reading?

3. If bookings already exist and a holiday is later added,
what should happen?
Options:- Reject Holiday, force rescheduling by no.
of holidays

4. Can one booking span multiple working days?

Status: Decided (2026-09-07). Flagged in the specification as a Week 1
trap: "Decide early whether a job may span two working days. The
specification does not tell you, and the answer changes the slot engine
substantially."

Decision: No. A slot must fit entirely within one working day's opening
and closing times, using that day's own calendar entry.

Reasoning: It keeps the slot engine's candidate generation bounded to one
day's units, which is what makes the per-day availability array and its
prefix sums possible at all. It also matches how a workshop actually
works: a vehicle on a lift overnight blocks the bay without anyone
working on it.

Consequence: A job estimated longer than the workshop's working day can
never be scheduled, and is refused rather than silently truncated. This is
covered by a test. If long jobs are ever needed they must be split into
several bookings, which is a modelling change, not a tuning change.

5. Should capability and skill remain separate entities?
OR
Should they be unified?
Current design:- capability is vehicle and bay restriction
skill is technical qualification

7. If required parts cannot be reserved, should we:
Booking fail?
Booking become pending(extend the slot)?
Work order become waiting?

8. When a work order enters AWAITING_PARTS, should bay and technician
remain reserved?
Currently following:- release both

9. When parts arrive, does:AWAITING_PARTS -> IN_PROGRESS
should we find the new booking for it or shall we extend the 
slots for each booking?

10. Can one work order be worked by multiple technicians
simultaneously?

11. Should inventory be checked:
At booking creation?
At work start?
Both?

12. Does SLA pause if customer depot becomes unreachable?

Status: Decided (2026-09-07). Flagged in the specification (US-4.1) as
explicitly unstated: "The clock pauses while AWAITING_PARTS. Whether it
also pauses when the customer depot is unreachable is unstated; decide
and document."

Decision: Yes. The service-level clock pauses while the customer depot is
unreachable, using the same pause mechanism as AWAITING_PARTS. Pause
intervals are recorded in `awaiting_raised` with a `pause_reason` of
either AWAITING_PARTS or DEPOT_UNREACHABLE.

Options considered:
  a) Do not pause. Simpler, but the workshop is held accountable for
     delay it cannot influence, so the compliance metric in US-4.2 stops
     measuring workshop performance and starts measuring depot
     connectivity.
  b) Pause (chosen). The clock measures only time the workshop could
     actually have acted on.

Reasoning: A service level is a commitment by the workshop. Time lost
because the depot cannot be contacted is outside the workshop's control,
so counting it would make the metric unactionable and would push crews to
game triage ordering. This keeps INV-7 coherent: elapsed service-level
time never counts hours outside the working calendar, and never counts a
paused interval, regardless of which reason opened that pause.

Consequence: A pause reason is required on every pause row so the two
causes can be reported separately, and so a depot-connectivity problem
remains visible rather than being silently absorbed into the SLA figure.
Both reasons are subtracted identically by the calendar arithmetic, so no
second code path exists.

13. Is there a maximum future booking horizon?

Status: Decided (2026-09-07).

Decision: 14 days. A slot search starts no earlier than today in the
workshop's own time zone and looks forward 14 days. If nothing is feasible
in that window the caller is told the slot was not booked.

Reasoning: The specification requires the engine to respect a horizon and
"return what exists rather than searching indefinitely", but does not fix
a length. Two weeks is long enough to absorb a fully booked week and short
enough that the engine's per-day arrays stay small.

Consequence: A search starting in the past is refused outright, since a
slot already gone cannot be worked. Both are covered by tests.

14. The Feasible-Slot Engine signature includes a `searchHorizon` parameter, but the specification does not define its exact structure.

Status: Decided (2026-09-07).

Decision: `searchHorizon` is `{ startDate, numberOfDays }`, both supplied
by the caller.

Reasoning: Passing the start date in rather than reading the system clock
inside the engine is what keeps the engine a pure function, so a search is
reproducible and testable without waiting for real time to pass. It also
satisfies the injected-clock rule at the one point where it would
otherwise be easiest to break.

15. How to map bay with workshop?
Currently, we are going manywithone relationship between bay and workshop and not many to many

Name in skills table is not unique

17. Where does the next-due baseline come from for an asset
that has never been serviced?

Status: Confirmed (2026-09-07). The specification flags this itself in
US-1.2: "Where an asset has never been serviced, the baseline is the
acquisition date and acquisition odometer. Teams must notice that this is
stated nowhere else and confirm it."

Decision: For an asset with no COMPLETED work order under a given plan,
next-due is computed from `asset.acquisition_date` and
`asset.acquisition_odometer_km`. Once a service completes under that plan,
the baseline moves to that work order's `completed_at` and
`odometer_at_service`.

Reasoning: The baseline is per (asset, plan), not per asset. An asset can
sit on several plans with different intervals, and servicing it under one
plan must not reset the clocks of the others.

Consequence: The due list joins work_order through booking, because
`booking.maintenance_plan_id` is the only place the plan is recorded.
Until ExecutionService starts completing work orders, every asset is
measured from acquisition, which is correct but means the service-history
branch is only exercised by tests.

18. Should the DUE_SOON thresholds be per plan, per asset class or per pairing?

Status: Decided (2026-09-07). US-1.3 requires the "soon" thresholds to be
"configurable as data, not constants in code" but does not say where they
live. Note that the section 5.6.1 ERD models `asset_class_plan` as a bare
join table ("Asset class, plan; composite unique"), so this is a
deliberate departure from the given design.

Decision: Stored on `asset_class_plan`, the class-and-plan pairing. A
request may override them for what-if planning, but the stored value is
the default.

Options considered:
  a) On `maintenance_plan`. Simplest, and a CHECK constraint can compare
     the window against the interval because both sit on one row. But it
     forces every class using that plan to share one window.
  b) On `asset_class_plan` (chosen). One plan can apply to several
     classes: the same 10 000 km service may justify a 500 km warning on
     a van and 2 000 km on a heavy vehicle that is harder to get into a
     bay. The interval is a property of the policy; the warning window is
     a property of applying that policy to a class.

Reasoning: The window is an attribute of the relationship, not of either
entity alone. Putting it on the plan would make the traffic light wrong
for whichever class was not considered when the value was chosen.

Consequence: A CHECK constraint cannot reference another table, so the
rule "the window must be shorter than the interval it qualifies" is
enforced by triggers instead (V15), following the append-only trigger
pattern already established in V9. Two triggers are needed, because the
rule can be broken from either side: by widening a window, or by
shrinking the interval underneath an existing window. The service layer
repeats the check so callers get a business error rather than a raw
constraint violation.



Decision: No.

Reason: Certification validity periods must not overlap. This avoids ambiguity during scheduling and allows a deterministic answer to the question: "Is this technician certified for this skill on date X


Open Question #1

BreakdownRequest currently stores no estimated duration,
required skill, or required capability.

Scheduler therefore cannot automatically allocate a bay or technician.

Need decision:
- Store directly in BreakdownRequest
or
- Introduce BreakdownAssessment step.


Open Question #2

Breakdown scheduling currently allows any workshop to
handle any breakdown.

Need business rule defining Workshop ↔ Depot mapping
before validation can be implemented.

19. Where do a breakdown's work requirements come from?

Status: Decided (2026-09-07). The section 5.6.1 ERD lists breakdown_request
without a duration, required skill or required capability, so a breakdown
could not be scheduled from stored data alone.

Decision: `breakdown_request` now carries `required_skill_code`,
`required_capability_code` and `estimated_duration_minutes` (V16). Triage
records them; the booking endpoint reads them rather than accepting them
from the caller.

The duration is derived from `skill.time`, the standard time for the
required skill, and then written back onto the breakdown.

Reasoning: Storing rather than deriving on every read pins the estimate,
for the same reason `sla_policy_id` is pinned at intake. Editing a skill's
standard time next month must not retrospectively change the duration of
work already triaged and booked. Taking the values from the record rather
than the request also stops two people booking the same breakdown with
different durations.

Consequence: A breakdown with neither a stored estimate nor a required
skill is refused with a message asking for triage first, rather than being
booked with a guessed duration.

20. Which workshop may service a breakdown?

Status: Decided (2026-09-07). The specification does not say whether a
breakdown reported at one depot may be serviced anywhere.

Decision: Any workshop belonging to the depot that reported the breakdown,
and no other. `workshop.depot_id` must equal `breakdown_request.depot_id`.

Reasoning: A depot has its own workshops; sending an immobilised vehicle
to another depot's workshop is a transport problem the system does not
model. Restricting to the depot keeps scheduling honest without hard-coding
a single workshop, so a depot with several workshops can still balance load
across them.

Consequence: The booking endpoint validates the pairing and returns 422
naming both depots when they differ. If cross-depot recovery is ever
needed, it becomes an explicit transfer rather than a silent booking.

