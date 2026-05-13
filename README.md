# Currently - Smart Energy Tracking & Management

A full-stack web application for residential energy consumption tracking, analysis, and management. Currently helps users understand and optimize their electricity usage through detailed appliance tracking, room-level insights, and actionable recommendations.

---

## Features

### Dashboard & Overview
- Real-time energy consumption dashboard
- Quick access to key metrics and alerts
- Weekly cost visualization with hero cards

### Map My House
- Interactive floor plan canvas
- Room-based organization of appliances
- Visual layout of your home energy ecosystem
- Room creation and management

### My Appliances
- Comprehensive appliance inventory
- Energy consumption per device
- Appliance categorization and tagging
- Real-time status monitoring

### Watch Your Watts
- **Biggest Eaters**: Identify high-consumption appliances
- **Room Consumption**: Break down energy usage by room
- **Cost Forecast**: Predict monthly/quarterly bills
- **Room Summary Cards**: Quick statistics for each room

### Smart Insights
- AI-powered energy optimization recommendations
- Usage pattern analysis
- Peak consumption alerts
- Energy-saving suggestions

### Bills Vault
- Store and organize utility bills
- Historical billing data
- Cost trend analysis

### User Management
- Secure authentication with JWT tokens
- Customizable energy settings
- Multi-user household support
- User-specific appliance assignments

---

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.3.0
- **Language**: Java 17
- **Database**: PostgreSQL 17
- **Authentication**: Spring Security with JWT
- **API**: RESTful JSON
- **Build**: Maven

### Frontend
- **Framework**: React 19
- **Build Tool**: Vite 7
- **Router**: React Router DOM 7
- **Charting**: Recharts 3.7
- **Icons**: Lucide React
- **Styling**: CSS3

### Deployment
- **Backend**: Docker + Docker Compose
- **Frontend**: Vercel-ready configuration
- **Database**: PostgreSQL in Docker container

---

## Project Structure

```
currently/
├── currently-backend/          # Spring Boot REST API
│   ├── src/main/java/
│   │   └── com/currently/currently_backend/
│   │       ├── controller/    # REST endpoints
│   │       ├── service/       # Business logic
│   │       ├── model/         # JPA entities
│   │       ├── repository/    # Data access
│   │       ├── dto/           # Data transfer objects
│   │       ├── config/        # Spring configuration
│   │       ├── security/      # Security filters & handlers
│   │       └── util/          # JWT and utilities
│   ├── docker/
│   │   └── postgres/          # Database initialization
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── pom.xml
│
└── currently-frontend/         # React SPA
    ├── src/
    │   ├── public/            # Static assets
    │   ├── private/           # Authenticated routes
    │   │   ├── dashboard/
    │   │   ├── mapmyhouse/
    │   │   ├── myappliances/
    │   │   ├── smartinsights/
    │   │   ├── watchyourwatts/
    │   │   └── shared/
    │   ├── api/               # API client functions
    │   ├── styles/            # Global styles
    │   ├── App.jsx
    │   └── main.jsx
    ├── vite.config.js
    ├── eslint.config.js
    ├── package.json
    └── vercel.json
```


## Authentication

Currently uses JWT (JSON Web Tokens) for stateless authentication:

- **JWT Secret**: Set via `JWT_SECRET` environment variable (Base64-encoded)
- **Token Expiration**: Configurable via `app.jwt.expiration-ms` (default: 1 hour)
- **Secure Flow**: 
  1. User logs in with email/password
  2. Server validates credentials
  3. JWT token issued with user email as subject
  4. Frontend stores token in session/storage
  5. Token included in `Authorization: Bearer <token>` header for API calls

---

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login

### Appliances
- `GET /api/appliances` - List all appliances
- `POST /api/appliances` - Create new appliance
- `GET /api/appliances/{id}` - Get appliance details
- `PUT /api/appliances/{id}` - Update appliance
- `DELETE /api/appliances/{id}` - Delete appliance

### Rooms
- `GET /api/rooms` - List all rooms
- `POST /api/rooms` - Create new room
- `GET /api/rooms/{id}` - Get room details
- `PUT /api/rooms/{id}` - Update room
- `DELETE /api/rooms/{id}` - Delete room

### Energy & Insights
- `GET /api/energy/consumption` - Get consumption data
- `GET /api/insights` - Get AI recommendations
- `GET /api/bills` - List stored bills
- `POST /api/bills` - Upload bill file
- `GET /api/user/settings` - Get user energy settings
- `PUT /api/user/settings` - Update energy settings


## Database Schema

**Users** - User accounts and authentication
- id, email, password_hash, created_at, updated_at

**Rooms** - Physical spaces in home
- id, user_id, name, area, created_at

**Appliances** - Energy-consuming devices
- id, name, model, watts, category, room_id

**UserAppliances** - User-specific appliance assignments
- id, user_id, appliance_id, monthly_cost, status

**Bills** - Stored utility bills
- id, user_id, file_path, bill_date, total_amount

**UserEnergySettings** - User preferences
- id, user_id, budget, peak_hour, alerts_enabled

---

## Deployment

You can find the link above at https://currently-omega.vercel.app/

---

## 👨‍💻 Author

**Liam Connell**  
Created: November 2025

