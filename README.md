Currently is a secure web application designed to help households better understand and manage their electricity consumption. The application allows users to digitally model their home, assign appliances to rooms, and estimate energy usage and associated costs based on realistic usage assumptions. The project focuses on usability, transparency, and secure software engineering practices, without relying on physical monitoring hardware such as smart meters or smart plugs.

This project is being developed as part of a final-year Computing degree, with a specific emphasis on cybersecurity, backend security, and secure data handling.

Core Features

Secure user registration and login using JWT-based authentication

Map My House feature for creating rooms and floors

My Appliances feature for assigning appliances and configuring usage

Appliance-level energy estimation using kWh calculations

Structured appliance reference dataset (JSON-based)

User-specific data persistence and access control

Planned features include a dashboard for aggregated insights, the “Watch Your Watts” cost forecasting module, and an optional AI-assisted recommendation feature.

Technology Stack
Frontend

React

Plain CSS

Backend

Java Spring Boot

Spring Security

JWT (JSON Web Tokens)

SQLite (development)

PostgreSQL (planned for deployment)

Other Tools

JSON appliance reference dataset

Notion for sprint planning and documentation
===================================
Development Notes on Commit History

During early development, the backend was developed in a separate repository structure while core authentication and security features were being implemented. This included approximately eight incremental commits focused on backend setup, Spring Security configuration, and JWT authentication.

At a later stage, the project structure was reorganised and the Git repository was moved into a new root source directory to unify frontend, backend, and documentation under a single repository. As a result of this restructuring, the earlier commit history was not retained in the current repository. Subsequent development continues within the consolidated project structure.

This change reflects a repository organisation decision rather than a loss of development work, and the implemented backend functionality remains intact.
