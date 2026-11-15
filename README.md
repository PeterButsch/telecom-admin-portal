# Telecom Demo Admin Portal

An operations console for a Singapore mobile operator (Singtel, GOMO and hi!Card brands), built
with Next.js and Spring Boot.

> **Demo use only:** This project uses generated sample data and publishes the demo staff
> password on its sign-in page. Do not use real customer data or expose the application to the
> public internet.

## Features

- **Dashboard** - KPI cards and charts: active customers and lines, monthly recurring revenue,
  open cases, devices in use
- **Customers** - Consumer (NRIC, FIN or passport) and business (UEN) customers with search by
  name, customer number, email, UEN or mobile number. ID numbers are only ever shown masked
  (for example `*****567D`) and cannot be searched.
- **Mobile Lines** - Every line with its brand, plan, SIM type, status, contract and porting details
- **Plans** - The Singtel, GOMO and hi!Card catalogue: postpaid, SIM-only, prepaid and data-only
  plans for consumer and enterprise segments
- **Devices** - Handset inventory with IMEI and the line each device is assigned to
- **Usage** - Call, data and SMS records per line, including roaming
- **Support Cases** - Customer cases with category, channel, priority and SLA tracking
- **Sign-in and roles** - Staff sign in with their own account; each role only sees the pages
  and actions its job needs, both in the UI and in the API
- **Staff Accounts** - Administrators add staff, change roles, deactivate accounts, reset
  passwords and unlock accounts after repeated failed sign-ins
- **Audit Log** - Sign-ins, failed sign-ins, denied requests, customer record views and every
  change, with who did it and when. Entries cannot be changed or deleted, and NRIC/FIN numbers
  never appear in them.

## Tech Stack

**Frontend:**

- Next.js 16 with App Router
- React 19
- Tailwind CSS 4
- shadcn/ui components
- Recharts (via shadcn/ui)
- TypeScript

**Backend:**

- Spring Boot 4.0
- Java 21
- Gradle
- SQLite in WAL mode, schema managed by Flyway
- Generated Singapore demo data on first start

## Getting Started

### Prerequisites

- Node.js 22+
- Java 21+
- pnpm

### Setup

```bash
pnpm setup    # Install dependencies
```

### Demo Data

The database schema is managed by Flyway (`backend/src/main/resources/db/migration`). When the
backend starts with an empty database it fills it with generated Singapore demo data. A fixed
random seed gives every fresh database the same customers and lines; dates are relative to the
day the data was generated.

| Entity        | Count    |
| ------------- | -------- |
| Plans         | 15       |
| Customers     | 605      |
| Accounts      | 689      |
| Mobile lines  | 1,774    |
| Devices       | 1,085    |
| Usage records | ~166,000 |
| Support cases | ~520     |

```bash
pnpm reset:db   # Delete the database; the backend re-creates and re-seeds it on its next start
```

Set `APP_DEMO_DATA=false` to start with an empty database instead.

### Signing In

With demo data enabled the backend creates one staff account per role. They all use the
password `Demo-Pass-2026` (set `APP_DEMO_PASSWORD` to change it), and the sign-in page lists
them so you can sign in with one click.

| Username   | Name         | Role                       | Can                                                          |
| ---------- | ------------ | -------------------------- | ------------------------------------------------------------ |
| `admin`    | Grace Lim    | Administrator              | Everything, including staff accounts                         |
| `agent`    | Priya Nair   | Care Agent                 | View and edit customers, cases and devices; no deleting      |
| `teamlead` | Marcus Tan   | Care Team Lead             | As Care Agent, plus delete customers and read the audit log  |
| `fraud`    | Siti Rahman  | Fraud & Compliance Analyst | Read customers and lines, work cases, read the audit log     |
| `netops`   | Daniel Ong   | Network Operations         | Read lines, usage, devices and cases; no customer records    |
| `product`  | Rachel Wong  | Product Manager            | Manage the plan catalogue and see the dashboard; no customers |

Sessions end after 30 minutes without activity. Five wrong passwords in a row lock an account
for 15 minutes; an administrator can unlock it sooner on the Staff Accounts page. Passwords
need at least 12 characters with letters and digits and must not contain the username.

With `APP_DEMO_DATA=false` no demo accounts are created. Set `APP_ADMIN_PASSWORD` on the first
start to create an `admin` account with that password, then add the other staff from the portal.

### Development

Run both frontend and backend:

```bash
pnpm dev
```

- Frontend: http://localhost:3000
- Backend: http://localhost:8080

Or run separately:

```bash
pnpm dev:frontend   # Frontend only
pnpm dev:backend    # Backend only
```

### Testing

```bash
pnpm test           # All tests
pnpm test:frontend  # Vitest
pnpm test:backend   # Gradle test
pnpm test:e2e       # Playwright; signs in with the demo accounts
```

### Linting

```bash
pnpm lint
```

## Application Pages

| Page          | URL          | Description                       |
| ------------- | ------------ | --------------------------------- |
| Dashboard     | `/dashboard` | KPIs and charts                   |
| Customers     | `/customers` | Consumer and business customers   |
| Mobile Lines  | `/lines`     | Lines, plans and contract details |
| Plans         | `/plans`     | Plan catalogue for all brands     |
| Devices       | `/devices`   | Handset inventory                 |
| Usage         | `/usage`     | Call, data and SMS usage          |
| Support Cases | `/cases`     | Customer cases with SLA tracking  |
| Staff         | `/staff`     | Staff accounts (administrators)   |
| Audit Log     | `/audit`     | Audit trail (team leads, fraud)   |
| Sign in       | `/login`     | Staff sign-in                     |

## API Endpoints

| Endpoint           | Methods                  | Description                                      |
| ------------------ | ------------------------ | ------------------------------------------------ |
| `/api/health`      | GET                      | Health check (returns 503 if the database fails) |
| `/api/customers`   | GET, POST, PATCH, DELETE | Customers (paged, searchable)                    |
| `/api/accounts`    | GET, POST                | Billing accounts of a customer                   |
| `/api/lines`       | GET                      | Mobile lines (paged, searchable)                 |
| `/api/plans`       | GET, POST, PUT, DELETE   | Plan catalogue                                   |
| `/api/add-ons`     | GET                      | Add-on catalogue                                 |
| `/api/promotions`  | GET                      | Promotions                                       |
| `/api/devices`     | GET, POST, PATCH, DELETE | Device inventory                                 |
| `/api/usage`       | GET                      | Usage records (paged)                            |
| `/api/cases`       | GET, POST, PATCH         | Support cases (paged)                            |
| `/api/dashboard/*` | GET                      | Dashboard statistics                             |
| `/api/auth/*`      | GET, POST                | Sign in and out, current user, own password      |
| `/api/staff-users` | GET, POST, PATCH         | Staff accounts, unlock and password reset        |
| `/api/audit-events`| GET                      | Audit trail (paged, filterable)                  |

Every endpoint except `/api/health` and sign-in needs a signed-in session (the `TELCO_SESSION`
cookie) and a role that allows it; otherwise it returns 401 or 403. Requests that change data
must echo the `XSRF-TOKEN` cookie in an `X-XSRF-TOKEN` header (`GET /api/auth/csrf` sets it).

Paged endpoints accept `page` and `size` (at most 100) and return
`{ content, page, totalPages, totalElements, hasNext }`.

`PATCH` updates only the fields you send. Errors use the RFC 9457 problem format
(`application/problem+json`) with a readable `detail`, and validation failures list
the invalid fields under `errors`.

### Configuration

| Setting                 | Default   | Purpose                                         |
| ----------------------- | --------- | ----------------------------------------------- |
| `APP_DB_PATH`           | `app.db`  | SQLite database file used by the backend        |
| `APP_DEMO_DATA`         | `true`    | Seed an empty database with generated demo data |
| `APP_DEMO_PASSWORD`     | `Demo-Pass-2026` | Password of the demo staff accounts      |
| `APP_ADMIN_PASSWORD`    | (none)    | Creates an `admin` account when there is no staff and no demo data |
| `APP_SESSION_TIMEOUT`   | `30m`     | Sign staff out after this much inactivity       |
| `APP_SECURE_COOKIES`    | `false`   | Mark cookies Secure; set to `true` behind HTTPS |
| `app.max-request-bytes` | `1048576` | Request bodies above this size get a 413        |

## API Documentation

Start the backend, sign in to the portal, then open the Swagger UI at
`http://localhost:8080/swagger-ui.html` in the same browser.

## Project Structure

```
├── app/                    # Next.js pages
│   ├── dashboard/          # Dashboard with KPIs/charts
│   ├── customers/          # Customers
│   ├── lines/              # Mobile lines
│   ├── plans/              # Plan catalogue
│   ├── devices/            # Device inventory
│   ├── usage/              # Usage records
│   ├── cases/              # Support cases
│   ├── staff/              # Staff accounts
│   ├── audit/              # Audit log
│   └── login/              # Sign-in
├── backend/                # Spring Boot backend
│   └── src/main/
│       ├── java/com/example/telecom/
│       │   ├── api/        # Paging, search and time helpers
│       │   ├── audit/      # Audit trail
│       │   ├── security/   # Sign-in, roles and permissions
│       │   ├── controller/ # REST controllers
│       │   ├── model/      # JPA entities
│       │   ├── repository/ # Spring Data repositories
│       │   └── seed/       # Demo data generator
│       └── resources/db/migration/  # Flyway schema migrations
├── components/             # React components
│   └── ui/                 # shadcn/ui components
├── e2e/                    # Playwright tests
├── hooks/                  # Data-loading React hooks
└── lib/                    # API client, formatting and labels
```

## License

No open-source license is granted. See `LICENSE`.
