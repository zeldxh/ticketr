# ticketr

simple event ticket system. web-based sales and management with role-based access, real-time availability, and concurrency-safe purchase handling.

## description

ticketr provides:

- two roles: normal user and administrator
- real-time ticket availability
- concurrency control to prevent overselling

## stack

| technology       | version | purpose                    |
|------------------|---------|----------------------------|
| java             | 21      | runtime                    |
| spring boot      | 3.x     | backend                    |
| thymeleaf        | 3.1     | server-side templates      |
| bootstrap        | 5.3     | ui                         |
| mysql            | 8.0+    | database                   |
| spring security  | 6.x     | auth and authorization     |

## architecture

```
browser  -->  spring boot (mvc)  -->  mysql
```

## package layout

```
src/main/java/com/example/sistemaboletos/
  config/       security and app configuration
  controller/   http handlers and navigation
  model/        entities (Usuario, Evento, Compra, Rol, EntidadBase)
  model/servicio/  service interfaces and implementations
  repository/   jpa data access
  SistemaBoletosApplication.java
```

## features

**auth**

- login and registration
- default admin account: `admin@admin.com` / `admin123`

**events (admin only)**

- crud at `/admin/eventos`
- list: `GET /admin/eventos`
- save: `POST /admin/eventos/guardar`
- delete: `GET /admin/eventos/eliminar/{id}`

**purchases**

- concurrency-safe ticket purchase via `@Transactional` and `synchronized` in `EventoServiceImpl.comprarBoletos()`

## run

**prerequisites:** jdk 21+, mysql 8.0+, maven

1. create database:

   ```sql
   CREATE DATABASE sistema_boletos;
   ```

2. copy the env file and set your credentials:

   ```sh
   cp .env.example .env
   ```

3. start the app:

   ```bash
   mvn spring-boot:run
   ```

## requirements checklist

| area           | requirement           | how it is met |
|----------------|-----------------------|---------------|
| architecture   | client-server         | spring boot backend, thymeleaf/bootstrap frontend |
| concurrency    | thread safety         | `@Transactional` + `synchronized` in `EventoServiceImpl.comprarBoletos()` ([code](src/main/java/com/example/sistemaboletos/model/servicio/EventoServiceImpl.java)) |
| security       | authentication        | spring security; `/admin/**` restricted by role |
| persistence    | full crud             | jpa repositories for users, events, purchases, tickets |
| validation     | error handling        | purchase validation and spring security error views (e.g. `login?error`) |
| data structures| generic collections   | `List<T>`, `Optional<T>`, security context maps |
| oop            | abstract class        | `EntidadBase` for shared `id` |
| oop            | enum                  | `Rol` (USER, ADMIN) |
| oop            | interface              | `IEventoService` implemented by `EventoServiceImpl` |

## license

MIT License, see [LICENSE](LICENSE).
