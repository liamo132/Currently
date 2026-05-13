# Currently - User Stories

## Epic 1: User Authentication & Account Management

### US-1.1: User Registration
**As a** new user  
**I want to** create an account with my email and password  
**So that** I can start tracking my home energy consumption

**Acceptance Criteria:**
- User can enter email, password, and confirm password
- Password must be at least 8 characters
- Email must be unique in the system
- Successful registration redirects to login
- Invalid inputs show clear error messages

---

### US-1.2: User Login
**As a** registered user  
**I want to** log in with my email and password  
**So that** I can access my energy tracking dashboard

**Acceptance Criteria:**
- User can enter email and password
- Valid credentials return JWT token
- Token is stored securely (localStorage/sessionStorage)
- Failed login shows error message
- Login redirects to dashboard on success

---

### US-1.3: Update Energy Settings
**As a** user  
**I want to** set my monthly electricity budget and peak hour preferences  
**So that** I can receive alerts when approaching budget limits and optimize usage

**Acceptance Criteria:**
- User can set monthly budget in dollars
- User can select peak hours (e.g., 7-11 PM)
- User can enable/disable alerts
- Settings are saved and persisted
- User can edit settings anytime

---

## Epic 2: Home Energy Mapping

### US-2.1: Create Rooms
**As a** homeowner  
**I want to** create rooms in my home with their dimensions  
**So that** I can organize appliances by location

**Acceptance Criteria:**
- User can add room name and area (sq ft)
- User can create multiple rooms
- Room list is displayed with all rooms
- User can edit/delete rooms
- Rooms are persisted in database

---

### US-2.2: Map My House Visual Layout
**As a** user  
**I want to** see an interactive floor plan of my home  
**So that** I can visually understand my home's layout and appliance placement

**Acceptance Criteria:**
- Canvas displays all created rooms
- Rooms can be positioned on canvas
- Visual representation shows room names
- User can drag rooms to rearrange layout
- Layout is saved and loaded on revisit

---

## Epic 3: Appliance Management

### US-3.1: Add Appliances
**As a** homeowner  
**I want to** add appliances to my home with their power consumption details  
**So that** I can track energy usage by device

**Acceptance Criteria:**
- User can enter appliance name, model, and wattage
- User can assign appliance to a room
- User can categorize appliance (e.g., HVAC, Kitchen, Laundry)
- Appliance list shows all added devices
- User can edit/delete appliances

---

### US-3.2: View Appliance Details
**As a** a user  
**I want to** see detailed information about each appliance  
**So that** I can understand its power consumption and efficiency

**Acceptance Criteria:**
- Appliance card displays name, model, wattage, room
- Click appliance to view detailed stats
- Monthly estimated cost is calculated and shown
- Energy category and status are visible
- User can see appliance history (optional)

---

## Epic 4: Energy Consumption Tracking

### US-4.1: View Dashboard
**As a** user  
**I want to** see a real-time dashboard with current energy consumption metrics  
**So that** I can quickly understand my home's energy usage

**Acceptance Criteria:**
- Dashboard shows total consumption (kWh)
- Current cost display (today, week, month)
- Weekly cost trend visualization
- Quick action buttons for common tasks
- Alerts section for budget warnings

---

### US-4.2: Biggest Energy Eaters
**As a** user  
**I want to** see which appliances consume the most energy  
**So that** I can focus on reducing usage from high-consumption devices

**Acceptance Criteria:**
- List shows appliances ranked by consumption
- Each entry displays wattage and estimated monthly cost
- Can sort by consumption or cost
- Visual chart showing consumption breakdown
- Recommendations for high-consumption appliances

---

### US-4.3: Room-Level Consumption Breakdown
**As a** user  
**I want to** see energy consumption broken down by room  
**So that** I can identify which areas of my home use the most energy

**Acceptance Criteria:**
- Dashboard shows consumption per room
- Total kWh and estimated cost per room
- Visual cards with room summary statistics
- Comparison view between rooms
- Room consumption trends over time

---

### US-4.4: Cost Forecast
**As a** user  
**I want to** see a forecast of my upcoming electricity bill  
**So that** I can plan my budget and identify cost trends

**Acceptance Criteria:**
- Forecast shows projected monthly bill based on current usage
- 3-month and 12-month projections available
- Chart shows historical vs. projected costs
- Alerts for unusually high projected costs
- Recommendations to reduce projected bills

---

## Epic 5: Insights & Recommendations

### US-5.1: Smart Energy Insights
**As a** a user  
**I want to** receive AI-powered recommendations for reducing energy consumption  
**So that** I can optimize my energy usage and lower bills

**Acceptance Criteria:**
- System analyzes usage patterns
- Generates actionable recommendations (e.g., "Turn off bedroom lights during day")
- Recommendations are prioritized by potential savings
- User can mark recommendations as "done"
- Insight page shows top 5-10 recommendations

---

### US-5.2: Peak Hour Notifications
**As a** user  
**I want to** be notified of peak energy hours  
**So that** I can shift usage to off-peak times and save money

**Acceptance Criteria:**
- Alert sent during configured peak hours
- Notification includes current consumption vs. average
- Alert suggests high-consumption appliances to reduce
- User can customize peak hour settings
- Notifications can be enabled/disabled

---

## Epic 6: Bill Management

### US-6.1: Store & Organize Utility Bills
**As a** a user  
**I want to** upload and store my utility bills  
**So that** I can track historical billing data and spot trends

**Acceptance Criteria:**
- User can upload bill PDF/image file
- Bill date and total amount are extracted/entered
- Bills are organized chronologically
- User can view bill details and download files
- Search/filter bills by date range

---

### US-6.2: Bill Cost Trend Analysis
**As a** user  
**I want to** see trends in my electricity costs over time  
**So that** I can assess the impact of energy-saving efforts

**Acceptance Criteria:**
- Chart shows monthly bill amounts over time
- Trend line indicates cost direction
- Comparison with previous year's costs
- Identify months with unusual spikes
- Export bill history as PDF/CSV

---

## Epic 7: User Experience & Settings

### US-7.1: Responsive Dashboard
**As a** user  
**I want to** access my energy dashboard on any device  
**So that** I can check my consumption anytime, anywhere

**Acceptance Criteria:**
- Dashboard is responsive (mobile, tablet, desktop)
- All features accessible on mobile
- Performance optimized for slow connections
- Touch-friendly interface
- Quick-load times (<2 seconds)

---

### US-7.2: User Profile & Preferences
**As a** user  
**I want to** manage my profile and preferences  
**So that** the app works best for my needs

**Acceptance Criteria:**
- User can update email and password
- Theme selection (light/dark mode)
- Notification preferences (email/push)
- Language selection (if multi-language)
- Data export/download option

---

## Epic 8: System Health & Performance

### US-8.1: System Status Monitoring
**As a** user  
**I want to** see the status of my system and any alerts  
**So that** I can ensure all devices are functioning correctly

**Acceptance Criteria:**
- System status page shows all components
- Green/yellow/red indicators for health
- Alert history and resolution status
- Sensor connectivity status
- Last sync time for data updates

---

### US-8.2: Data Accuracy & Calibration
**As a** an advanced user  
**I want to** verify and calibrate energy consumption data  
**So that** I can ensure accurate billing and recommendations

**Acceptance Criteria:**
- Comparison with actual utility bill data
- Manual adjustment capability for known errors
- Calibration wizard for sensor setup
- Data accuracy report
- Historical calibration logs

---

## Priority & Phasing

### Phase 1 (MVP - Weeks 1-4)
- US-1.1, US-1.2: Authentication ✅
- US-2.1: Create Rooms ✅
- US-3.1: Add Appliances ✅
- US-4.1: View Dashboard ✅

### Phase 2 (Core Features - Weeks 5-8)
- US-2.2: Map My House Layout
- US-4.2: Biggest Energy Eaters
- US-4.3: Room Consumption Breakdown
- US-4.4: Cost Forecast

### Phase 3 (Intelligence - Weeks 9-12)
- US-5.1: Smart Energy Insights
- US-5.2: Peak Hour Notifications
- US-6.1: Store Utility Bills
- US-6.2: Bill Trend Analysis

### Phase 4 (Polish & Advanced - Weeks 13+)
- US-1.3: Energy Settings
- US-7.1: Responsive Design
- US-7.2: User Preferences
- US-8: System Monitoring

---

## Notes

- All user stories include JWT authentication requirement
- Backend API must enforce authorization on all endpoints
- Frontend uses React Router for navigation
- Database constraints ensure data integrity
- All timestamps in UTC
- Sensitive data (passwords, bills) encrypted at rest
