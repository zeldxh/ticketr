# ticketr

Simple event ticket system. Web-based sales and management with role-based access, real-time availability, and concurrency-safe purchase handling.

## Description

ticketr provides:

- Two roles: normal user and administrator
- Real-time ticket availability
- Concurrency control to prevent overselling

## Stack

| Technology      | Version | Purpose                |
|-----------------|---------|------------------------|
| Java            | 21      | Runtime                |
| Spring Boot     | 3.x     | Backend                |
| Thymeleaf       | 3.1     | Server-side templates  |
| Bootstrap       | 5.3     | UI                     |
| MySQL           | 8.0+    | Database               |
| Spring Security | 6.x     | Auth and authorization |

## Architecture

```
browser  -->  spring boot (mvc)  -->  mysql
```

## Package Layout

```
src/main/java/com/example/sistemaboletos/
  config/       security and app configuration
  controller/   http handlers and navigation
  model/        entities (Usuario, Evento, Compra, Rol, EntidadBase)
  model/servicio/  service interfaces and implementations
  repository/   jpa data access
  SistemaBoletosApplication.java
```

## Features

**Auth**

- Login and registration
- Default admin account: `admin@admin.com` / `admin123`

**Events (admin only)**

- CRUD at `/admin/eventos`
- List: `GET /admin/eventos`
- Save: `POST /admin/eventos/guardar`
- Delete: `GET /admin/eventos/eliminar/{id}`

**Purchases**

- Concurrency-safe ticket purchase via `@Transactional` and `synchronized` in `EventoServiceImpl.comprarBoletos()`

## Run

**Prerequisites:** JDK 21+, MySQL 8.0+, Maven

1. Create the database:

   ```sql
   CREATE DATABASE sistema_boletos;
   ```

2. Copy the env file and set your credentials:

   ```sh
   cp .env.example .env
   ```

3. Start the app:

   ```bash
   mvn spring-boot:run
   ```

## Requirements Checklist

| Area            | Requirement         | How it is met |
|-----------------|---------------------|---------------|
| Architecture    | Client-server       | Spring Boot backend, Thymeleaf/Bootstrap frontend |
| Concurrency     | Thread safety       | `@Transactional` + `synchronized` in `EventoServiceImpl.comprarBoletos()` ([code](src/main/java/com/example/sistemaboletos/model/servicio/EventoServiceImpl.java)) |
| Security        | Authentication      | Spring Security; `/admin/**` restricted by role |
| Persistence     | Full CRUD           | JPA repositories for users, events, purchases, tickets |
| Validation      | Error handling      | Purchase validation and Spring Security error views (e.g. `login?error`) |
| Data structures | Generic collections | `List<T>`, `Optional<T>`, security context maps |
| OOP             | Abstract class      | `EntidadBase` for shared `id` |
| OOP             | Enum                | `Rol` (USER, ADMIN) |
| OOP             | Interface           | `IEventoService` implemented by `EventoServiceImpl` |

## License

MIT License, see [LICENSE](LICENSE).
