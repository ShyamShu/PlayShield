# PayShield AI

**A microservices-based payment risk assessment and monitoring
platform.**

PayShield AI is a portfolio project that simulates how existing payment
transaction records can be assessed for potential risk, classified, and
routed into an analyst investigation workflow.

The platform is designed to receive transaction data, evaluate it using
deterministic Java-based rules, create alerts and investigation cases
when configured risk thresholds are met, and provide analysts with tools
to review the evidence. Generative AI is planned as an assistant for
explaining and summarizing recorded information; it is not intended to
determine risk scores, declare fraud, or make case decisions.

> **Project status:** The Identity Service and Transaction Service
> foundations are implemented, including transaction persistence,
> idempotent submission, a transactional outbox, and Kafka event
> publishing. Local PostgreSQL/Kafka infrastructure is implemented. The
> Risk Service, Investigation Service, API Gateway, frontend, and AI
> capabilities are planned. This is a simulated portfolio platform using
> synthetic data, not a certified payment, banking, or fraud-detection
> system.

------------------------------------------------------------------------

## Contents

-   [Project goals](#project-goals)
-   [Current implementation](#current-implementation)
-   [Architecture](#architecture)
-   [Technology stack](#technology-stack)
-   [Service responsibilities](#service-responsibilities)
-   [Security design](#security-design)
-   [Transaction ingestion and event
    workflow](#transaction-ingestion-and-event-workflow)
-   [Event contract](#event-contract)
-   [Local infrastructure](#local-infrastructure)
-   [Getting started](#getting-started)
-   [API overview](#api-overview)
-   [Project roadmap](#project-roadmap)
-   [Design principles](#design-principles)

## Project goals

-   Assess submitted transaction records and eventually classify their
    risk as **LOW**, **MEDIUM**, or **HIGH**.
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

**Domain boundary:** PayShield is a transaction risk monitoring and
assessment platform. It receives records of transactions that have
already been processed by an external payment system. It does not
initiate, authorize, decline, settle, transfer, or execute payments.
`paymentStatus` describes the status in the submitted external
transaction record; a future PayShield `riskLevel` will be a separate
assessment.

## Current implementation

### Identity Service

-   Spring Security-based authentication and authorization foundation.
-   RSA/RS256 access-token signing.
-   Resource services verify JWTs using the corresponding public
    key/JWKS configuration.
-   Role-based authorization foundation for `ADMIN`, `ANALYST`, and
    `AUDITOR`.
-   Opaque refresh-token design with SHA-256 token-hash storage,
    rotation, and revocation.

### Transaction Service

-   REST API to submit transactions, retrieve a transaction by ID, and
    retrieve paginated/filterable transaction results.
-   Request validation for transaction fields, supported transaction
    types, payment statuses, channels, currency, and positive monetary
    amounts.
-   Authenticated administrator identity is taken from the JWT subject;
    it is not accepted from the request body.
-   Idempotent transaction submission using the `Idempotency-Key`
    header, a request hash, and a database uniqueness constraint scoped
    to the submitting administrator.
-   Repeated requests with the same key and same payload return the
    existing transaction; reuse of the same key with a different payload
    returns a conflict.
-   PostgreSQL persistence with database constraints and indexes.
-   Transaction and outbox-event records are written in the same
    database transaction.

### Transactional Outbox and Kafka publisher

-   Outbox records are stored in PostgreSQL alongside transaction
    records.
-   A scheduled publisher claims unpublished events and publishes them
    to the `transaction-submitted` Kafka topic.
-   PostgreSQL `FOR UPDATE SKIP LOCKED` is used to allow concurrent
    publishers to claim different rows.
-   Claim tokens and lease expiry support recovery when a publisher
    stops while holding claimed events.
-   Kafka publication is awaited before an event is marked as published
    in PostgreSQL.
-   Failed publication attempts are recorded and rescheduled using
    exponential backoff with jitter.
-   Kafka messages use `customerRef` as the key so events for the same
    customer are routed to the same partition.
-   Correlation IDs are accepted through `X-Correlation-ID` or generated
    by the Transaction Service and propagated into the outbox and event.
-   The event payload is strongly typed. Monetary amounts are serialized
    as decimal strings, timestamps as ISO-8601 strings, and
    administrator identity (`submittedBy`) is excluded from the
    downstream event payload.

**Delivery semantics:** The outbox publisher provides reliable
at-least-once-style delivery, not end-to-end exactly-once processing. A
process can fail after Kafka acknowledges a message but before
PostgreSQL records `published_at`, so a message may be published again.
The future consumer must deduplicate by `eventId`. A maximum retry
policy and dead-letter handling are still planned.

## Architecture

The implemented transaction ingestion and event-publishing path is:

``` text
                 Admin / External Transaction Source
                               |
                    HTTPS / JWT / REST API
                               |
                               v
                       Transaction Service
                         :8082
                               |
                 +-------------+-------------+
                 |                           |
                 v                           v
          transactions table           outbox_events table
                 |                           |
                 +------ same PostgreSQL transaction
                                             |
                                             v
                                     Outbox Publisher
                                             |
                                             v
                                  Kafka: transaction-submitted
                                             |
                                             v
                               Future Risk Assessment Service
                                             |
                                             v
                              Future Investigation Service
```

The target architecture also includes an API Gateway, frontend, and AI
assistance module:

``` text
                         React Frontend
                              |
                         HTTPS / JWT
                              |
                         API Gateway
                            :8080
                              |
             +----------------+----------------+
             |                |                |
             v                v                v
       Identity Service  Transaction Service  Risk Service
            :8081              :8082             :8083
                                |                  ^
                                |                  |
                                +------ Kafka -----+
                                       |
                                       v
                              Investigation Service
                                      :8084
                                       |
                              AI assistance module
                                       |
                                  LLM provider
```

The second diagram describes the target design; it does not imply that
every component is implemented.

### Communication patterns

-   **Synchronous REST:** Request/response interactions, such as
    transaction submission and queries.
-   **Asynchronous Kafka events:** The implemented Transaction Service
    publishes transaction-submitted events. Risk evaluation and
    investigation event flows are future work.
-   **Database per service:** Services own their databases. One service
    must not directly query another service's tables. Cross-service
    information is exchanged through APIs or events.

## Technology stack

  ------------------------------------------------------------------------
  Area                    Technology              Status
  ----------------------- ----------------------- ------------------------
  Backend                 Java, Spring Boot       Implemented for Identity
                                                  and Transaction
                                                  foundations

  Security                Spring Security, JWT,   Identity foundation
                          RSA / RS256, SHA-256    implemented

  Token key distribution  JWKS / public-key       Implemented/configured
                          verification            for resource-service
                                                  validation

  Database                PostgreSQL              Implemented locally;
                                                  Identity and Transaction
                                                  data

  Database migrations     Flyway                  Used by Transaction
                                                  Service

  Persistence             Spring Data JPA,        Implemented in
                          Hibernate               Transaction Service

  Messaging               Apache Kafka            Local broker, topic, and
                                                  Transaction Service
                                                  publisher implemented

  Event reliability       Transactional Outbox,   Implemented in
                          retries, claim leases   Transaction Service

  Local infrastructure    Docker, Docker Compose  Implemented

  Frontend                React                   Planned

  AI assistance           Generative AI / LLM     Planned
                          provider                

  Deployment              Kubernetes              Planned
  ------------------------------------------------------------------------

Exact framework and dependency versions are defined by the individual
service build files.

## Service responsibilities

  -----------------------------------------------------------------------
  Service / component     Responsibility          Status
  ----------------------- ----------------------- -----------------------
  Identity Service        Authentication,         Implemented foundation
                          access-token issuance,  
                          refresh-token           
                          lifecycle, user         
                          administration, roles,  
                          and authorization       

  Transaction Service     Accept and validate     Implemented foundation
                          external transaction    
                          records, persist and    
                          query them, enforce     
                          idempotency, and create 
                          outbox events           

  Outbox Publisher        Claim pending outbox    Implemented and locally
                          rows, publish to Kafka, verified
                          track acknowledgements, 
                          and retry failures      

  Risk Service            Consume transaction     Planned
                          events, maintain        
                          transaction history,    
                          evaluate deterministic  
                          rules, calculate risk   
                          scores, and publish     
                          results                 

  Investigation Service   Create alerts and       Planned
                          cases, support analyst  
                          workflows, record       
                          outcomes, notes, and    
                          audit events            

  API Gateway             Route requests, apply   Planned
                          gateway-level controls, 
                          and propagate           
                          correlation IDs         

  React Frontend          Dashboard, transaction  Planned
                          explorer, alert queue,  
                          case workspace, and     
                          administration screens  

  AI module               Explain recorded alert  Planned
                          evidence, summarize     
                          cases, and answer       
                          case-scoped questions   
  -----------------------------------------------------------------------

## Security design

Security is a foundational part of PayShield AI.

### Asymmetric JWT signing and verification

The Identity Service uses **RSA with RS256** to sign access tokens:

-   The **private key** is held by the Identity Service and used to sign
    tokens.
-   The corresponding **public key** is made available to resource
    services through a JWKS endpoint/configuration.
-   Resource services validate a token's signature and relevant claims
    such as expiry and issuer before authorizing access.

RS256 uses SHA-256 as the hash function in the RSA signature process. It
is a digital-signature mechanism; it does not encrypt the JWT payload.

### Why validate tokens in every service?

The API Gateway is an entry point, but it should not be the only
security boundary. Independent validation by each service follows a
defense-in-depth approach: a service verifies the credential before
authorizing access to its own resources.

### Roles

The authorization model defines three roles:

-   **ADMIN:** Manages users and risk rules, and submits transaction
    records for monitoring.
-   **ANALYST:** Reviews alerts and cases, adds notes, records
    investigation outcomes, and uses permitted AI assistance.
-   **AUDITOR:** Has read-only access to transactions, alerts, case
    outcomes, and audit history.

The Identity Service implements the role-based access-control
foundation. The Transaction Service restricts transaction submission to
administrators and permits authorized roles to retrieve transaction
data.

### Refresh tokens

The Identity Service uses opaque refresh tokens, stores their SHA-256
hashes rather than raw token values, and supports token rotation and
revocation.

## Transaction ingestion and event workflow

The implemented Transaction Service workflow is:

1.  An authenticated administrator submits an external transaction
    record to the Transaction Service.
2.  The service validates the request and extracts the administrator
    identity from the JWT.
3.  The service checks the idempotency key and request hash to avoid
    duplicate submissions.
4.  The transaction record and its `TRANSACTION_SUBMITTED` outbox event
    are persisted in the same PostgreSQL transaction.
5.  The scheduled outbox publisher claims eligible events.
6.  The publisher serializes the event and sends it to Kafka using
    `customerRef` as the message key.
7.  After Kafka acknowledges publication, the publisher marks the outbox
    event as published. On failure, it records the attempt and schedules
    a retry.

The planned downstream workflow is:

1.  The Risk Service consumes the transaction event.
2.  It deduplicates events using `eventId` and evaluates configured
    deterministic rules.
3.  It records a bounded risk score, risk category, and rule evidence.
4.  The Investigation Service consumes risk results and creates an alert
    and case when the configured threshold is met.
5.  An analyst reviews the evidence and records an outcome.
6.  Relevant actions are recorded in an audit trail.

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

## Event contract

The Transaction Service publishes events to the Kafka topic:

``` text
transaction-submitted
```

The current event envelope has the following shape:

``` json
{
  "eventId": "unique-event-uuid",
  "eventType": "TRANSACTION_SUBMITTED",
  "schemaVersion": 1,
  "aggregateType": "TRANSACTION",
  "aggregateId": "transaction-uuid",
  "correlationId": "request-correlation-id",
  "createdAt": "2026-11-22T10:12:57.831436Z",
  "payload": {
    "transactionId": "transaction-uuid",
    "customerRef": "CUS-1001",
    "merchantRef": "MER-2001",
    "amount": "1500.50",
    "currency": "INR",
    "transactionType": "PURCHASE",
    "paymentStatus": "AUTHORIZED",
    "channel": "ONLINE",
    "deviceRef": "DEV-1001",
    "description": "Online shopping purchase",
    "occurredAt": "2026-11-22T08:30:00Z",
    "correlationId": "request-correlation-id"
  }
}
```

The example values are illustrative.

Contract notes:

-   `eventId` uniquely identifies the outbox event and will be used by
    consumers for deduplication.
-   `schemaVersion` identifies the payload contract version.
-   `aggregateId` identifies the transaction.
-   `correlationId` connects the HTTP request and downstream event
    processing.
-   `amount` is serialized as a decimal string to preserve its exact
    decimal representation.
-   `occurredAt` and `createdAt` are ISO-8601 timestamps.
-   `customerRef` is the Kafka message key. Events for the same customer
    are routed to the same partition, preserving the order in which
    Kafka receives them within that partition.
-   `submittedBy` is retained in the Transaction Service database for
    auditing but is not included in the downstream event payload.

The event publisher uses Kafka acknowledgement before marking an outbox
row as published. This still permits duplicate delivery in a crash
window; downstream consumers must be idempotent. Per-customer ordering
across multiple concurrent publisher instances is not yet guaranteed.

## Local infrastructure

The root `docker-compose.yml` defines the local infrastructure
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

### Kafka topic

The Transaction Service publishes to `transaction-submitted`, configured
locally with three partitions and replication factor one.

Connection addresses depend on where the client runs:

  Client location                         Bootstrap address
  --------------------------------------- -------------------
  Spring Boot running on Windows/host     `localhost:9093`
  Kafka CLI inside the Kafka container    `localhost:19092`
  Another service inside Docker Compose   `kafka:19092`

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

Create a local `.env` file beside `docker-compose.yml`. Supply local
development values for the variables referenced by the Compose file:

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

The root Compose file starts the infrastructure dependencies, not the
application services. Run each Spring Boot service using its own Maven
Wrapper and service-specific configuration.

For the Transaction Service, run from its service directory:

``` powershell
.\mvnw.cmd spring-boot:run
```

Configure connection strings according to where the application runs:

  -----------------------------------------------------------------------
  Dependency              Application running on  Application running in
                          host                    Compose
  ----------------------- ----------------------- -----------------------
  PostgreSQL              `localhost:5433`        `postgres:5432`

  Kafka                   `localhost:9093`        `kafka:19092`
  -----------------------------------------------------------------------

The Transaction Service runs on port `8082` in the current local
configuration. Check the service's `application.yml` and environment
configuration for database credentials, JWT public-key/JWKS settings,
and Kafka settings before starting it.

### 5. Create the Kafka topic

If the topic has not already been created and Kafka automatic topic
creation is disabled, run this from PowerShell:

``` powershell
docker exec -it payshield-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:19092 --create --topic transaction-submitted --partitions 3 --replication-factor 1
```

Describe the topic:

``` powershell
docker exec -it payshield-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:19092 --describe --topic transaction-submitted
```

### 6. Submit a transaction

The Transaction Service exposes:

``` http
POST http://localhost:8082/api/v1/transactions
```

Required headers:

``` http
Authorization: Bearer <access-token>
Content-Type: application/json
Idempotency-Key: txn-local-001
X-Correlation-ID: payshield-local-001
```

`X-Correlation-ID` is optional; if omitted, the service generates one.
`Idempotency-Key` is required.

Example request body:

``` json
{
  "customerRef": "CUS-1001",
  "merchantRef": "MER-2001",
  "amount": 1500.50,
  "currency": "INR",
  "transactionType": "PURCHASE",
  "paymentStatus": "AUTHORIZED",
  "channel": "ONLINE",
  "deviceRef": "DEV-1001",
  "description": "Online shopping purchase",
  "occurredAt": "2026-11-22T08:30:00Z"
}
```

The administrator identity is extracted from the access token. Do not
send `submittedBy` in the request body.

The endpoint returns `201 Created` for a new transaction and `200 OK`
when an identical idempotent request is replayed.

### 7. Inspect published Kafka events

Run the Kafka console consumer from PowerShell:

``` powershell
docker exec -it payshield-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:19092 --topic transaction-submitted --from-beginning --formatter-property print.key=true
```

The printed key should be the `customerRef`. The message value should
contain the event envelope and typed payload described in [Event
contract](#event-contract).

## API overview

Current Transaction Service endpoints:

  -----------------------------------------------------------------------------
  Method                  Endpoint                      Purpose
  ----------------------- ----------------------------- -----------------------
  `POST`                  `/api/v1/transactions`        Submit a transaction
                                                        record (ADMIN)

  `GET`                   `/api/v1/transactions/{id}`   Retrieve a transaction
                                                        by ID

  `GET`                   `/api/v1/transactions`        Retrieve paginated and
                                                        filterable transactions
  -----------------------------------------------------------------------------

The list endpoint supports filters for fields such as customer
reference, merchant reference, payment status, transaction type,
channel, currency, and occurred-at date range, along with pagination and
sorting.

Example:

``` http
GET /api/v1/transactions?customerRef=CUS-1001&paymentStatus=AUTHORIZED&page=0&size=20&sort=createdAt,desc
```

Access is protected by JWT authentication and role-based authorization.
Consult the controller and service configuration for the exact current
permission rules.

## Project roadmap

-   [x] Repository and local PostgreSQL/Kafka infrastructure setup
-   [x] Identity Service foundation
-   [x] RSA-based JWT signing and role-based authorization foundation
-   [x] Transaction Service API and transaction persistence foundation
-   [x] Transaction validation, filtering, and pagination
-   [x] Idempotent transaction submission
-   [x] Transactional outbox persistence
-   [x] Kafka outbox publisher
-   [x] Concurrent outbox claiming with `FOR UPDATE SKIP LOCKED`
-   [x] Claim tokens, leases, and retry backoff with jitter
-   [x] Typed transaction event contract
-   [x] Correlation ID propagation
-   [x] Customer-based Kafka message key
-   [ ] Consumer deduplication and offset management
-   [ ] Maximum retry policy and dead-letter handling
-   [ ] Risk Service and deterministic rule engine
-   [ ] Investigation Service, alerts, cases, and audit trail
-   [ ] API Gateway
-   [ ] React dashboard
-   [ ] Generative AI assistance
-   [ ] Integration tests, reliability, and security hardening
-   [ ] Kubernetes deployment and operational documentation

## Design principles

-   **Clear service ownership:** Each microservice owns its domain logic
    and database.
-   **Defense in depth:** Security checks are performed at the service
    boundary, not only at the gateway.
-   **Reliable event publishing:** Transaction and outbox records are
    committed atomically; a publisher sends events asynchronously and
    records delivery state.
-   **Idempotency:** API retries are handled using idempotency keys and
    request hashes. Future consumers must deduplicate by event ID.
-   **Traceability:** Correlation IDs and audit events help follow
    requests and important business actions.
-   **Data minimization:** Events contain information needed by
    downstream services, not unnecessary administrator identity data.
-   **Customer event partitioning:** Kafka messages use customer
    references as keys to group a customer's events in one partition.
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
