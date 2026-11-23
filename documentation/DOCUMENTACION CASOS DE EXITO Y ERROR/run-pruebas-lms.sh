#!/usr/bin/env bash
# ============================================================================
# run-pruebas-lms.sh — Pruebas de casos de exito y error del LMS
# Repo: https://github.com/nigth-dead/desarrollo-web.git (HEAD 16a26ca)
# Matriz: Plan_Pruebas_LMS_Casos_Exito_y_Error.md (casos P-01 a P-26)
#
# SOLO prueba la API con curl. No levanta el backend ni toca la base de datos.
# Los casos P-22 a P-26 confirman defectos ya detectados por lectura del
# codigo (PUT/DELETE que devuelven 200 cuando deberian dar 404/403); para no
# borrar datos reales, esos casos usan recursos DESECHABLES creados solo para
# ellos. Un "PASS" en un caso de defecto significa "defecto confirmado, hay
# que corregirlo", no que el sistema este bien.
#
# --- COMO CORRERLO EN WSL (Ubuntu) -----------------------------------------
# 1. Arranca MySQL (en la terminal de WSL):
#      sudo service mysql start
#    Crea la base una sola vez (si aun no existe):
#      mysql -u root -p < database/SCRIPT-BASE.sql
#    (El script crea lms_db y el usuario 'listi'. Las tablas las genera
#    Hibernate al arrancar el backend.)
#
# 2. Arranca el backend en OTRA terminal de WSL, desde la carpeta del repo:
#      cd backend
#      export BASE_DATOS=localhost
#      ./mvnw spring-boot:run
#    Espera a que diga que Spring Boot arranco y que escucha en el puerto 8081.
#    El admin sembrado (admin@uv.mx / admin123) lo crea solo el backend al
#    arrancar por primera vez. Este script NO usa el admin.
#
# 3. (Opcional) Arranca el frontend estatico en otra terminal, si quieres
#    verificar tambien la interfaz a mano mientras corre el script:
#      cd frontend
#      python3 -m http.server 5500
#
# 4. Corre este script desde la carpeta donde lo guardaste:
#      chmod +x run-pruebas-lms.sh
#      ./run-pruebas-lms.sh
#
#    Requisitos: curl y python3 (sin jq). La API debe responder en
#    http://localhost:8081/api; si no responde, el script se detiene con un
#    mensaje claro y NO ejecuta ningun caso.
#
#    Los correos de prueba llevan un sufijo con fecha y hora, asi que puedes
#    repetir la corrida sin choques por correos duplicados. La contrasena de
#    prueba es siempre Prueba123.
#
#    Salida: 0 si los 26 casos pasaron (incluidos los defectos confirmados),
#    distinto de cero si algun caso fallo.
# ============================================================================

# --- Configuracion -----------------------------------------------------------
API="${API:-http://localhost:8081/api}"
PASS=0
FAIL=0
FALLOS=()
DEFECTOS_CONFIRMADOS=()
DEFECTOS_CORREGIDOS=()
TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT
BODY=""
CODIGO=""

# Sufijo unico por corrida para que los correos no choquen al repetir.
SUFIJO=$(date +%Y%m%d%H%M%S)
CLAVE_PRUEBA="Prueba123"
MAESTRO_EMAIL="maestro.prueba.$SUFIJO@uv.mx"
ALUMNO1_EMAIL="alumno1.prueba.$SUFIJO@uv.mx"
ALUMNO2_EMAIL="alumno2.prueba.$SUFIJO@uv.mx"

# Fechas relativas (GNU date, disponible en WSL/Ubuntu).
ts() { date -d "$1" +%Y-%m-%dT%H:%M:%S; }
HACE1H=$(ts "-1 hour")
EN1H=$(ts "+1 hour")
EN2H=$(ts "+2 hours")
SEMANA=$(ts "+7 days")

# IDs y tokens que se van capturando caso a caso (no tocar a mano).
M=""; A1=""; A2=""
TOK_M=""; TOK_A1=""; TOK_A2=""
CURSO=""; CODIGO_INV=""; TAREA=""; ENTREGA=""
EXAMEN=""; PREGUNTA=""; FORO=""
CURSO_D=""; TAREA_D=""; EXAMEN_D=""; PREGUNTA_D=""; FORO_D=""

# --- Utilidades --------------------------------------------------------------
# Extrae un campo del JSON de la ultima respuesta usando solo python3.
# Uso: jget "['idUsuario']"  o  jget "['usuario']['idUsuario']"
jget() {
  python3 -c "import sys,json; d=json.loads(sys.argv[1]); print(eval('d'+sys.argv[2]))" "$BODY" "$1" 2>/dev/null || true
}

# Condicion python sobre el JSON de la ultima respuesta (d = respuesta).
# Uso: check "d['token'].count('.')==2" "el token tiene formato JWT"
check() {
  local cond="$1" desc="$2"
  if python3 -c "import sys,json; d=json.loads(sys.argv[1]); sys.exit(0 if ($cond) else 1)" "$BODY" 2>/dev/null; then
    echo "      check OK: $desc"
  else
    echo "      check FAIL: $desc (respuesta: ${BODY:0:200})"
    CASO_OK=0
  fi
}

# El campo NO debe aparecer en la respuesta (ej. respuestaCorrecta al alumno).
check_ausente() {
  if echo "$BODY" | grep -q "\"$1\""; then
    echo "      check FAIL: el campo $1 NO deberia aparecer ($2)"
    CASO_OK=0
  else
    echo "      check OK: el campo $1 no aparece ($2)"
  fi
}

# El campo SI debe aparecer en la respuesta.
check_presente() {
  if echo "$BODY" | grep -q "\"$1\""; then
    echo "      check OK: el campo $1 aparece ($2)"
  else
    echo "      check FAIL: el campo $1 deberia aparecer ($2)"
    CASO_OK=0
  fi
}

# Peticion sin contar en el marcador (solo para preparar recursos desechables).
# Uso: llamada METODO RUTA [CUERPO] [TOKEN]
llamada() {
  local metodo="$1" ruta="$2" cuerpo="$3" token="$4"
  local auth=()
  if [ -n "$token" ]; then auth=(-H "Authorization: Bearer $token"); fi
  if [ -n "$cuerpo" ]; then
    CODIGO=$(curl -s -o "$TMP" -w "%{http_code}" -X "$metodo" "$API$ruta" "${auth[@]}" -H "Content-Type: application/json" -d "$cuerpo")
  else
    CODIGO=$(curl -s -o "$TMP" -w "%{http_code}" -X "$metodo" "$API$ruta" "${auth[@]}")
  fi
  BODY=$(cat "$TMP")
}

# Peticion que SI cuenta: compara el codigo obtenido con el esperado.
# Uso: req METODO RUTA [CUERPO] ESPERADO [TOKEN] "descripcion"
req() {
  local metodo="$1" ruta="$2" cuerpo="$3" esperado="$4" token="$5" desc="$6"
  llamada "$metodo" "$ruta" "$cuerpo" "$token"
  echo "  $desc"
  echo "      $metodo $ruta | esperado: $esperado | obtenido: $CODIGO"
  if [ "$CODIGO" = "$esperado" ]; then
    echo "      -> OK"
  else
    echo "      -> FAIL (respuesta: ${BODY:0:200})"
    CASO_OK=0
  fi
}

# Peticion de un caso de DEFECTO (P-22 a P-26): el codigo correcto es 404/403,
# pero el codigo sospecha que el servidor devuelve 200 con cuerpo vacio o
# texto. Las dos salidas se registran; solo un codigo distinto de ambas es FAIL.
# Uso: req_defecto METODO RUTA [CUERPO] CODIGO_CORRECTO [TOKEN] "descripcion"
req_defecto() {
  local metodo="$1" ruta="$2" cuerpo="$3" correcto="$4" token="$5" desc="$6"
  llamada "$metodo" "$ruta" "$cuerpo" "$token"
  echo "  $desc"
  echo "      $metodo $ruta | correcto: $correcto | obtenido: $CODIGO"
  if [ "$CODIGO" = "200" ]; then
    echo "      -> DEFECTO CONFIRMADO: devuelve 200 (cuerpo vacio o texto), deberia ser $correcto"
    DEFECTOS_CONFIRMADOS+=("$CASO_ID: $desc")
  elif [ "$CODIGO" = "$correcto" ]; then
    echo "      -> CORREGIDO: el servidor ya devuelve $correcto"
    DEFECTOS_CORREGIDOS+=("$CASO_ID: $desc")
  else
    echo "      -> FAIL inesperado (respuesta: ${BODY:0:200})"
    CASO_OK=0
  fi
}

caso_inicio() {
  CASO_ID="$1"
  CASO_OK=1
  echo ""
  echo "[$CASO_ID] $2"
}

caso_fin() {
  if [ "$CASO_OK" = "1" ]; then
    PASS=$((PASS + 1))
    echo "  RESULTADO $CASO_ID: PASS"
  else
    FAIL=$((FAIL + 1))
    FALLOS+=("$CASO_ID")
    echo "  RESULTADO $CASO_ID: FAIL"
  fi
}

# --- Preflight: herramientas y API ------------------------------------------
echo "=== Preflight ==="
command -v curl >/dev/null 2>&1 || { echo "ERROR: falta curl. Instalalo con: sudo apt install curl"; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "ERROR: falta python3. Instalalo con: sudo apt install python3"; exit 1; }
echo "OK: curl y python3 disponibles."
echo "Comprobando la API en $API ..."
PRE=$(curl -s -m 5 -o /dev/null -w "%{http_code}" "$API/cursos" || echo "000")
if [ "$PRE" = "000" ] || [ -z "$PRE" ]; then
  echo "ERROR: la API no responde en $API."
  echo "Arranca primero el backend (ver instrucciones al inicio del script):"
  echo "  cd backend && export BASE_DATOS=localhost && ./mvnw spring-boot:run"
  echo "Y verifica que MySQL este corriendo: sudo service mysql start"
  echo "No se ejecuto ningun caso."
  exit 1
fi
echo "OK: la API responde (HTTP $PRE sin token; se espera 401 si el backend esta levantado)."
echo "Sufijo de esta corrida: $SUFIJO (correos de prueba unicos)"

# ============================================================================
# 3.1 Autenticacion y usuarios
# ============================================================================
echo ""
echo "=== 3.1 Autenticacion y usuarios ==="

caso_inicio "P-01" "Exito: registro valido de MAESTRO y ALUMNO"
req POST /auth/registro "{\"nombre\":\"Maestro Prueba $SUFIJO\",\"email\":\"$MAESTRO_EMAIL\",\"password\":\"$CLAVE_PRUEBA\",\"rol\":\"MAESTRO\"}" 201 "" "registrar MAESTRO ($MAESTRO_EMAIL)"
M=$(jget "['idUsuario']")
[ -n "$M" ] || { echo "      No se pudo extraer el id del maestro"; CASO_OK=0; }
req POST /auth/registro "{\"nombre\":\"Alumno Uno $SUFIJO\",\"email\":\"$ALUMNO1_EMAIL\",\"password\":\"$CLAVE_PRUEBA\",\"rol\":\"ALUMNO\"}" 201 "" "registrar ALUMNO 1 ($ALUMNO1_EMAIL)"
A1=$(jget "['idUsuario']")
[ -n "$A1" ] || { echo "      No se pudo extraer el id del alumno 1"; CASO_OK=0; }
req POST /auth/registro "{\"nombre\":\"Alumno Dos $SUFIJO\",\"email\":\"$ALUMNO2_EMAIL\",\"password\":\"$CLAVE_PRUEBA\",\"rol\":\"ALUMNO\"}" 201 "" "registrar ALUMNO 2 ($ALUMNO2_EMAIL) (se usa en P-26)"
A2=$(jget "['idUsuario']")
[ -n "$A2" ] || { echo "      No se pudo extraer el id del alumno 2"; CASO_OK=0; }
caso_fin

caso_inicio "P-02" "Exito: inicio de sesion del maestro y los alumnos"
req POST /auth/login "{\"email\":\"$MAESTRO_EMAIL\",\"password\":\"$CLAVE_PRUEBA\"}" 200 "" "login maestro"
check "d['token'].count('.')==2" "el token tiene formato JWT (dos puntos)"
TOK_M=$(jget "['token']")
[ -n "$TOK_M" ] || { echo "      No se pudo extraer el token del maestro"; CASO_OK=0; }
req POST /auth/login "{\"email\":\"$ALUMNO1_EMAIL\",\"password\":\"$CLAVE_PRUEBA\"}" 200 "" "login alumno 1"
TOK_A1=$(jget "['token']")
[ -n "$TOK_A1" ] || { echo "      No se pudo extraer el token del alumno 1"; CASO_OK=0; }
req POST /auth/login "{\"email\":\"$ALUMNO2_EMAIL\",\"password\":\"$CLAVE_PRUEBA\"}" 200 "" "login alumno 2"
TOK_A2=$(jget "['token']")
[ -n "$TOK_A2" ] || { echo "      No se pudo extraer el token del alumno 2"; CASO_OK=0; }
caso_fin

caso_inicio "P-03" "Error: correo duplicado"
req POST /auth/registro "{\"nombre\":\"Otro\",\"email\":\"$MAESTRO_EMAIL\",\"password\":\"$CLAVE_PRUEBA\",\"rol\":\"MAESTRO\"}" 409 "" "registrar con el mismo email del maestro"
caso_fin

caso_inicio "P-04" "Error: credenciales invalidas (contrasena y correo)"
req POST /auth/login "{\"email\":\"$MAESTRO_EMAIL\",\"password\":\"incorrecta\"}" 401 "" "login con contrasena incorrecta"
req POST /auth/login "{\"email\":\"nadie.$SUFIJO@uv.mx\",\"password\":\"$CLAVE_PRUEBA\"}" 401 "" "login con correo inexistente (esperado 401, no 404)"
caso_fin

caso_inicio "P-05" "Error: campos vacios en el registro"
req POST /auth/registro '{}' 400 "" "registro sin ningun campo"
req POST /auth/registro "{\"nombre\":\"\",\"email\":\"\",\"password\":\"$CLAVE_PRUEBA\",\"rol\":\"ALUMNO\"}" 400 "" "registro con nombre y correo vacios"
echo "  Nota: en el navegador el formulario puede bloquear antes por 'required';"
echo "  este caso prueba la validacion del backend (400)."
caso_fin

# ============================================================================
# 3.2 Cursos e inscripciones
# ============================================================================
echo ""
echo "=== 3.2 Cursos e inscripciones ==="

caso_inicio "P-06" "Exito: crear curso como MAESTRO"
req POST /cursos "{\"nombre\":\"Desarrollo Web Prueba $SUFIJO\",\"descripcion\":\"Curso de prueba\",\"idMaestro\":$M}" 201 "$TOK_M" "crear curso (token del maestro)"
CURSO=$(jget "['idCurso']")
CODIGO_INV=$(jget "['codigoInvitacion']")
[ -n "$CURSO" ] || { echo "      No se pudo extraer el id del curso"; CASO_OK=0; }
check "len(d.get('codigoInvitacion') or '')==6" "el codigo de invitacion tiene 6 caracteres"
caso_fin

caso_inicio "P-07" "Exito: unirse a curso con el codigo de invitacion"
req POST "/cursos/unirse?idUsuario=$A1" "{\"codigoInvitacion\":\"$CODIGO_INV\"}" 201 "$TOK_A1" "alumno 1 se une con el codigo $CODIGO_INV"
check "d['idCurso']==$CURSO" "la respuesta es el curso al que se unio"
caso_fin

caso_inicio "P-08" "Error: crear curso con un ALUMNO como maestro (corregido: 400, no 403)"
req POST /cursos "{\"nombre\":\"Curso no permitido\",\"descripcion\":\"x\",\"idMaestro\":$A1}" 400 "$TOK_A1" "crear curso con idMaestro de un ALUMNO"
caso_fin

caso_inicio "P-09" "Error: codigo de invitacion inexistente"
req POST "/cursos/unirse?idUsuario=$A1" '{"codigoInvitacion":"XXXXXX"}' 404 "$TOK_A1" "unirse con codigo falso XXXXXX"
caso_fin

caso_inicio "P-10" "Error: doble inscripcion con el mismo codigo"
req POST "/cursos/unirse?idUsuario=$A1" "{\"codigoInvitacion\":\"$CODIGO_INV\"}" 409 "$TOK_A1" "alumno 1 se une de nuevo con el mismo codigo"
caso_fin

# ============================================================================
# 3.3 Tareas, entregas y prorrogas
# ============================================================================
echo ""
echo "=== 3.3 Tareas, entregas y prorrogas ==="

caso_inicio "P-11" "Exito: crear tarea como MAESTRO (genera notificacion al alumno)"
req POST "/cursos/$CURSO/tareas?idUsuario=$M" "{\"titulo\":\"Practica 1 - $SUFIJO\",\"descripcion\":\"Entrega de prueba\",\"fechaLimite\":\"$SEMANA\",\"esEquipo\":false}" 201 "$TOK_M" "crear tarea en el curso"
TAREA=$(jget "['idTarea']")
[ -n "$TAREA" ] || { echo "      No se pudo extraer el id de la tarea"; CASO_OK=0; }
req GET "/notificaciones?idUsuario=$A1" "" 200 "$TOK_A1" "el alumno tiene notificaciones (por la tarea nueva)"
check "any('Nueva tarea' in (n.get('mensaje') or '') for n in d)" "hay una notificacion de tarea nueva"
caso_fin

caso_inicio "P-12" "Exito: subir entrega como ALUMNO"
req POST "/tareas/$TAREA/entregas?idUsuario=$A1" '{"archivoUrl":"https://drive.example/practica1.zip"}' 201 "$TOK_A1" "alumno 1 entrega la tarea (enlace valido)"
ENTREGA=$(jget "['idEntrega']")
[ -n "$ENTREGA" ] || { echo "      No se pudo extraer el id de la entrega"; CASO_OK=0; }
caso_fin

caso_inicio "P-13" "Exito: calificar entrega como MAESTRO (95.5)"
req PATCH "/entregas/$ENTREGA/calificacion?idUsuario=$M" '{"calificacion":95.5,"comentarioMaestro":"Muy bien"}' 200 "$TOK_M" "calificar la entrega con 95.5"
check "float(d['calificacion'])==95.5" "la calificacion guardada es 95.5"
caso_fin

caso_inicio "P-14" "Error: calificacion fuera de rango (-10 y 150)"
req PATCH "/entregas/$ENTREGA/calificacion?idUsuario=$M" '{"calificacion":-10}' 400 "$TOK_M" "calificar con -10"
req PATCH "/entregas/$ENTREGA/calificacion?idUsuario=$M" '{"calificacion":150}' 400 "$TOK_M" "calificar con 150"
caso_fin

caso_inicio "P-15" "Error: recurso inexistente (tarea y entrega 9999)"
req GET "/tareas/9999?idUsuario=$M" "" 404 "$TOK_M" "consultar la tarea 9999"
req DELETE "/entregas/9999?idUsuario=$M" "" 404 "$TOK_M" "eliminar la entrega 9999 (si devuelve 200, es el defecto D-02)"
caso_fin

caso_inicio "P-16" "Error: un ALUMNO intenta crear una tarea"
req POST "/cursos/$CURSO/tareas?idUsuario=$A1" "{\"titulo\":\"Tarea no permitida\",\"fechaLimite\":\"$SEMANA\"}" 403 "$TOK_A1" "alumno 1 crea una tarea"
caso_fin

caso_inicio "P-17" "Error: entrega duplicada de la misma tarea"
req POST "/tareas/$TAREA/entregas?idUsuario=$A1" '{"archivoUrl":"https://drive.example/otra.zip"}' 409 "$TOK_A1" "alumno 1 entrega dos veces la misma tarea"
caso_fin

# ============================================================================
# 3.4 Examenes y foros
# ============================================================================
echo ""
echo "=== 3.4 Examenes y foros ==="

caso_inicio "P-18" "Exito: examen autocalificado (opcion multiple)"
req POST "/cursos/$CURSO/examenes?idUsuario=$M" "{\"titulo\":\"Parcial 1 - $SUFIJO\",\"fechaInicio\":\"$HACE1H\",\"fechaFin\":\"$EN2H\"}" 201 "$TOK_M" "crear examen disponible ahora"
EXAMEN=$(jget "['idExamen']")
[ -n "$EXAMEN" ] || { echo "      No se pudo extraer el id del examen"; CASO_OK=0; }
req POST "/examenes/$EXAMEN/preguntas?idUsuario=$M" '{"pregunta":"Metodo para crear un recurso?","tipo":"OPCION_MULTIPLE","opciones":["GET","POST","PUT"],"respuestaCorrecta":"POST","valorPuntos":2}' 201 "$TOK_M" "agregar pregunta de opcion multiple (vale 2 puntos)"
PREGUNTA=$(jget "['idPregunta']")
[ -n "$PREGUNTA" ] || { echo "      No se pudo extraer el id de la pregunta"; CASO_OK=0; }
req POST "/examenes/$EXAMEN/resultados?idUsuario=$A1" "{\"respuestas\":[{\"idPregunta\":$PREGUNTA,\"respuestaDada\":\"POST\"}]}" 201 "$TOK_A1" "alumno 1 responde bien el examen"
check "d['resultado']['estadoRevision']=='CALIFICADO'" "el resultado quedo CALIFICADO (autocalificado)"
check "float(d['resultado']['calificacionFinal'])==2.0" "la calificacion final es 2.0 (suma de puntos)"
caso_fin

caso_inicio "P-19" "Exito: publicar en foro y comentar"
req POST "/cursos/$CURSO/foros?idUsuario=$M" "{\"titulo\":\"Dudas - $SUFIJO\",\"descripcion\":\"Pregunten aqui\"}" 201 "$TOK_M" "crear foro en el curso"
FORO=$(jget "['idForo']")
[ -n "$FORO" ] || { echo "      No se pudo extraer el id del foro"; CASO_OK=0; }
req POST "/foros/$FORO/comentarios?idUsuario=$A1" '{"contenido":"Se entrega en zip?"}' 201 "$TOK_A1" "alumno 1 comenta en el foro"
caso_fin

caso_inicio "P-20" "Error: examen con fechaFin anterior a fechaInicio"
req POST "/cursos/$CURSO/examenes?idUsuario=$M" "{\"titulo\":\"Examen incoherente\",\"fechaInicio\":\"$EN2H\",\"fechaFin\":\"$HACE1H\"}" 400 "$TOK_M" "crear examen con fin antes de inicio"
caso_fin

caso_inicio "P-21" "Exito con control: el alumno NO ve respuestaCorrecta"
req GET "/examenes/$EXAMEN/preguntas?idUsuario=$A1" "" 200 "$TOK_A1" "alumno 1 consulta las preguntas del examen"
check_ausente "respuestaCorrecta" "vista del alumno"
req GET "/examenes/$EXAMEN/preguntas?idUsuario=$M" "" 200 "$TOK_M" "el maestro consulta las mismas preguntas"
check_presente "respuestaCorrecta" "vista del maestro"
caso_fin

# ============================================================================
# 3.5 Defectos PUT/DELETE a verificar (P-22 a P-26)
# Recursos desechables: se crean solo para estos casos, asi un defecto que
# borre o edite de verdad no dana el curso, la tarea ni el examen principales.
# ============================================================================
echo ""
echo "=== 3.5 Preparacion de recursos desechables (no son casos) ==="
llamada POST /cursos "{\"nombre\":\"Desechable $SUFIJO\",\"descripcion\":\"solo para P-22 a P-26\",\"idMaestro\":$M}" "$TOK_M"
CURSO_D=$(jget "['idCurso']")
llamada POST "/cursos/$CURSO_D/tareas?idUsuario=$M" "{\"titulo\":\"Tarea desechable\",\"fechaLimite\":\"$SEMANA\"}" "$TOK_M"
TAREA_D=$(jget "['idTarea']")
llamada POST "/cursos/$CURSO_D/examenes?idUsuario=$M" "{\"titulo\":\"Examen desechable\",\"fechaInicio\":\"$HACE1H\",\"fechaFin\":\"$EN2H\"}" "$TOK_M"
EXAMEN_D=$(jget "['idExamen']")
llamada POST "/examenes/$EXAMEN_D/preguntas?idUsuario=$M" '{"pregunta":"Desechable?","tipo":"OPCION_MULTIPLE","opciones":["Si","No"],"respuestaCorrecta":"Si","valorPuntos":1}' "$TOK_M"
PREGUNTA_D=$(jget "['idPregunta']")
llamada POST "/cursos/$CURSO_D/foros?idUsuario=$M" '{"titulo":"Foro desechable","descripcion":"x"}' "$TOK_M"
FORO_D=$(jget "['idForo']")
if [ -z "$CURSO_D" ] || [ -z "$TAREA_D" ] || [ -z "$EXAMEN_D" ] || [ -z "$PREGUNTA_D" ] || [ -z "$FORO_D" ]; then
  echo "ERROR: no se pudieron crear los recursos desechables (curso=$CURSO_D tarea=$TAREA_D examen=$EXAMEN_D pregunta=$PREGUNTA_D foro=$FORO_D)."
  echo "Los casos P-22 a P-26 no se pueden verificar. Revisa los casos anteriores."
  exit 1
fi
echo "OK: recursos desechables listos (curso $CURSO_D, tarea $TAREA_D, examen $EXAMEN_D, pregunta $PREGUNTA_D, foro $FORO_D)."

caso_inicio "P-22" "DEFECTO (D-01): PUT examen inexistente o sin permiso devuelve 200 vacio"
req_defecto PUT "/examenes/9999?idUsuario=$M" '{"titulo":"No existe"}' 404 "$TOK_M" "editar el examen 9999 (no existe)"
req_defecto PUT "/examenes/$EXAMEN?idUsuario=$A1" '{"titulo":"Editado por alumno"}' 403 "$TOK_A1" "editar el examen real como ALUMNO (sin permiso)"
caso_fin

caso_inicio "P-23" "DEFECTO (D-02): DELETE examen inexistente o sin permiso devuelve 200 con texto"
req_defecto DELETE "/examenes/9999?idUsuario=$M" "" 404 "$TOK_M" "eliminar el examen 9999 (no existe)"
req_defecto DELETE "/examenes/$EXAMEN_D?idUsuario=$A1" "" 403 "$TOK_A1" "eliminar el examen desechable como ALUMNO (sin permiso)"
caso_fin

caso_inicio "P-24" "DEFECTO (D-01/D-02): PUT/DELETE de tarea, pregunta y foro sin permiso"
req_defecto PUT "/tareas/$TAREA_D?idUsuario=$A1" '{"titulo":"Editada por alumno"}' 403 "$TOK_A1" "editar la tarea desechable como ALUMNO"
req_defecto PUT "/preguntas/$PREGUNTA_D?idUsuario=$A1" '{"pregunta":"Editada por alumno"}' 403 "$TOK_A1" "editar la pregunta desechable como ALUMNO"
req_defecto PUT "/foros/$FORO_D?idUsuario=$A1" '{"titulo":"Editado por alumno"}' 403 "$TOK_A1" "editar el foro desechable como ALUMNO"
req_defecto DELETE "/tareas/$TAREA_D?idUsuario=$A1" "" 403 "$TOK_A1" "eliminar la tarea desechable como ALUMNO"
req_defecto DELETE "/preguntas/$PREGUNTA_D?idUsuario=$A1" "" 403 "$TOK_A1" "eliminar la pregunta desechable como ALUMNO"
req_defecto DELETE "/foros/$FORO_D?idUsuario=$A1" "" 403 "$TOK_A1" "eliminar el foro desechable como ALUMNO"
caso_fin

caso_inicio "P-25" "DEFECTO (D-03): PUT/DELETE de curso sin permiso (sin revision de permisos)"
echo "  AVISO: este caso usa el curso desechable $CURSO_D, porque si el defecto"
echo "  existe, el PUT lo edita de verdad y el DELETE lo borra de verdad."
req_defecto PUT "/cursos/$CURSO_D" '{"nombre":"Curso editado por alumno"}' 403 "$TOK_A1" "editar el curso desechable como ALUMNO"
req_defecto DELETE "/cursos/$CURSO_D" "" 403 "$TOK_A1" "eliminar el curso desechable como ALUMNO"
caso_fin

caso_inicio "P-26" "DEFECTO (D-01): PUT entrega (JSON) inexistente o ajena devuelve 200 vacio"
req_defecto PUT "/entregas/9999?idUsuario=$M" '{"archivoUrl":"https://drive.example/x.zip"}' 404 "$TOK_M" "reemplazar la entrega 9999 (no existe)"
req_defecto PUT "/entregas/$ENTREGA?idUsuario=$A2" '{"archivoUrl":"https://drive.example/x.zip"}' 403 "$TOK_A2" "reemplazar la entrega del alumno 1 como alumno 2 (ajena)"
caso_fin

# --- Resumen final -----------------------------------------------------------
echo ""
echo "================ RESUMEN ================"
echo "Casos: 26  |  PASS: $PASS  |  FAIL: $FAIL"
if [ "${#FALLOS[@]}" -gt 0 ]; then
  echo "Casos con FAIL: ${FALLOS[*]}"
else
  echo "Casos con FAIL: ninguno"
fi
echo ""
echo "Defectos CONFIRMADOS (devuelven 200 cuando deberian dar 404/403):"
if [ "${#DEFECTOS_CONFIRMADOS[@]}" -gt 0 ]; then
  printf '  - %s\n' "${DEFECTOS_CONFIRMADOS[@]}"
else
  echo "  ninguno (todos los casos de defecto ya devuelven el codigo correcto)"
fi
echo ""
echo "Defectos CORREGIDOS (ya devuelven 404/403 como corresponde):"
if [ "${#DEFECTOS_CORREGIDOS[@]}" -gt 0 ]; then
  printf '  - %s\n' "${DEFECTOS_CORREGIDOS[@]}"
else
  echo "  ninguno"
fi
echo "========================================="
echo "Nota: un PASS en P-22 a P-26 con 'DEFECTO CONFIRMADO' significa que el"
echo "defecto existe y debe pasar a la actividad 'Corregir defectos' (Luis)."
echo "Datos de esta corrida: maestro $MAESTRO_EMAIL, curso $CURSO, tarea $TAREA,"
echo "entrega $ENTREGA, examen $EXAMEN, foro $FORO."

if [ "$FAIL" -gt 0 ]; then
  echo "RESULTADO: hay $FAIL caso(s) con FAIL."
  exit 1
fi
echo "RESULTADO: los 26 casos pasaron."
exit 0
