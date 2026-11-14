# Pruebas con curl — POST y GET

Servidor: `http://localhost:8081`
(Ejecutar en Git Bash. En PowerShell usar `curl.exe` en lugar de `curl`.)

## Usuarios

**1. Crear un maestro (POST)**
```bash
curl -i -X POST http://localhost:8081/api/usuarios -H "Content-Type: application/json" -d '{"nombre":"Juan Perez","email":"juan@uv.mx","password":"123456","rol":"MAESTRO"}'
```
Esperado: `201 Created` con el usuario creado (sin contraseña).

**2. Crear un alumno (POST)**
```bash
curl -i -X POST http://localhost:8081/api/usuarios -H "Content-Type: application/json" -d '{"nombre":"Ana Lopez","email":"ana@uv.mx","password":"123456","rol":"ALUMNO"}'
```

**3. Email repetido (POST)**
```bash
curl -i -X POST http://localhost:8081/api/usuarios -H "Content-Type: application/json" -d '{"nombre":"Otro","email":"juan@uv.mx","password":"123","rol":"ALUMNO"}'
```
Esperado: `409 Conflict`.

**4. Listar usuarios (GET)**
```bash
curl -i http://localhost:8081/api/usuarios
```

**5. Obtener un usuario (GET)**
```bash
curl -i http://localhost:8081/api/usuarios/1
```
Esperado: `200 OK`. Con un id que no existe (ej. `/999`): `404 Not Found`.

## Cursos

**6. Crear curso (POST)** — `idMaestro` debe ser un usuario con rol MAESTRO
```bash
curl -i -X POST http://localhost:8081/api/cursos -H "Content-Type: application/json" -d '{"nombre":"Desarrollo Web","descripcion":"Seccion 501","idMaestro":1}'
```
Esperado: `201 Created` con el `codigoInvitacion` generado.

**7. Crear curso con un alumno como maestro (POST)**
```bash
curl -i -X POST http://localhost:8081/api/cursos -H "Content-Type: application/json" -d '{"nombre":"Prueba","idMaestro":2}'
```
Esperado: `400 Bad Request`.

**8. Listar cursos (GET)**
```bash
curl -i http://localhost:8081/api/cursos
```

**9. Obtener un curso (GET)**
```bash
curl -i http://localhost:8081/api/cursos/1
```
