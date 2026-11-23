# Reporte de Avance del Proyecto

**Equipo:** 2
**Fecha:** 22/11/2026
**Número de reporte:** 5
**Repositorio GitHub:** https://github.com/nigth-dead/desarrollo-web.git

---

# 1. Actividades realizadas

Desde el reporte 4 se completó la Entregable 5, la versión candidata del sistema:

- Se probaron los casos de éxito y de error del backend con el script de pruebas
  en PowerShell (`pruebas-lms-con-token.ps1`, con token JWT) y con la batería de
  curl (`documentation/API_documentation/pruebas-api.sh`).
- Se corrigieron los defectos encontrados en esas pruebas.
- Se completó el README con las instrucciones para reproducir el proyecto en WSL.
- Se prepararon las evidencias: capturas del sistema funcionando, salida de las
  pruebas del backend y enlaces a los commits de cada integrante.
- Se verificó la ejecución en WSL siguiendo solo el README: clonar el repo, crear
  la base de datos, arrancar el backend y el frontend, y probar el flujo completo.

---

# 2. Evidencias de trabajo

| Actividad | Integrante responsable | Enlace a commit |
|------------|------------------------|-----------------|
| Completar README y preparar evidencias | Edgar Alejandro Cortes Martinez | https://github.com/nigth-dead/desarrollo-web/commit/aee1974 |
| Preparar evidencias | Edgar Alejandro Cortes Martinez | https://github.com/nigth-dead/desarrollo-web/commit/e6d7e92b4e63c4352dcd3e24389181cc33819fbc |
| Corregir defectos | Luis David Sosa Fernández | https://github.com/nigth-dead/desarrollo-web/commit/ac72f7162e3e8284d2f41ad89e40cf53c1b81ee8 |
| Verificar ejecución en WSL | Héctor Manuel Toral Huerta | [enlace al commit] |
| Probar casos de éxito y error | Kendar Emmanuel Strop Garcia | [enlace al commit] |

---

# 3. Problemas encontrados

- **El comando para crear la base no funciona igual en PowerShell:** en Windows el
  `mysql -u root -p < database/SCRIPT-BASE.sql` se corre en CMD, porque
  PowerShell no acepta `<`. Se aclaró en el README, con MySQL Workbench como
  alternativa.

---

# 4. Avance funcional

**Operativas:**
- Registro e inicio de sesión de usuarios, con login y autenticación por token
  (JWT, 8 horas).
- Creación de cursos e inscripción de alumnos (con código de invitación y por
  correo desde el maestro).
- Tareas individuales y por equipo, entregas (con fecha límite y prórrogas) y
  calificación de entregas.
- Foros y comentarios.
- Exámenes con preguntas de opción múltiple (calificación automática) y abiertas
  (calificación manual), con recálculo de la calificación final.
- Notificaciones.
- Formularios del frontend conectados a la API mediante funciones fetch.
- Tablas de cursos, tareas y usuarios en el frontend.
- Validación de respuestas HTTP.
- Validaciones y mensajes de error y confirmación en el frontend, sin cerrar los
  modales al corregir (por ejemplo, la prórroga con fecha de ayer muestra la
  advertencia en el mismo modal).
- Operaciones GET, POST, PUT, PATCH y DELETE de todos los recursos, verificadas
  con las pruebas de curl y con el script de pruebas en PowerShell.
- Reproducción completa del proyecto en WSL siguiendo solo el README.

**Pendientes:**
- PDF de documentacion final
- Grabacion de video explicativo
---

# 5. Uso de IA generativa

Documentar el uso de herramientas de inteligencia artificial durante el desarrollo del proyecto.


---No se uso IA generativa

# 6. Participación individual

- **Edgar Alejandro Cortes Martinez:** Completó el README con las instrucciones
  para reproducir el proyecto en WSL y preparó las evidencias de la entrega final.
- **Kendar Emmanuel Strop Garcia:** Probó los casos de éxito y error del sistema.
- **Héctor Manuel Toral Huerta:** Verificó la ejecución del proyecto en WSL.
- **Luis David Sosa Fernández:** Corrigió los defectos encontrados en las pruebas.

---

# 7. Próximas actividades

Indicar las tareas que se realizarán antes del siguiente reporte.

- Actividad 1 Edgar Alejandro Cortes Martinez: Preparar la defensa técnica
- Actividad 2 Luis David Sosa Fernández: Preparar la defensa técnica
- Actividad 3 Héctor Manuel Toral Huerta: Preparar la defensa técnica
- Actividad 4 Kendar Emmanuel Strop Garcia: Preparar la defensa técnica


---

## Observaciones adicionales

- 
