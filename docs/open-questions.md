1. How should an incorrectly entered odometer reading
be corrected while preserving audit history?

2. Can a new odometer reading equal
the current latest reading?

3. If bookings already exist and a holiday is later added,
what should happen?
Options:- Reject Holiday, force rescheduling by no.
of holidays

4. Can one booking span multiple working days?
Current?

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

13. Is there a maximum future booking horizon?

14. The Feasible-Slot Engine signature includes a `searchHorizon` parameter, but the specification does not define its exact structure.

15. How to map bay with workshop?
Currently, we are going manywithone relationship between bay and workshop and not many to many

Name in skills table is not unique

16. Question: Can a technician have overlapping certifications for the same skill?

Decision: No.

Reason: Certification validity periods must not overlap. This avoids ambiguity during scheduling and allows a deterministic answer to the question: "Is this technician certified for this skill on date X