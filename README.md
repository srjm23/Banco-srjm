# Banco SRJM

Sistema bancário didático com frontend web, API REST e PostgreSQL. Permite abertura de contas, login por conta ou CPF, depósito, pagamento de boleto, PIX interno, extrato, recuperação de senha e administração de contas.

## Arquitetura

```text
Navegador (localhost:8088)
          |
          v
Nginx: frontend + proxy /api
          |
          v
Spring Boot: API e regras bancárias
       |                 |
       v                 v
 PostgreSQL        SMTP / Mailpit
```

| Componente | Tecnologia | Responsabilidade |
| --- | --- | --- |
| `frontend/` | HTML, CSS, JavaScript e Nginx | Interface, formulários e proxy `/api` |
| `backend/` | Java e Spring Boot 3 | API, autenticação, validações e regras financeiras |
| `postgres/` | PostgreSQL | Clientes, contas, chaves PIX e transações |
| Mailpit | SMTP local | E-mails de recuperação |
| Flyway | Migrações SQL | Versionamento do esquema |
| Docker Compose | Contêineres | Build, redes, volumes e inicialização |

Somente o Nginx publica a aplicação. Backend e PostgreSQL permanecem nas redes internas do Docker.

## Imagens Chainguard

> **Destaque de segurança:** frontend, backend e PostgreSQL usam imagens-base da **Chainguard**, com superfície reduzida de ataque e menor quantidade de pacotes e CVEs.

| Componente | Imagem-base | Imagem gerada pelo projeto |
| --- | --- | --- |
| Frontend | `cgr.dev/chainguard/nginx:latest` | `srjm2024/banco-srjm-frontend:latest` |
| Build do backend | `cgr.dev/chainguard/maven:latest` | estágio de compilação |
| Runtime do backend | `cgr.dev/chainguard/jre:latest-dev` | `srjm2024/banco-srjm-backend:latest` |
| PostgreSQL | `cgr.dev/chainguard/postgres:latest` | `srjm2024/banco-srjm-postgres:latest` |

O runtime Java instala apenas `fontconfig` e `ttf-dejavu`, necessários para gerar extratos em PDF, PNG e JPEG. Em produção, recomenda-se fixar versões ou digests no lugar de `latest` para garantir builds reproduzíveis.

Imagem auxiliar: `axllent/mailpit:v1.27`, servidor de e-mail local acessível em `http://localhost:8025`.

## Como executar

Requisito: Docker Engine ou Docker Desktop com Docker Compose v2.

```bash
docker compose up --build
```

Acessos:

- aplicação: `http://localhost:8088`;
- e-mails locais: `http://localhost:8025`;
- Swagger: `http://localhost:8088/api/swagger-ui.html`;
- OpenAPI: `http://localhost:8088/api/openapi`;
- health check: `http://localhost:8088/api/actuator/health`.

Para parar sem apagar dados:

```bash
docker compose down
```

O banco atual usa o volume `postgres_data_v18`. `docker compose down -v` também remove volumes e pode apagar os dados definitivamente.

## Funcionalidades e regras

### Contas e autenticação

- A conta de cliente exige nome, e-mail, telefone com 11 dígitos, CPF válido e senha; o saldo inicial é zero.
- E-mail, telefone e CPF devem ser únicos.
- O banco gera o número da conta e calcula o dígito verificador por Módulo 11.
- O login aceita CPF ou referência da conta (`numero-dv`) e cria uma sessão `JSESSIONID`.
- A senha é armazenada como hash BCrypt.
- O CPF é cifrado com AES-GCM e possui hash SHA-256 separado para busca e unicidade.
- No painel administrativo, o CPF aparece como `123.***.***-**`.
- Contas bloqueadas ou encerradas não realizam movimentações; contas encerradas também não podem entrar.

### Recuperação de senha

- O usuário informa conta e e-mail e recebe um link com token aleatório.
- O token vale 30 minutos, pode ser usado uma vez e somente seu hash SHA-256 é persistido.
- No ambiente local, o link pode ser consultado no Mailpit.

### Operações financeiras

- **Depósito:** credita uma conta ativa e registra o novo saldo no extrato.
- **Pagamento:** substitui o saque e exige código de barras de 44 dígitos, valor, descrição e senha.
- O pagamento valida o saldo, produz a linha digitável de 47 dígitos e registra `PAGAMENTO`.
- **PIX interno:** funciona somente entre contas do Banco SRJM por chave cadastrada.
- Chaves disponíveis: e-mail, telefone, CPF e aleatória alfanumérica de 12 caracteres.
- Ao informar a chave, a API retorna o nome do destinatário antes da confirmação.
- Origem e destino precisam estar ativos; conta bloqueada ou encerrada não recebe PIX.
- O PIX valida senha e saldo, bloqueia ambas as contas e grava débito e crédito atomicamente com o mesmo `transferId`.
- Contas administradoras não executam operações financeiras.

### Extrato, encerramento e administração

- A visão geral mostra as três movimentações mais recentes.
- O extrato completo está disponível em JSON, texto, PDF, PNG e JPEG.
- Cada lançamento guarda tipo, crédito/débito, valor, saldo posterior, descrição, contraparte e data.
- Uma conta só pode ser encerrada quando estiver ativa, com saldo zerado e senha válida; seu histórico é preservado.
- A conta administradora exige nome, senha e `ADMINISTRATOR_CREATION_TOKEN` (`srjm` no ambiente local).
- O administrador consulta contas de clientes ativas, bloqueadas e encerradas, com filtros, dados cadastrais, CPF mascarado, saldos e total consolidado do banco.

## Modelo de dados

```text
clients
   `-- accounts
         |-- pix_keys
         |-- bank_transactions
         `-- password_reset_tokens
```

| Tabela | Conteúdo principal |
| --- | --- |
| `clients` | Nome, e-mail, telefone, CPF cifrado/hash e criação |
| `accounts` | Número, DV, senha BCrypt, perfil, status, saldo e encerramento |
| `pix_keys` | Tipo, valor único, conta e criação |
| `bank_transactions` | Tipo, direção, valor, saldo posterior, contraparte e data |
| `password_reset_tokens` | Hash, expiração e utilização do token |

Status: `ATIVA`, `BLOQUEADA` e `ENCERRADA`. Direções: `C` para crédito e `D` para débito. Tipos: `DEPOSITO`, `PAGAMENTO`, `PIX` e `SAQUE` apenas para compatibilidade histórica.

O Hibernate usa `ddl-auto: validate`; mudanças no esquema são feitas por novas migrações em `backend/src/main/resources/db/migration/`. Uma migração já aplicada nunca deve ser editada.

## API principal

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/accounts` | Abrir conta |
| `POST` | `/api/auth/login` | Entrar com CPF ou conta |
| `GET` | `/api/auth/me` | Consultar sessão atual |
| `DELETE` | `/api/auth/logout` | Encerrar sessão |
| `POST` | `/api/auth/forgot-password` | Solicitar recuperação |
| `POST` | `/api/auth/reset-password` | Definir nova senha |
| `GET/POST` | `/api/accounts/{conta}/pix-keys` | Listar ou criar chaves PIX |
| `GET` | `/api/accounts/admin/all` | Consultar contas como administrador |
| `PATCH` | `/api/accounts/{conta}/status` | Alterar status administrativamente |
| `POST` | `/api/accounts/{conta}/close` | Encerrar conta |
| `GET` | `/api/accounts/{conta}/statement` | Extrato JSON |
| `GET` | `/api/accounts/{conta}/statement/text` | Extrato em texto |
| `GET` | `/api/accounts/{conta}/statement/download` | Baixar PDF, PNG ou JPEG |
| `POST` | `/api/transactions/deposits` | Depositar |
| `POST` | `/api/transactions/payments` | Pagar boleto |
| `GET` | `/api/transactions/pix/recipient` | Identificar destinatário PIX |
| `POST` | `/api/transactions/pix` | Realizar PIX interno |

Consultas e movimentações de clientes são conferidas contra a conta autenticada na sessão.

## Organização do código

```text
backend/src/main/java/.../
  controller/  endpoints HTTP
  service/     regras bancárias, sessão, senha e documentos
  repository/  consultas JPA e bloqueios no PostgreSQL
  domain/      entidades e enums
  dto/         contratos JSON e validações
  mapper/      respostas seguras
  exception/   tratamento padronizado de erros
  util/        conta e dígito verificador

frontend/
  index.html             estrutura das telas
  assets/css/styles.css  layout e responsividade
  assets/js/app.js       eventos, máscaras e chamadas à API
  nginx.conf             frontend, proxy e stub de métricas
```

## Observabilidade preparada, mas desativada

O projeto mantém suporte a Micrometer, métricas Prometheus e tracing OpenTelemetry/OTLP. O Nginx possui `stub_status`, e há configurações para Nginx Exporter, PostgreSQL Exporter e OpenTelemetry Collector.

Os serviços de monitoração estão comentados no `docker-compose.yml` e não são iniciados. Para ativá-los futuramente, será necessário descomentar os serviços e iniciar o backend com o perfil Spring `monitoring`.

## Configuração e segurança

Variáveis principais: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DATA_ENCRYPTION_KEY`, `ADMINISTRATOR_CREATION_TOKEN`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` e `FRONTEND_URL`.

Os valores padrão servem apenas para desenvolvimento. Em produção, use segredos externos, HTTPS, limitação de tentativas, proteção CSRF, auditoria, sessões persistentes e imagens fixadas por versão ou digest.

## Testes

```bash
cd backend
mvn clean verify
```

Os testes cobrem regras centrais, como dígito verificador, encerramento, PIX para contas encerradas e documentos de extrato.
