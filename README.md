# desarrollo-web

Sistema LMS (Learn Management System),
con backend en Spring Boot, base de datos MySQL y frontend en HTML/CSS/JavaScript
que consume la API con fetch.

## Requisitos

- WSL con Ubuntu.
- Java 17 o superior.
- Maven.
- MySQL.
- Git.
- Un servidor estático para el frontend (por ejemplo, la extensión Live Server de VS Code, o Python 3 con `python3 -m http.server`).

## 1. Clonar el repositorio

```bash
git clone https://github.com/nigth-dead/desarrollo-web.git
cd desarrollo-web
```

## 2. Crear la base de datos

En MySQL, ejecutar el script [script de la base de datos](database/SCRIPT-BASE.sql), que crea la base `lms_db` y el usuario de la aplicación con permisos de acceso remoto y local:

```bash
mysql -u root -p < database/SCRIPT-BASE.sql
```

Las tablas se creano o actualizan solas al arrancar el backend (`spring.jpa.hibernate.ddl-auto=update`).

## 3. Configurar el backend

El backend lee el host de MySQL de la variable de entorno `BASE_DATOS`.
En WSL, con MySQL en la misma máquina:

```bash
export BASE_DATOS=localhost
```

El secreto JWT es opcional: si no se define `JWT_SECRETO`, el backend usa su valor
de desarrollo. Para una prueba tal cual, no hace falta definirlo.

## 4. Arrancar el backend

Desde la terminal de Windows, ver la IP de WiFi:

```bash
ipconfig
```

Copiar la dirección IPv4 del adaptador de WiFi.

Luego, en la terminal de WSL, desde la carpeta `backend/`:

```bash
cd backend
export BASE_DATOS='192.168.1.XX'
./mvnw spring-boot:run
```

Reemplazar `192.168.1.XX` por la IP obtenida con `ipconfig`.

El backend queda en `http://localhost:8081` y la API en `http://localhost:8081/api`.

## 5. Arrancar el frontend

El frontend es estático y apunta a `http://localhost:8081/api`. 

Se puede iniciar el frontend usando Live Server de VS Code o desde la carpeta `frontend/`, con Python 3:

```bash
cd ../frontend
python3 -m http.server 5500
```

Y abrir `http://localhost:5500/index.html`. 


## 6. Probar el flujo

### 6.1 Primero: entrar como administrador

Al arrancar el backend por primera vez se crea solo el usuario administrador:

- Correo: `admin@uv.mx`
- Contraseña: `admin123`
- Rol: `ADMIN`

Iniciar sesión con ese usuario en `index.html`. El login guarda el token y lo envía en las peticiones a la API.

### 6.2 Crear los usuarios de prueba

Desde el formulario de registro (`registro.html`), crear un usuario con rol `MAESTRO` y otro con rol `ALUMNO`, eligiendo el rol en el propio formulario. Si las contraseñas no coinciden, muestra el aviso; al registrarse bien, muestra "Usuario registrado exitosamente." y redirige a `usuarios.html`, donde el administrador puede ver, editar y eliminar usuarios.

### 6.3 Casos de uso para probar

**Como administrador:**
- Ver la lista de usuarios, editar sus datos y su rol, y eliminar usuarios en `usuarios.html`.

**Como maestro:**
- Iniciar sesión con el usuario `MAESTRO`.
- Crear un curso y una tarea, e inscribir al alumno.
- Crear un examen.
- Calificar una entrega.
- Registrar una prórroga con fecha de ayer: muestra la advertencia en el mismo modal y permite corregirla sin cerrarlo.

**Como alumno:**
- Iniciar sesión con el usuario `ALUMNO`.
- Inscribirse en un curso y ver sus tareas.
- Entregar una tarea.
- Responder un examen y ver su calificación.

## Estructura del proyecto

```
backend/        Spring Boot (Java 17), API REST en el puerto 8081
database/       SCRIPT-BASE.sql (crea lms_db y el usuario)
frontend/       HTML, CSS y JS que consumen la API con fetch
documentation/  Documentación de la API, pruebas y reportes de actividad
```

## Notas

- Las pruebas de la API con curl están en `documentation/API_documentation/pruebas-api.sh`.
- La documentación de uso de IA generativa está en `documentation/IAG/`.
