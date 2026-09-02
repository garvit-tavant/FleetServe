==========================================================
MODULE 2 - CAPACITY & SCHEDULING SERVICE
==========================================================

----------------------------------------------------------
WORKSHOP APIS
----------------------------------------------------------

POST    /api/workshops
GET     /api/workshops
GET     /api/workshops/{workshopId}
POST    /api/workshops/{workshopId}/activate
POST    /api/workshops/{workshopId}/deactivate

----------------------------------------------------------
SERVICE BAY APIS
----------------------------------------------------------

POST    /api/workshops/{workshopId}/bays
GET     /api/workshops/{workshopId}/bays
GET     /api/bays/{bayId}
POST    /api/bays/{bayId}/activate
POST    /api/bays/{bayId}/deactivate

----------------------------------------------------------
CAPABILITY APIS
----------------------------------------------------------

POST    /api/capabilities
GET     /api/capabilities
GET     /api/capabilities/{capabilityCode}

----------------------------------------------------------
BAY CAPABILITY APIS
----------------------------------------------------------

POST    /api/bays/{bayId}/capabilities/{capabilityCode}
GET     /api/bays/{bayId}/capabilities
DELETE  /api/bays/{bayId}/capabilities/{capabilityCode}

----------------------------------------------------------
SKILL APIS
----------------------------------------------------------

POST    /api/skills
GET     /api/skills
GET     /api/skills/{skillCode}

----------------------------------------------------------
TECHNICIAN APIS
----------------------------------------------------------

POST    /api/technicians
GET     /api/technicians
GET     /api/technicians/{technicianId}
POST    /api/technicians/{technicianId}/activate
POST    /api/technicians/{technicianId}/deactivate
POST    /api/workshops/{workshopId}/technicians

----------------------------------------------------------
TECHNICIAN SKILL APIS
----------------------------------------------------------

POST    /api/technicians/{technicianId}/skills
GET     /api/technicians/{technicianId}/skills
POST    /api/technician-skills/{technicianSkillId}/expire

----------------------------------------------------------
WORKING CALENDAR APIS
----------------------------------------------------------

POST    /api/workshops/{workshopId}/calendar
GET     /api/workshops/{workshopId}/calendar
PUT     /api/calendars/{calendarId}
DELETE  /api/calendars/{calendarId}

----------------------------------------------------------
HOLIDAY APIS
----------------------------------------------------------

POST    /api/holidays
GET     /api/holidays
GET     /api/holidays/{holidayId}
DELETE  /api/holidays/{holidayId}

----------------------------------------------------------
SCHEDULING APIS
----------------------------------------------------------

POST    /api/scheduling/slots/search

----------------------------------------------------------
BOOKING APIS
----------------------------------------------------------

POST    /api/bookings
GET     /api/bookings
GET     /api/bookings/{bookingId}
POST    /api/bookings/{bookingId}/reschedule
POST    /api/bookings/{bookingId}/cancel

----------------------------------------------------------
BOOKING HISTORY APIS
----------------------------------------------------------

GET     /api/bookings/{bookingId}/history

==========================================================
TOTAL APIS = 41
==========================================================

Workshop               5
Service Bay            5
Capability             3
Bay Capability         3
Skill                  3
Technician             5
Technician Skill       3
Working Calendar       4
Holiday                4
Scheduling             1
Booking                5
Booking History        1

TOTAL                 42
==========================================================