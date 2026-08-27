# Banco SRJM

Sistema bancário didático com abertura de contas, autenticação, depósito, saque, PIX interno, extrato e painel administrativo.

## Arquitetura

```text
Navegador
   │  http://localhost:8088
   ▼
Nginx (frontend e proxy /api)
   │
   ▼
Spring Boot 3 + Java 17
   │  JPA / Flyway
   ▼
PostgreSQL 16
```

| Componente | Responsabilidade |
| --- | --- |
| `frontend/` | Interface responsiva em HTML, CSS e JavaScript |
| `backend/` | API REST, autenticação e regras bancárias |
| `postgres` | Persistência de clientes, contas e transações |
| `docker-compose.yml` | Inicializa e conecta os três serviços |

Somente o Nginx publica uma porta. A API e o PostgreSQL permanecem nas redes internas do Docker.

## Tecnologias

- Java 17 e Spring Boot 3.3;
- Spring Web, Validation e Data JPA;
- PostgreSQL 16 e Flyway;
- BCrypt para senhas;
- Apache PDFBox e Java2D para extratos;
- Nginx, Docker e Docker Compose.

## Como executar

Requisito: Docker Engine ou Docker Desktop com Docker Compose v2.

```bash
docker compose up --build
```

Acessos:

- aplicação: `http://localhost:8088`;
- Swagger: `http://localhost:8088/api/swagger-ui.html`;
- health check: `http://localhost:8088/api/actuator/health`.

Parar sem apagar dados:

```bash
docker compose down
```

Apagar também o volume do PostgreSQL:

```bash
docker compose down -v
```

## Regras de negócio

- O banco gera um número aleatório de seis dígitos e garante sua unicidade.
- O dígito verificador é calculado pelo Módulo 11.
- O login utiliza a referência `numero-dv` e a senha criada na abertura.
- A senha é persistida somente como hash BCrypt.
- Consultas e operações só aceitam a conta autenticada na sessão.
- Depósito, saque e PIX exigem contas com status `ATIVA`.
- O PIX funciona apenas entre contas existentes e ativas do Banco SRJM.
- Saque e PIX não permitem saldo negativo.
- O PIX bloqueia origem e destino e grava débito e crédito na mesma transação do banco.
- Uma conta só pode ser encerrada quando estiver ativa, com saldo zerado e senha válida.
- O encerramento preserva o cliente e o histórico de transações.
- Contas administradoras podem consultar contas ativas, datas de abertura e saldos.

## Autenticação

O login cria uma sessão HTTP identificada pelo cookie `JSESSIONID`.

- cadastro, senha, saldo e transações ficam no PostgreSQL;
- a sessão ativa fica na memória do backend;
- reiniciar a API exige novo login, mas não remove o cadastro;
- uma sessão não pode consultar ou movimentar outra conta.

O perfil administrador é persistido em `accounts.administrator`. Neste projeto didático ele pode ser selecionado na abertura; em produção, essa permissão deve ser concedida apenas por um administrador autorizado.

## Modelo de dados

```text
clients
   └── accounts
          └── bank_transactions
```

- `clients`: nome e data de criação do cliente;
- `accounts`: número, DV, hash da senha, perfil, status e saldo;
- `bank_transactions`: tipo, direção, valor, saldo posterior, contraparte e data/hora.

Status: `ATIVA`, `BLOQUEADA` ou `ENCERRADA`. No extrato, `C` representa crédito e `D` representa débito.

## API principal

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/accounts` | Abrir conta |
| `POST` | `/api/auth/login` | Entrar com conta e senha |
| `GET` | `/api/auth/me` | Recuperar a conta da sessão |
| `DELETE` | `/api/auth/logout` | Encerrar a sessão |
| `GET` | `/api/accounts/{numero-dv}` | Consultar a própria conta |
| `PATCH` | `/api/accounts/{numero-dv}/status` | Alterar status |
| `POST` | `/api/accounts/{numero-dv}/close` | Encerrar conta |
| `GET` | `/api/accounts/{numero-dv}/statement` | Consultar extrato JSON |
| `GET` | `/api/accounts/{numero-dv}/statement/text` | Consultar extrato em texto |
| `GET` | `/api/accounts/{numero-dv}/statement/download?format=pdf` | Baixar PDF, PNG ou JPEG |
| `GET` | `/api/accounts/admin/active` | Listar contas ativas como administrador |
| `POST` | `/api/transactions/deposits` | Realizar depósito |
| `POST` | `/api/transactions/withdrawals` | Realizar saque |
| `POST` | `/api/transactions/pix` | Realizar PIX interno |

## Organização do backend

```text
controller/   Entrada HTTP e construção das respostas
service/      Autenticação e regras de negócio
repository/   Consultas e bloqueios no PostgreSQL
domain/       Entidades JPA e enums
dto/          Contratos JSON de entrada e saída
mapper/       Conversão de entidades para respostas seguras
exception/    Erros de validação e regras bancárias
util/         Referência numero-dv e cálculo do Módulo 11
config/       BCrypt e OpenAPI
```

Arquivos centrais:

- `BankService.java`: abertura, depósito, saque, PIX, saldo e encerramento;
- `AccountSessionService.java`: login, sessão, propriedade da conta e administrador;
- `StatementDocumentService.java`: geração de PDF, PNG e JPEG;
- `AccountController.java`: conta, extrato e administração;
- `TransactionController.java`: depósito, saque e PIX;
- `AuthController.java`: login, sessão atual e logout.

## Organização do frontend

- `index.html`: página pública, painel autenticado, formulários e modais;
- `assets/css/styles.css`: identidade visual e responsividade;
- `assets/js/app.js`: sessão, requisições, operações e máscaras;
- `nginx.conf`: arquivos estáticos e proxy `/api` para o backend.

Antes do login, apenas logotipo, slogan, entrada e abertura de conta ficam visíveis. As operações ficam no painel autenticado.

As máscaras permitem digitar somente números:

```text
2615339 → 261533-9
123456  → R$ 1.234,56
```

## Banco e migrações

O Hibernate usa `ddl-auto: validate`; ele valida o esquema, mas não altera tabelas. Toda mudança persistente deve ser uma nova migração em:

```text
backend/src/main/resources/db/migration/
```

Exemplo: `V7__add_client_document.sql`.

Nunca modifique uma migração que já tenha sido aplicada a um banco existente.

## Testes

Com Java 17 e Maven:

```bash
cd backend
mvn clean verify
```

Sem Java local:

```bash
docker run --rm \
  -v "$PWD/backend:/workspace" \
  -w /workspace \
  maven:3.9-eclipse-temurin-17 \
  mvn clean verify
```

Os testes cobrem o dígito verificador, regras de encerramento e geração de PDF, PNG e JPEG.

## Guia de alterações

| Necessidade | Local principal |
| --- | --- |
| Textos ou campos | `frontend/index.html` |
| Cores e layout | `frontend/assets/css/styles.css` |
| Interação e máscaras | `frontend/assets/js/app.js` |
| Regra bancária | `backend/.../service/BankService.java` |
| Autenticação | `backend/.../service/AccountSessionService.java` |
| Endpoint | `backend/.../controller/` |
| JSON da API | `backend/.../dto/` |
| Entidade | `backend/.../domain/` e nova migração |
| Consulta ao banco | `backend/.../repository/` |
| Extrato para download | `StatementDocumentService.java` |
| Portas e banco | `application.yml` e `docker-compose.yml` |

Fluxo recomendado: migração, entidade/DTO/repository, service, controller, frontend, testes e `docker compose up --build`.
