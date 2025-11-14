AI Developer Instruction Prompt for Currently Project

When generating or revising code:

"Write fully commented code suitable for a college project, using clear educational comments explaining logic, purpose, and structure. Maintain modularity and readability. Always assume this will be reviewed by an academic examiner."

Code Commenting Guidelines

Every code file (React, Java, SQL, etc.) must:

1. Begin with a short header comment:

/*
 * File: Dashboard.jsx
 * Description: Displays user’s energy consumption dashboard with charts and cost overview.
 * Author: Liam Connell
 * Date: YYYY-MM-DD
 */

2. Include function-level comments describing purpose, inputs, and outputs:

// Function: fetchUserData
// Purpose: Retrieves user electricity data from backend API and updates the state.
// Inputs: userId (String)
// Outputs: Updates dashboard metrics and charts

3. Add inline comments for any logic that isn’t self-explanatory, e.g.:

// Calculate total cost by summing appliance-level consumption
const totalCost = appliances.reduce((sum, item) => sum + item.cost, 0);


Every Git commit must:

Be frequent (after each major change or fix).

Use structured commit messages:

feat: add energy forecast module
fix: correct login redirect issue
refactor: clean up MapMyHouse component
docs: update README with DB schema info
test: add WatchYourWatts unit tests


Include at least one commit per development session, ensuring version traceability.

Development Tools

Frontend: React (Vite) + CSS

Backend: Spring Boot

Database: SQLite (local dev), PostgreSQL (deployment)

Version Control: Git + GitHub

Design: Figma (UI/UX prototypes)

Documentation: Notion (sprints, notes, progress logs)

Diagramming: PlantUML (architecture & flow diagrams)
