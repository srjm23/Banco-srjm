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
- A recuperação de senha envia um link de uso único, válido por 30 minutos, ao e-mail cadastrado.
- Tokens de recuperação são persistidos somente como hash SHA-256 e invalidados após o uso.
- A abertura exige e-mail, telefone com DDD e CPF válido, todos únicos por cliente.
- O CPF é cifrado com AES-GCM antes de ser persistido; um hash SHA-256 separado permite verificar unicidade sem expor o número.
- Consultas e operações só aceitam a conta autenticada na sessão.
- Depósito, saque e PIX exigem contas com status `ATIVA`.
- O PIX funciona apenas entre contas existentes e ativas do Banco SRJM.
- Cada conta pode registrar chaves PIX de e-mail, telefone, CPF e uma aleatória alfanumérica de 12 caracteres.
- O PIX é realizado somente entre contas do Banco SRJM por meio de uma chave PIX previamente cadastrada.
- A conta de origem precisa possuir saldo suficiente para o valor integral do PIX.
- Saque e PIX não permitem saldo negativo.
- O PIX bloqueia origem e destino e grava débito e crédito na mesma transação do banco.
- Uma conta só pode ser encerrada quando estiver ativa, com saldo zerado e senha válida.
- O encerramento preserva o cliente e o histórico de transações.
- Contas administradoras são exclusivamente gerenciais: não realizam depósito, saque ou PIX e não exibem área de transações.
- O painel administrativo consulta somente contas de clientes, seus dados, CPF cifrado, status e saldo, além do saldo total consolidado do banco.
- A abertura comum não realiza depósito inicial; o saldo começa zerado.
- A conta administradora é aberta apenas com nome, senha e o token definido em `ADMINISTRATOR_CREATION_TOKEN` (`srjm` no ambiente local).
- Administradores podem listar todas as contas, incluindo contatos, perfil, status e saldo, sem expor senha ou CPF.

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
          ├── pix_keys
          └── bank_transactions
```

- `clients`: nome, e-mail, telefone, CPF cifrado, hash do CPF e data de criação do cliente;
- `accounts`: número, DV, hash da senha, perfil, status e saldo;
- `pix_keys`: tipo, valor único, conta vinculada e data de criação;
- `bank_transactions`: tipo, direção, valor, saldo posterior, contraparte e data/hora.

Status: `ATIVA`, `BLOQUEADA` ou `ENCERRADA`. No extrato, `C` representa crédito e `D` representa débito.

## API principal

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/accounts` | Abrir conta |
| `POST` | `/api/auth/login` | Entrar com conta e senha |
| `GET` | `/api/auth/me` | Recuperar a conta da sessão |
| `DELETE` | `/api/auth/logout` | Encerrar a sessão |
| `POST` | `/api/auth/forgot-password` | Solicitar link de redefinição de senha |
| `POST` | `/api/auth/reset-password` | Criar uma nova senha com o token recebido |
| `GET` | `/api/accounts/{numero-dv}` | Consultar a própria conta |
| `GET` | `/api/accounts/{numero-dv}/pix-keys` | Listar as próprias chaves PIX |
| `POST` | `/api/accounts/{numero-dv}/pix-keys` | Criar chave PIX de e-mail, telefone ou aleatória |
| `PATCH` | `/api/accounts/{numero-dv}/status` | Alterar status |
| `POST` | `/api/accounts/{numero-dv}/close` | Encerrar conta |
| `GET` | `/api/accounts/{numero-dv}/statement` | Consultar extrato JSON |
| `GET` | `/api/accounts/{numero-dv}/statement/text` | Consultar extrato em texto |
| `GET` | `/api/accounts/{numero-dv}/statement/download?format=pdf` | Baixar PDF, PNG ou JPEG |
| `GET` | `/api/accounts/admin/all` | Listar todas as contas como administrador |
| `POST` | `/api/transactions/deposits` | Realizar depósito |
| `POST` | `/api/transactions/withdrawals` | Realizar saque |
| `POST` | `/api/transactions/pix` | Realizar PIX interno |

## E-mail de recuperação

No ambiente Docker, o Mailpit recebe os e-mails localmente. Depois de solicitar a recuperação, abra `http://localhost:8025` para acessar a caixa de entrada de desenvolvimento.

Para envio real, configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` e `FRONTEND_URL` no ambiente. O `FRONTEND_URL` deve apontar para a URL pública da aplicação.

Defina também `DATA_ENCRYPTION_KEY` com um segredo longo e exclusivo em produção. Essa chave deve ser preservada com segurança: sem ela, não é possível decifrar os CPFs já cadastrados.

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
