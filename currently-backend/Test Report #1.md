# Test Report #1

## Module
Currently Backend

## Purpose Of This Report
This report is the first testing report for the backend part of the project. The aim of this report is to show what tests were created, what areas of the system they cover, and how many tests are currently in the project.

This report is mainly focused on the core testing work that has been done so far. If any failed tests, defects, or fixes need to be discussed in more detail, that can be written in **Test Report #2**.

## What Was Tested
The backend testing was split into a few main areas:

- Unit tests for core service logic using JUnit 5 and Mockito
- Integration tests for backend API endpoints
- End-to-end style backend journeys using MockMvc
- Forecasting and cost calculation checks
- Database CRUD and relationship testing

## Test Files Included
- `src/test/java/com/currently/currently_backend/CurrentlyBackendApplicationTests.java`
- `src/test/java/com/currently/currently_backend/RoomServiceTest.java`
- `src/test/java/com/currently/currently_backend/UserApplianceServiceTest.java`
- `src/test/java/com/currently/currently_backend/UserEnergySettingsServiceTest.java`
- `src/test/java/com/currently/currently_backend/InsightServiceTest.java`
- `src/test/java/com/currently/currently_backend/PersistenceCrudTests.java`
- `src/test/java/com/currently/currently_backend/IntegrationApiTests.java`

## Test Breakdown By Module

### 1. Application Startup
- `CurrentlyBackendApplicationTests`: 1 test
- This checks that the Spring application context loads correctly.

### 2. Room Management
- `RoomServiceTest`: 4 tests
- This covers:
  - room sorting
  - missing room name validation
  - blank name validation
  - ownership checks when updating rooms

### 3. User Appliance Management
- `UserApplianceServiceTest`: 7 tests
- This covers:
  - getting appliance data for the current user
  - cost and kWh calculation for continuous appliances
  - cost and kWh calculation for per-use appliances
  - validation of incorrect usage input
  - room ownership checks
  - update ownership checks
  - delete ownership checks

### 4. Energy Settings
- `UserEnergySettingsServiceTest`: 4 tests
- This covers:
  - getting saved settings
  - saving updated settings
  - applying a default tariff when needed
  - rejecting a null request

### 5. Insight And Forecasting Logic
- `InsightServiceTest`: 5 tests
- This covers:
  - generating insights from appliance data
  - checking expected forecast/cost values
  - pagination of insight results
  - preventing another user from accessing a run
  - handling the case where no appliance data exists

### 6. Database And Persistence
- `PersistenceCrudTests`: 4 tests
- This covers:
  - room create, read, and delete behaviour
  - unique email constraint checking
  - preventing an appliance from being saved without a user
  - preventing deletion of a room that is still referenced

### 7. Integration Testing
- `IntegrationApiTests`: 14 tests
- This covers:
  - register and login flow
  - unauthorised access to protected endpoints
  - validation error responses
  - invalid login handling
  - duplicate registration handling
  - room CRUD through the API
  - cross-user access protection
  - public appliance catalogue endpoint
  - appliance creation and insight generation
  - end-to-end backend user journey
  - energy settings API
  - insight no-data response

## Total Number Of Tests

| Test Class | Number Of Tests |
| --- | ---: |
| `CurrentlyBackendApplicationTests` | 1 |
| `RoomServiceTest` | 4 |
| `UserApplianceServiceTest` | 7 |
| `UserEnergySettingsServiceTest` | 4 |
| `InsightServiceTest` | 5 |
| `PersistenceCrudTests` | 4 |
| `IntegrationApiTests` | 14 |
| **Total** | **39** |

## Summary Of Coverage
Overall, the test suite covers the main backend features that were expected for this stage of the project. The biggest focus was on service logic, API behaviour, validation, user ownership/security rules, forecasting calculations, and database relationships.
I think the test set is a good size for the project because it covers the main features without becoming too excessive. The integration tests are especially useful because they check how multiple backend parts work together instead of only testing isolated methods.

## Notes
- The current repository contains **39 test methods** in total.
- The test names and comments were written so it is clear what each test is checking.
- If failed tests or bugs need to be documented separately, that can be done in **Test Report #2**.

## Limitations
- In this coding environment, the full Maven test run could not be completed because of local toolchain issues.
- Because of that, this report is based on the source test suite and its coverage, rather than a full successful run in this environment.
- A full local or lab-machine test run would still be useful as a final check.

## Conclusion
To conclude, the backend currently has a solid first round of testing in place. The tests cover the core services, integration flows, forecasting logic, and persistence layer. This report shows the main testing work completed in this first pass, while any failed cases, debugging steps, and retesting can be documented more clearly in **Test Report #2**.
