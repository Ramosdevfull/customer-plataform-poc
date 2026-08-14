# demo-customer-service — Requirements

## O que a aplicação resolve

A plataforma resolve o problema de **gerenciar o ciclo de vida de clientes** dentro
de um ecossistema distribuído, integrando o cadastro central com três sistemas
externos de forma segura, resiliente e assíncrona:

- **Cadastro e manutenção de clientes**: criar, editar, remover, consultar,
  listar e filtrar clientes por nome e status — regras de negócio isoladas do
  framework (arquitetura hexagonal), evitando que decisões de domínio fiquem
  acopladas a Spring, banco de dados ou mensageria.
- **Integração com serviço externo de crédito**: consultar o score de cada cliente
  a partir do CPF, sem derrubar a aplicação quando o serviço externo estiver
  lento ou indisponível (Circuit Breaker + Retry + Timeout + Fallback).
- **Integração assíncrona entre sistemas**: notificar outros serviços sobre a
  criação de clientes (`CUSTOMER_CREATED`) e receber atualizações de status
  (`CUSTOMER_STATUS_CHANGE`) via RabbitMQ, com garantia de idempotência —
  importantes em mensageria onde o broker pode entregar a mesma mensagem mais de
  uma vez (entrega "pelo menos uma vez").
- **Segurança de acesso**: API protegida por OAuth 2.0 / JWT (Resource Server),
  com separação clara entre perfis de leitura (`USER`) e escrita (`ADMIN`).

O resultado é um **template de microsserviço "senior"** pronto para uso real:
negócio testável independente de infraestrutura, resiliência comprovada,
mensageria segura contra duplicidades e ecossistema executável com um único
comando Docker Compose.

---

## Requisitos Funcionais (RF)

### RF01 — Gerenciamento de Clientes

A aplicação deve permitir a **criação (POST)**, **atualização (PUT)**,
**remoção (DELETE)** e **consulta por ID (GET)** de clientes.

| Operação | Endpoint | Status de sucesso |
| --- | --- | --- |
| Criar | `POST /customers` | `201 Created` |
| Atualizar | `PUT /customers/{id}` | `200 OK` |
| Remover | `DELETE /customers/{id}` | `204 No Content` |
| Consultar por ID | `GET /customers/{id}` | `200 OK` |

**Campos do cliente**: `id`, `name`, `cpf`, `email`, `status` (`ACTIVE`/`INACTIVE`).

**Validações**: nome e e-mail obrigatórios; e-mail com formato válido; CPF com
exatamente 11 dígitos numéricos; CPF duplicado retorna `409 Conflict`; cliente
inexistente retorna `404 Not Found`.

### RF02 — Listagem e Filtro de Clientes

A aplicação deve permitir a listagem geral e filtros:

| Operação | Endpoint |
| --- | --- |
| Listar todos | `GET /customers` |
| Filtrar por status | `GET /customers?status=ACTIVE\|INACTIVE` |
| Buscar por nome | `GET /customers/search?name=...` (case-insensitive) |

### RF03 — Consulta de Score Externo

A aplicação deve disponibilizar o endpoint `GET /customers/{id}/score`, que
consulta um serviço externo (`GET /scores/{cpf}`) usando o CPF do cliente e
retorna o score e sua classificação:

```json
{ "cpf": "12345678901", "score": 750, "classification": "LOW_RISK" }
```

### RF04 — Publicação de Evento de Criação

Ao criar um novo cliente com sucesso, a aplicação deve publicar o evento
`CUSTOMER_CREATED` no RabbitMQ (exchange `customer.exchange`, routing key
`customer.created`, fila `customer.created.queue`) com os dados do cliente e do
evento (`eventId`, `eventType`, `customerId`, `name`, `cpf`, `email`, `status`,
`createdAt`).

### RF05 — Processamento Assíncrono de Status

A aplicação deve consumir eventos `CUSTOMER_STATUS_CHANGE` da fila
`customer.status.change.queue` e atualizar o status do cliente correspondente.
O processamento deve ser **idempotente**: mensagens com `eventId` já processado
são descartadas (ACK) sem atualização redundante.

### RF06 — Autorização Baseada em Perfil

- Perfis **USER** podem apenas consultar dados (GET).
- Perfis **ADMIN** possuem acesso total (escrita, alteração e deleção).

---

## Requisitos Não Funcionais (RNF)

### RNF01 — Arquitetura e Linguagem

Construído com **Java 21+** e **Spring Boot 3+**. Implementação atual: Java 21,
Spring Boot 4.1.0, Spring Cloud 2025.1.2. Arquitetura **Hexagonal (Ports and
Adapters)**: domínio e casos de uso isolados de framework, banco e mensageria.

### RNF02 — Segurança

Acesso protegido via **OAuth 2.0 e JWT**, com a aplicação atuando como
**Resource Server** (Keycloak como Identity Provider). O claim `realm_access.roles`
é convertido em autoridades (`ROLE_USER`/`ROLE_ADMIN`) aplicadas às regras de
autorização dos endpoints.

### RNF03 — Idempotência e Consistência Eventual

O consumidor RabbitMQ deve ser idempotente, garantindo a consistência dos dados
mesmo em caso de mensagens duplicadas. Implementação: tabela
`tb_processed_messages` (chave `eventId`) consultada/gravada na mesma transação
do processamento.

### RNF04 — Resiliência e Tratamento de Erros

- **Circuit Breaker, Timeouts e Retry** (Resilience4j) na integração HTTP de
  Score: retry 2 tentativas, timeout 2s, janela 5 chamadas com 50% de falhas para
  abrir o circuito (open por 10s), com fallback gracioso
  (`score=0`, `classification=UNAVAILABLE`).
- Tratamento de **CPF duplicado** (`409`), **recurso não encontrado** (`404`) e
  **validação de entrada** (`400`) via `@ControllerAdvice`.
- **Dead Letter Exchange (DLX)** no RabbitMQ: falhas irreversíveis de
  processamento vão para a fila `customer.status.change.dlq`.

### RNF05 — Testabilidade

- Testes **unitários** com JUnit 5 / Mockito (casos de uso, listener, adaptadores).
- Testes de **integração** com Testcontainers (PostgreSQL e RabbitMQ reais).
- Total atual: 23 testes (17 unitários + 6 de integração).

### RNF06 — Containerização

A solução completa deve ser executável via **Docker Compose com um único comando**
(`docker compose up --build`), incluindo: `demo-customer-service`, `postgres`,
`rabbitmq`, `keycloak` (com realm importado automaticamente) e `wiremock`
(mock do serviço de Score).

---

## Resumo das tecnologias

| Camada | Tecnologia |
| --- | --- |
| Linguagem / Runtime | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Arquitetura | Hexagonal (Ports and Adapters) |
| Persistência | Spring Data JPA + PostgreSQL 16 |
| Mensageria | Spring AMQP + RabbitMQ 3 |
| Segurança | Spring Security OAuth2 Resource Server + Keycloak 22 |
| Resiliência | Resilience4j 2.3.0 (Circuit Breaker, Retry, TimeLimiter) |
| Testes | JUnit 5, Mockito, Testcontainers 2.0.5 |
| Containerização | Docker Compose (5 serviços) |
