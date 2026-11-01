# PayShield AI

**A microservices-based payment risk assessment and monitoring
platform.**

PayShield AI is a portfolio project that simulates how payment
transactions can be assessed for potential risk, classified, and routed
into an analyst investigation workflow.

The platform is designed to receive transaction data, evaluate it using
deterministic Java-based rules, create alerts and investigation cases
when configured risk thresholds are met, and provide analysts with tools
to review the evidence. Generative AI is planned as an assistant for
explaining and summarizing recorded information; it is not intended to
determine risk scores, declare fraud, or make case decisions.

> **Project status:** The Identity Service and local PostgreSQL/Kafka
> infrastructure are implemented. The Transaction, Risk, Investigation,
> API Gateway, frontend, and AI capabilities are planned or in progress.
> This is a simulated portfolio platform using synthetic data, not a
> certified payment, banking, or fraud-detection system.

------------------------------------------------------------------------

## Contents

-   [Project goals](#project-goals)
-   [Planned architecture](#planned-architecture)
-   [Technology stack](#technology-stack)
-   [Service responsibilities](#service-responsibilities)
-   [Security design](#security-design)
-   [Transaction risk workflow](#transaction-risk-workflow)
-   [Local infrastructure](#local-infrastructure)
-   [Getting started](#getting-started)
-   [Project roadmap](#project-roadmap)
-   [Design principles](#design-principles)

## Project goals

-   Assess incoming transactions and classify their risk as **LOW**,
    **MEDIUM**, or **HIGH**.
-   Keep each microservice responsible for its own data and business
    logic.
-   Use asynchronous events for the transaction-to-risk-to-investigation
    workflow.
-   Provide role-based access for administrators, analysts, and
    auditors.
-   Preserve evidence and audit history so risk decisions and
    investigation actions can be understood.
-   Use Generative AI to explain recorded evidence and summarize
    investigations while keeping decisions under deterministic rules and
    human control.

## Planned architecture

The target architecture contains a React single-page application, an API
Gateway, and four Spring Boot services. Each service owns its own
PostgreSQL database.

``` text
                    React Frontend
                          |
                     HTTPS / JWT
                          |
                     API Gateway
                       :8080
                          |
        +-----------------+------------------+
        |                 |                  |
        v                 v                  v
 Identity Service   Transaction Service   Risk Service
     :8081                :8082               :8083
        |                   |                   |
   identity_db        transaction_db          risk_db
                            |                   ^
                            |                   |
                            +------ Kafka ------+
                                      |
                                      v
                              Investigation Service
                                      :8084
                                      |
                               investigation_db
                                      |
                               AI assistance module
                                      |
                                  LLM provider
```

This diagram describes the target design, not a claim that every
component is already implemented.

### Communication patterns

-   **Synchronous REST:** Request/response interactions, such as
    frontend calls through the gateway.
-   **Asynchronous Kafka events:** The planned transaction,
    risk-evaluation, and investigation workflows.
-   **Database per service:** Services own their databases. One service
    must not directly query another service's tables. Cross-service
    information is exchanged through APIs or events.

## Technology stack

  Area                      Technology
  ------------------------- --------------------------------------------
  Backend                   Java, Spring Boot
  Security                  Spring Security, JWT, RSA / RS256, SHA-256
  Token key distribution    JWKS
  Database                  PostgreSQL
  Messaging                 Apache Kafka
  Frontend (planned)        React
  AI assistance (planned)   Generative AI / LLM provider
  Local infrastructure      Docker, Docker Compose
  Deployment (planned)      Kubernetes

Exact framework and dependency versions are defined by the individual
service build files and will be documented as implementation evolves.

## Service responsibilities

  -----------------------------------------------------------------------
  Service                 Responsibility          Status
  ----------------------- ----------------------- -----------------------
  Identity Service        Authentication,         **Implemented**
                          access-token issuance,  
                          refresh-token           
                          lifecycle, user         
                          administration, roles   
                          and authorization       

  Transaction Service     Accept and validate     Planned / in progress
                          transactions, persist   
                          them, support queries   
                          and publish             
                          transaction-created     
                          events                  

  Risk Service            Consume transaction     Planned
                          events, maintain        
                          transaction history,    
                          evaluate deterministic  
                          rules, calculate risk   
                          scores and publish      
                          results                 

  Investigation Service   Create alerts and       Planned
                          cases, support analyst  
                          workflows, record       
                          outcomes, notes and     
                          audit events            

  API Gateway             Route requests, apply   Planned
                          gateway-level controls  
                          and propagate           
                          correlation IDs         

  React Frontend          Dashboard, transaction  Planned
                          explorer, alert queue,  
                          case workspace and      
                          administration screens  

  AI module               Explain recorded alert  Planned
                          evidence, summarize     
                          cases and answer        
                          case-scoped questions   
  -----------------------------------------------------------------------

## Security design

Security is a foundational part of PayShield AI.

### Asymmetric JWT signing and verification

The Identity Service uses **RSA with RS256** to sign access tokens:

-   The **private key** is held by the Identity Service and used to sign
    tokens.
-   The corresponding **public key** is made available to resource
    services through a JWKS endpoint.
-   Each resource service is designed to verify a token's signature and
    validate its expiry, issuer, and audience independently.

RS256 uses SHA-256 as the hash function in the RSA signature process. It
is a digital-signature mechanism; it does not encrypt the JWT payload.

### Why validate tokens in every service?

The API Gateway is an entry point, but it should not be the only
security boundary. Independent validation by each service follows a
defense-in-depth approach: a service verifies the credential before
authorizing access to its own resources.

### Roles

The authorization model defines three roles:

-   **ADMIN:** Manages users and risk rules, and submits or generates
    transactions.
-   **ANALYST:** Reviews alerts and cases, adds notes, records
    investigation outcomes, and uses permitted AI assistance.
-   **AUDITOR:** Has read-only access to transactions, alerts, case
    outcomes, and audit history.

The Identity Service implements role-based access control. Endpoint
permissions are enforced by the relevant services.

### Refresh tokens

The Identity Service uses opaque refresh tokens, stores their SHA-256
hashes rather than raw token values, and supports token rotation and
revocation.

## Transaction risk workflow

The target business workflow is:

1.  A transaction is submitted to the Transaction Service.
2.  The service validates and stores the transaction.
3.  A transaction-created event is published to Kafka.
4.  The Risk Service consumes the event and evaluates configured
    deterministic rules.
5.  The service records a bounded risk score, risk category, and rule
    evidence.
6.  The Investigation Service consumes the risk result and creates an
    alert and case when the configured threshold is met.
7.  An analyst reviews the evidence and records an outcome.
8.  Relevant actions are recorded in an audit trail.

Risk categories are **LOW**, **MEDIUM**, and **HIGH**. A risk score is a
rule-based assessment for this simulation; it is not a statistical
probability that fraud has occurred.

### Role of Generative AI

Generative AI is intended to assist with understanding recorded
information, for example:

-   Explain why an alert was raised using the available rule evidence.
-   Summarize an investigation timeline and analyst notes.
-   Answer questions scoped to a particular case.

AI output must be clearly identified as AI-generated and subject to
analyst review. The model does not set risk scores, declare fraud, close
cases, or perform write actions.

## Local infrastructure

The root `docker-compose.yml` currently defines the local infrastructure
dependencies:

-   **PostgreSQL 17 Alpine**, exposed on host port `5433`.
-   **Apache Kafka 4.3.1**, exposed to host applications on
    `localhost:9093` and to other Compose containers through
    `kafka:19092`.
-   Named Docker volumes for persistent PostgreSQL and Kafka data.
-   PostgreSQL initialization scripts mounted from
    `infrastructure/docker/postgres/init`.
-   Health checks for PostgreSQL and Kafka.

The Compose file provisions infrastructure dependencies; it does not, by
itself, build or start the Spring Boot services or frontend.

## Getting started

### Prerequisites

-   Docker Desktop or Docker Engine with the Docker Compose plugin.
-   Git.
-   Java and Maven (or the Maven Wrapper) for running the Spring Boot
    services.

### 1. Clone the repository

``` bash
git clone https://github.com/ShyamShu/PlayShield.git
cd PlayShield/payshield-ai
```

### 2. Configure environment variables

Create a local `.env` file beside `docker-compose.yml`. Supply strong
local development values for the variables referenced by the Compose
file:

``` dotenv
POSTGRES_ADMIN_USER=your_local_admin_user
POSTGRES_ADMIN_PASSWORD=your_local_admin_password

IDENTITY_DB_PASSWORD=your_identity_db_password
TRANSACTION_DB_PASSWORD=your_transaction_db_password
RISK_DB_PASSWORD=your_risk_db_password
INVESTIGATION_DB_PASSWORD=your_investigation_db_password
```

Use your own values. Do not commit `.env`, private keys, passwords,
access tokens, or other secrets to Git.

### 3. Start PostgreSQL and Kafka

``` bash
docker compose up -d
```

Check container status:

``` bash
docker compose ps
```

Follow logs:

``` bash
docker compose logs -f postgres kafka
```

Stop the infrastructure:

``` bash
docker compose down
```

To remove the persisted local database and Kafka data as well, use
`docker compose down -v`. **This permanently deletes the data stored in
those Compose volumes.**

### 4. Run application services

The root Compose file currently starts the infrastructure dependencies,
not the application services. Run each Spring Boot service using its own
Maven Wrapper and service-specific configuration.

Configure connection strings according to where the application runs:

  -----------------------------------------------------------------------
  Dependency              Application running on  Application running in
                          host                    Compose
  ----------------------- ----------------------- -----------------------
  PostgreSQL              `localhost:5433`        `postgres:5432`

  Kafka                   `localhost:9093`        `kafka:19092`
  -----------------------------------------------------------------------

## Project roadmap

-   [x] Repository and local PostgreSQL/Kafka infrastructure setup
-   [x] Identity Service foundation
-   [x] RSA-based JWT signing and role-based authorization foundation
-   [ ] Transaction Service and transaction persistence
-   [ ] Transactional outbox and Kafka event publishing
-   [ ] Risk Service and deterministic rule engine
-   [ ] Investigation Service, alerts, cases and audit trail
-   [ ] API Gateway
-   [ ] React dashboard
-   [ ] Generative AI assistance
-   [ ] Integration tests, reliability and security hardening
-   [ ] Kubernetes deployment and operational documentation

## Design principles

-   **Clear service ownership:** Each microservice owns its domain logic
    and database.
-   **Defense in depth:** Security checks are performed at the service
    boundary, not only at the gateway.
-   **Reliable event processing:** The planned event pipeline uses an
    outbox pattern, bounded retries, dead-letter topics, and idempotent
    consumers.
-   **Traceability:** Correlation IDs and audit events help follow
    requests and important business actions.
-   **Human accountability:** Analysts make investigation decisions; AI
    provides assistance rather than authority.
-   **Incremental delivery:** Implemented capabilities are distinguished
    from planned capabilities.

------------------------------------------------------------------------

## Repository

GitHub: https://github.com/ShyamShu/PlayShield/tree/devlop/payshield-ai

**PayShield AI is a learning and portfolio project focused on applying
backend engineering and distributed-systems concepts to a simulated
payment risk monitoring use case.**
