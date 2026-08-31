Asset
-----
POST   /api/assets
GET    /api/assets
GET    /api/assets/{id}
PUT    /api/assets/{id}, not implemented

Odometer
---------
POST   /api/assets/{id}/odometer-readings
GET    /api/assets/{id}/odometer-readings
GET    /api/assets/{id}/odometer-readings/latest

Maintenance Plan
----------------
POST   /api/maintenance-plans
GET    /api/maintenance-plans
GET    /api/maintenance-plans/{id}
PUT    /api/maintenance-plans/{id}, not implemented
PATCH  /api/maintenance-plans/{id}/deactivate, not implemented

Asset Class
-----------
POST   /api/asset-classes
GET    /api/asset-classes
GET    /api/asset-classes/{id}

Asset Class Plan Mapping
------------------------
POST   /api/asset-classes/{classId}/plans/{planId}
DELETE /api/asset-classes/{classId}/plans/{planId}
GET    /api/asset-classes/{classId}/plans

Due Calculation
---------------
GET    /api/maintenance-due
GET    /api/assets/{id}/due-status

Asset Lifecycle
---------------
POST   /api/assets/{id}/retire
POST   /api/assets/{id}/reinstate
