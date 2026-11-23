#!/usr/bin/env bash
# Pruebas de la API contra http://localhost:8081/api (correr con la base vacia)
# Cada peticion manda el token del usuario que aparece en ?idUsuario= ; si no hay, el del maestro.
API=http://localhost:8081/api
PASS=0; FAIL=0; BODY=""
TMP=$(mktemp)
declare -A TOK   # idUsuario -> token
DEF=""           # token por defecto (maestro)
TOKEN=""         # si se define, se usa este token en la siguiente peticion ("-" = sin token)

req() { # metodo ruta body esperado descripcion
  local m=$1 u=$2 b=$3 exp=$4 d=$5 code id t
  id=$(echo "$u" | grep -o 'idUsuario=[0-9]*' | cut -d= -f2)
  t="$DEF"; [ -n "$id" ] && [ -n "${TOK[$id]}" ] && t="${TOK[$id]}"
  [ -n "$TOKEN" ] && t="$TOKEN"; TOKEN=""
  local auth=(); [ -n "$t" ] && [ "$t" != "-" ] && auth=(-H "Authorization: Bearer $t")
  if [ -n "$b" ]; then
    code=$(curl -s -o "$TMP" -w "%{http_code}" -X "$m" "$API$u" "${auth[@]}" -H "Content-Type: application/json" -d "$b")
  else
    code=$(curl -s -o "$TMP" -w "%{http_code}" -X "$m" "$API$u" "${auth[@]}")
  fi
  BODY=$(cat "$TMP")
  if [ "$code" = "$exp" ]; then PASS=$((PASS+1)); printf "OK   %s %-45s %s  %s\n" "$m" "$u" "$code" "$d"
  else FAIL=$((FAIL+1)); printf "FAIL %s %-45s %s (esperado %s)  %s\n     -> %s\n" "$m" "$u" "$code" "$exp" "$d" "${BODY:0:300}"; fi
}
jget() { python -c "import sys,json; d=json.loads(sys.argv[1]); print(eval('d'+sys.argv[2]))" "$BODY" "$1"; }
check() { # condicion-python descripcion   (d = json del ultimo BODY)
  if python -c "import sys,json; d=json.loads(sys.argv[1]); sys.exit(0 if ($1) else 1)" "$BODY" 2>/dev/null; then
    PASS=$((PASS+1)); echo "OK   check: $2"
  else FAIL=$((FAIL+1)); echo "FAIL check: $2 -> ${BODY:0:300}"; fi
}
nopass() { if echo "$BODY" | grep -qi "password"; then FAIL=$((FAIL+1)); echo "FAIL check: la respuesta contiene password -> ${BODY:0:200}"; else PASS=$((PASS+1)); echo "OK   check: sin password en la respuesta"; fi; }

ts() { date -d "$1" +%Y-%m-%dT%H:%M:%S; }
AYER=$(ts "-1 day"); MANANA=$(ts "+1 day"); SEMANA=$(ts "+7 day"); HACE1H=$(ts "-1 hour"); EN1H=$(ts "+1 hour"); EN2D=$(ts "+2 day"); EN3D=$(ts "+3 day")

echo "=== AUTH"
req POST /auth/registro '{"nombre":"Profe Juan","email":"juan@uv.mx","password":"123456","rol":"maestro"}' 201 "registro maestro"; nopass; M=$(jget "['idUsuario']")
req POST /auth/registro '{"nombre":"Ana","email":"ana@uv.mx","password":"123456","rol":"ALUMNO"}' 201 "registro alumno 1"; A1=$(jget "['idUsuario']")
req POST /auth/registro '{"nombre":"Beto","email":"beto@uv.mx","password":"123456","rol":"ALUMNO"}' 201 "registro alumno 2"; A2=$(jget "['idUsuario']")
req POST /auth/registro '{"nombre":"Caro","email":"caro@uv.mx","password":"123456","rol":"ALUMNO"}' 201 "registro alumno 3"; A3=$(jget "['idUsuario']")
req POST /auth/registro '{"nombre":"X","email":"x@uv.mx","password":"1","rol":"ADMIN"}' 400 "no se puede registrar ADMIN"
req POST /auth/registro '{"nombre":"X","email":"ana@uv.mx","password":"1","rol":"ALUMNO"}' 409 "email repetido"
req POST /auth/registro '{"nombre":"X"}' 400 "faltan campos"
req POST /auth/login '{"email":"ana@uv.mx","password":"123456"}' 200 "login correcto"; nopass; check "d['usuario']['idUsuario']==$A1 and d['token'].count('.')==2" "regresa usuario y token JWT"; TOK[$A1]=$(jget "['token']")
req POST /auth/login '{"email":"juan@uv.mx","password":"123456"}' 200 "login maestro"; TOK[$M]=$(jget "['token']"); DEF=${TOK[$M]}
req POST /auth/login '{"email":"beto@uv.mx","password":"123456"}' 200 "login alumno 2"; TOK[$A2]=$(jget "['token']")
req POST /auth/login '{"email":"caro@uv.mx","password":"123456"}' 200 "login alumno 3"; TOK[$A3]=$(jget "['token']")
req POST /auth/login '{"email":"admin@uv.mx","password":"admin123"}' 200 "login admin (creado al arrancar)"; AD=$(jget "['usuario']['idUsuario']"); TOK[$AD]=$(jget "['token']"); check "d['usuario']['rol']=='ADMIN'" "rol ADMIN"
req POST /auth/login '{"email":"ana@uv.mx","password":"mal"}' 401 "password incorrecta"
req POST /auth/login '{"email":"nadie@uv.mx","password":"123456"}' 401 "email inexistente"
req GET "/auth/me?idUsuario=$A1" "" 200 "mis datos"; check "d['email']=='ana@uv.mx'" "me devuelve al usuario correcto"
req GET "/auth/me?idUsuario=999" "" 403 "idUsuario distinto al del token"
TOKEN=${TOK[$A1]}; req GET "/auth/me" "" 200 "me sin idUsuario (lo toma del token)"; check "d['email']=='ana@uv.mx'" "usuario del token"

echo "=== TOKEN Y CORS"
TOKEN="-"; req GET "/cursos" "" 401 "sin token"
TOKEN="abc.def.ghi"; req GET "/cursos" "" 401 "token invalido"
TOKEN="${TOK[$A1]%?}x"; req GET "/cursos" "" 401 "token con firma alterada"
FALSO=$(python -c "import sys,base64,json; h,p,f=sys.argv[1].split('.'); d=json.loads(base64.urlsafe_b64decode(p+'==')); d['sub']=sys.argv[2]; q=base64.urlsafe_b64encode(json.dumps(d,separators=(',',':')).encode()).decode().rstrip('='); print(h+'.'+q+'.'+f)" "${TOK[$A1]}" "$M")
TOKEN="$FALSO"; req GET "/cursos?idUsuario=$M" "" 401 "token con usuario cambiado (firma no coincide)"
TOKEN=${TOK[$A1]}; req GET "/cursos?idUsuario=$M" "" 403 "alumno no puede usar el idUsuario del maestro"
TOKEN="-"; req POST /auth/registro '{"nombre":"Libre","email":"libre@uv.mx","password":"123456","rol":"ALUMNO"}' 201 "registro no pide token"; LB=$(jget "['idUsuario']")
req POST /auth/login '{"email":"libre@uv.mx","password":"123456"}' 200 "login alumno libre"; TOK[$LB]=$(jget "['token']")
H=$(curl -s -o /dev/null -D - -X OPTIONS "$API/cursos" -H "Origin: http://localhost:5500" -H "Access-Control-Request-Method: GET" -H "Access-Control-Request-Headers: authorization")
if echo "$H" | grep -qi "access-control-allow-origin: http://localhost:5500"; then PASS=$((PASS+1)); echo "OK   check: CORS permite http://localhost:5500"; else FAIL=$((FAIL+1)); echo "FAIL check: CORS localhost -> $H"; fi
C2=$(curl -s -o /dev/null -w "%{http_code}" -X OPTIONS "$API/cursos" -H "Origin: http://sitio-externo.com" -H "Access-Control-Request-Method: GET")
if [ "$C2" = "403" ]; then PASS=$((PASS+1)); echo "OK   check: CORS bloquea otros sitios"; else FAIL=$((FAIL+1)); echo "FAIL check: CORS externo -> $C2"; fi

echo "=== USUARIOS (permisos de ADMIN)"
TOKEN=${TOK[$AD]}; req GET /usuarios "" 200 "admin lista usuarios"; nopass; check "len(d)==6" "6 usuarios"
req GET /usuarios "" 403 "maestro no lista usuarios"
TOKEN=${TOK[$A1]}; req POST /usuarios '{"nombre":"Hacker","email":"hacker@uv.mx","password":"123456","rol":"ADMIN"}' 403 "alumno no crea usuarios"
TOKEN=${TOK[$A1]}; req PATCH "/usuarios/$A1/rol" '{"rol":"ADMIN"}' 403 "alumno no se da rol ADMIN"
TOKEN=${TOK[$A1]}; req PUT "/usuarios/$A1" '{"rol":"ADMIN"}' 403 "alumno no cambia su rol con PUT"
TOKEN=${TOK[$A1]}; req PATCH "/usuarios/$M/password" '{"password":"robada"}' 403 "alumno no cambia la password de otro"
TOKEN=${TOK[$A1]}; req PUT "/usuarios/$M" '{"nombre":"Otro"}' 403 "alumno no edita a otro usuario"
TOKEN=${TOK[$A1]}; req DELETE "/usuarios/$M" "" 403 "alumno no elimina usuarios"
TOKEN=${TOK[$AD]}; req DELETE "/usuarios/$AD" "" 403 "admin no se elimina a si mismo"
TOKEN=${TOK[$AD]}; req PATCH "/usuarios/$AD/rol" '{"rol":"ALUMNO"}' 403 "admin no cambia su propio rol"
TOKEN=${TOK[$A1]}; req PUT "/usuarios/$A1" '{"nombre":"Ana Lopez"}' 200 "usuario edita su nombre"; check "d['nombre']=='Ana Lopez' and d['rol']=='ALUMNO'" "nombre cambiado, rol igual"
TOKEN=${TOK[$AD]}; req POST /usuarios '{"nombre":"Profe Dos","email":"profe2@uv.mx","password":"123456","rol":"MAESTRO"}' 201 "admin crea maestro"; M2=$(jget "['idUsuario']")
req POST /auth/login '{"email":"profe2@uv.mx","password":"123456"}' 200 "login maestro 2"; TOK[$M2]=$(jget "['token']")
TOKEN=${TOK[$AD]}; req GET /usuarios "" 200 "admin lista usuarios"; check "len(d)==7" "7 usuarios"
req GET "/usuarios/$M" "" 200 "ver usuario"

echo "=== CURSOS"
req POST /cursos "{\"nombre\":\"Desarrollo Web\",\"descripcion\":\"501\",\"idMaestro\":$M}" 201 "crear curso"; C=$(jget "['idCurso']"); COD=$(jget "['codigoInvitacion']"); nopass
TOKEN=${TOK[$AD]}; req POST /cursos "{\"nombre\":\"Otro\",\"idMaestro\":$A1}" 400 "alumno no puede ser maestro"
req GET /cursos "" 200 "listar cursos"; nopass
req GET "/cursos/$C" "" 200 "ver curso"; nopass
req POST "/cursos/unirse?idUsuario=$A1" "{\"codigoInvitacion\":\"$COD\"}" 201 "alumno 1 se une"
req POST "/cursos/unirse?idUsuario=$A1" "{\"codigoInvitacion\":\"$COD\"}" 409 "ya inscrito"
req POST "/cursos/unirse?idUsuario=$A2" "{\"codigoInvitacion\":\"${COD,,}\"}" 201 "alumno 2 se une (codigo en minusculas)"
req POST "/cursos/unirse?idUsuario=$A3" "{\"codigoInvitacion\":\"$COD\"}" 201 "alumno 3 se une"
req POST "/cursos/unirse?idUsuario=$M" "{\"codigoInvitacion\":\"$COD\"}" 403 "maestro no puede unirse"
req POST "/cursos/unirse?idUsuario=$A1" '{"codigoInvitacion":"ZZZZZZ"}' 404 "codigo inexistente"
req GET "/cursos?idUsuario=$M" "" 200 "mis cursos (maestro)"; check "len(d)==1" "maestro ve 1 curso"
req GET "/cursos?idUsuario=$A1" "" 200 "mis cursos (alumno)"; check "len(d)==1 and d[0]['idCurso']==$C" "alumno ve su curso"
req GET "/cursos/$C/alumnos?idUsuario=$A1" "" 200 "ver alumnos"; check "len(d)==3 and d[0]['alumno']['email']=='ana@uv.mx' and d[0]['fechaInscripcion']" "3 inscripciones con alumno y fecha"; nopass
req GET "/cursos/$C/alumnos?idUsuario=$AD" "" 403 "no miembro no ve alumnos"
req POST "/cursos/$C/codigo?idUsuario=$M" "" 200 "regenerar codigo"; check "d['codigoInvitacion']!='$COD'" "codigo cambio"
req POST "/cursos/$C/codigo?idUsuario=$A1" "" 403 "alumno no regenera codigo"
req GET "/cursos/999" "" 404 "curso inexistente"
req POST /cursos "{\"nombre\":\"Ajeno\",\"idMaestro\":$M2}" 403 "maestro no crea cursos a nombre de otro"
TOKEN=${TOK[$A1]}; req GET /cursos "" 200 "alumno sin idUsuario ve solo sus cursos"; check "len(d)==1 and d[0]['idCurso']==$C" "solo su curso"
TOKEN=${TOK[$LB]}; req GET /cursos "" 200 "alumno sin cursos"; check "len(d)==0" "no ve codigos de otros cursos"
TOKEN=${TOK[$LB]}; req GET "/cursos/$C" "" 403 "no miembro no ve el curso ni su codigo"
TOKEN=${TOK[$AD]}; req GET "/cursos/$C" "" 200 "admin ve cualquier curso"
TOKEN=${TOK[$A1]}; req PUT "/cursos/$C" '{"nombre":"Curso hackeado"}' 403 "alumno no edita el curso"
TOKEN=${TOK[$A1]}; req DELETE "/cursos/$C" "" 403 "alumno no elimina el curso"
TOKEN=${TOK[$A1]}; req DELETE "/cursos/$C/alumnos/$A3" "" 403 "alumno no da de baja a otro"
TOKEN=${TOK[$M2]}; req PUT "/cursos/$C" '{"nombre":"X"}' 403 "otro maestro no edita el curso"
req PUT "/cursos/$C" '{"nombre":"Desarrollo Web"}' 200 "maestro edita su curso"

echo "=== TAREAS"
req POST "/cursos/$C/tareas?idUsuario=$M" "{\"titulo\":\"Practica 1\",\"descripcion\":\"Form\",\"fechaLimite\":\"$SEMANA\",\"esEquipo\":false}" 201 "tarea individual"; T1=$(jget "['idTarea']")
req POST "/cursos/$C/tareas?idUsuario=$M" "{\"titulo\":\"Proyecto\",\"fechaLimite\":\"$SEMANA\",\"esEquipo\":true}" 201 "tarea por equipo"; T2=$(jget "['idTarea']")
req POST "/cursos/$C/tareas?idUsuario=$M" "{\"titulo\":\"Futura\",\"fechaProgramada\":\"$MANANA\",\"fechaLimite\":\"$SEMANA\"}" 201 "tarea programada a futuro"; T3=$(jget "['idTarea']")
req POST "/cursos/$C/tareas?idUsuario=$M" "{\"titulo\":\"Vencida\",\"fechaLimite\":\"$AYER\"}" 201 "tarea ya vencida"; T4=$(jget "['idTarea']")
req POST "/cursos/$C/tareas?idUsuario=$A1" '{"titulo":"X"}' 403 "alumno no crea tareas"
req POST "/cursos/$C/tareas?idUsuario=$M" '{"descripcion":"sin titulo"}' 400 "falta titulo"
req POST "/cursos/$C/tareas?idUsuario=$M" "{\"titulo\":\"X\",\"fechaProgramada\":\"$SEMANA\",\"fechaLimite\":\"$MANANA\"}" 400 "limite antes de programada"
req GET "/cursos/$C/tareas?idUsuario=$M" "" 200 "maestro ve tareas"; check "len(d)==4" "maestro ve las 4"
req GET "/cursos/$C/tareas?idUsuario=$A1" "" 200 "alumno ve tareas"; check "len(d)==3 and all(t['idTarea']!=$T3 for t in d)" "alumno no ve la programada a futuro"
req GET "/tareas/$T1?idUsuario=$A1" "" 200 "ver tarea"; nopass
req GET "/tareas/$T1?idUsuario=$AD" "" 403 "no miembro no ve tarea"
req GET "/tareas/999?idUsuario=$M" "" 404 "tarea inexistente"

echo "=== NOTIFICACIONES"
req GET "/notificaciones?idUsuario=$A1" "" 200 "mis notificaciones"; check "len(d)==4" "4 notificaciones (una por tarea)"
req GET "/notificaciones?idUsuario=$A1&leido=false" "" 200 "solo no leidas"; check "len(d)==4" "4 no leidas"
req GET "/notificaciones/no-leidas/total?idUsuario=$A1" "" 200 "total no leidas"; check "d['total']==4" "total = 4"
req GET "/notificaciones?idUsuario=999" "" 403 "idUsuario ajeno a la sesion"

echo "=== EQUIPOS"
req POST "/tareas/$T2/equipos?idUsuario=$M" '{"nombreEquipo":"Equipo Rojo"}' 201 "crear equipo"; E1=$(jget "['idEquipo']")
req POST "/tareas/$T2/equipos?idUsuario=$M" '{"nombreEquipo":"Equipo Azul"}' 201 "crear equipo 2"; E2=$(jget "['idEquipo']")
req POST "/tareas/$T1/equipos?idUsuario=$M" '{"nombreEquipo":"X"}' 400 "tarea individual no lleva equipos"
req POST "/tareas/$T2/equipos?idUsuario=$LB" '{"nombreEquipo":"X"}' 403 "alumno no inscrito no crea equipos"
req POST "/equipos/$E1/integrantes?idUsuario=$M" "{\"idsAlumnos\":[$A1,$A2]}" 201 "agregar integrantes"; check "len(d['integrantes'])==2" "2 integrantes"; nopass
req POST "/equipos/$E2/integrantes?idUsuario=$M" "{\"idsAlumnos\":[$A2]}" 409 "alumno ya en otro equipo"
req POST "/equipos/$E2/integrantes?idUsuario=$M" "{\"idsAlumnos\":[$AD]}" 400 "alumno no inscrito"
req GET "/tareas/$T2/equipos?idUsuario=$A3" "" 200 "ver equipos"; check "len(d)==2" "2 equipos"
req GET "/equipos/$E1?idUsuario=$A1" "" 200 "ver equipo"

echo "=== ENTREGAS"
req POST "/tareas/$T1/entregas?idUsuario=$A1" '{"archivoUrl":"https://drive.example/p1-ana.zip"}' 201 "entrega individual"; EN1=$(jget "['idEntrega']"); check "d['alumno']['idUsuario']==$A1 and d['equipo'] is None" "se lleno id_alumno"; nopass
req POST "/tareas/$T1/entregas?idUsuario=$A1" '{"archivoUrl":"https://drive.example/x"}' 409 "no se entrega dos veces"
req POST "/tareas/$T1/entregas?idUsuario=$M" '{"archivoUrl":"https://drive.example/x"}' 403 "maestro no entrega"
req POST "/tareas/$T1/entregas?idUsuario=$A2" '{}' 400 "falta archivoUrl"
req POST "/tareas/$T1/entregas?idUsuario=$A2" '{"archivoUrl":"no es un enlace"}' 400 "enlace invalido"
req GET "/tareas/$T1/entregas/mia?idUsuario=$A1" "" 200 "mi entrega"
req GET "/tareas/$T1/entregas/mia?idUsuario=$A2" "" 404 "aun no entrega"
req GET "/tareas/$T1/entregas?idUsuario=$M" "" 200 "maestro ve entregas"; check "len(d)==1" "1 entrega"
req GET "/tareas/$T1/entregas?idUsuario=$A1" "" 403 "alumno no ve todas"
req GET "/entregas/$EN1?idUsuario=$M" "" 200 "maestro ve entrega"
req GET "/entregas/$EN1?idUsuario=$A2" "" 403 "otro alumno no ve entrega"
req GET "/entregas/$EN1/archivo?idUsuario=$A1" "" 302 "descargar archivo (redirige)"
req POST "/tareas/$T2/entregas?idUsuario=$A2" '{"archivoUrl":"https://drive.example/proyecto.zip"}' 201 "entrega por equipo"; check "d['equipo']['idEquipo']==$E1 and d['alumno'] is None" "se lleno id_equipo"; nopass
req GET "/tareas/$T2/entregas/mia?idUsuario=$A1" "" 200 "companero ve la entrega del equipo"
req POST "/tareas/$T2/entregas?idUsuario=$A1" '{"archivoUrl":"https://drive.example/x"}' 409 "el equipo ya entrego"
req POST "/tareas/$T2/entregas?idUsuario=$A3" '{"archivoUrl":"https://drive.example/x"}' 400 "alumno sin equipo"
req POST "/tareas/$T4/entregas?idUsuario=$A3" '{"archivoUrl":"https://drive.example/x"}' 400 "fecha limite vencida"

echo "=== PRORROGAS"
req POST "/tareas/$T4/prorrogas?idUsuario=$A3" "{\"nuevaFechaLimite\":\"$EN2D\"}" 400 "sin justificacion"
req POST "/tareas/$T4/prorrogas?idUsuario=$A3" "{\"nuevaFechaLimite\":\"$EN2D\",\"justificacion\":\"Estuve enfermo\"}" 201 "alumno solicita prorroga"; check "not d['aprobada'] and d['justificacion']=='Estuve enfermo'" "queda pendiente con justificacion"
req POST "/tareas/$T4/entregas?idUsuario=$A3" '{"archivoUrl":"https://drive.example/x"}' 400 "prorroga pendiente no habilita entrega"
req POST "/tareas/$T4/prorrogas?idUsuario=$M" "{\"nuevaFechaLimite\":\"$EN3D\",\"idAlumno\":$A3}" 201 "maestro otorga prorroga"; check "d['aprobada']==True" "aprobada"
req POST "/tareas/$T4/entregas?idUsuario=$A3" '{"archivoUrl":"https://drive.example/tarde.zip"}' 201 "entrega con prorroga aprobada"
req POST "/tareas/$T4/prorrogas?idUsuario=$A1" "{\"nuevaFechaLimite\":\"$AYER\",\"justificacion\":\"x\"}" 400 "nueva fecha no posterior"
req POST "/tareas/$T4/prorrogas?idUsuario=$AD" "{\"nuevaFechaLimite\":\"$EN2D\",\"justificacion\":\"x\"}" 403 "no miembro"
req POST "/tareas/$T2/prorrogas?idUsuario=$A1" "{\"nuevaFechaLimite\":\"$(ts '+10 day')\",\"justificacion\":\"Falta un integrante\"}" 201 "prorroga para el equipo"; check "d['equipo']['idEquipo']==$E1" "va al equipo"
req GET "/tareas/$T4/prorrogas?idUsuario=$M" "" 200 "maestro ve prorrogas"; check "len(d)==2" "2 prorrogas"
req GET "/tareas/$T4/prorrogas?idUsuario=$A3" "" 200 "alumno ve sus prorrogas"; check "len(d)==2 and all(p['alumno']['idUsuario']==$A3 for p in d)" "solo las suyas"
req GET "/tareas/$T4/prorrogas?idUsuario=$A1" "" 200 "otro alumno"; check "len(d)==0" "no ve las de otros"

echo "=== FOROS Y COMENTARIOS"
req POST "/cursos/$C/foros?idUsuario=$M" '{"titulo":"Dudas","descripcion":"Pregunten aqui"}' 201 "crear foro"; F=$(jget "['idForo']")
req POST "/cursos/$C/foros?idUsuario=$A1" '{"titulo":"X"}' 403 "alumno no crea foro"
req POST "/cursos/$C/foros?idUsuario=$M" '{}' 400 "falta titulo"
req GET "/cursos/$C/foros?idUsuario=$A1" "" 200 "ver foros"; check "len(d)==1" "1 foro"
req GET "/foros/$F?idUsuario=$A2" "" 200 "ver foro"
req GET "/foros/$F?idUsuario=$AD" "" 403 "no miembro"
req POST "/foros/$F/comentarios?idUsuario=$A1" '{"contenido":"Se entrega en zip?"}' 201 "alumno comenta"; nopass
req POST "/foros/$F/comentarios?idUsuario=$M" '{"contenido":"Si, en zip"}' 201 "maestro comenta"
req POST "/foros/$F/comentarios?idUsuario=$AD" '{"contenido":"x"}' 403 "no miembro no comenta"
req POST "/foros/$F/comentarios?idUsuario=$A1" '{"contenido":"  "}' 400 "comentario vacio"
req GET "/foros/$F/comentarios?idUsuario=$A2" "" 200 "ver comentarios"; check "len(d)==2 and d[0]['contenido'].startswith('Se entrega')" "2 en orden"; nopass

echo "=== EXAMENES Y PREGUNTAS"
req POST "/cursos/$C/examenes?idUsuario=$M" "{\"titulo\":\"Parcial 1\",\"fechaInicio\":\"$HACE1H\",\"fechaFin\":\"$EN1H\"}" 201 "examen abierto ahora"; X1=$(jget "['idExamen']")
req POST "/cursos/$C/examenes?idUsuario=$M" "{\"titulo\":\"Quiz\",\"fechaInicio\":\"$HACE1H\",\"fechaFin\":\"$EN1H\"}" 201 "examen solo opcion multiple"; X2=$(jget "['idExamen']")
req POST "/cursos/$C/examenes?idUsuario=$M" "{\"titulo\":\"Final\",\"fechaInicio\":\"$MANANA\",\"fechaFin\":\"$EN2D\"}" 201 "examen futuro"; X3=$(jget "['idExamen']")
req POST "/cursos/$C/examenes?idUsuario=$M" "{\"titulo\":\"X\",\"fechaInicio\":\"$EN1H\",\"fechaFin\":\"$HACE1H\"}" 400 "fin antes de inicio"
req POST "/cursos/$C/examenes?idUsuario=$A1" "{\"titulo\":\"X\",\"fechaInicio\":\"$HACE1H\",\"fechaFin\":\"$EN1H\"}" 403 "alumno no crea examen"
req GET "/cursos/$C/examenes?idUsuario=$A1" "" 200 "ver examenes"; check "len(d)==3" "3 examenes"
req GET "/examenes/$X1?idUsuario=$A1" "" 200 "ver examen"
req POST "/examenes/$X1/preguntas?idUsuario=$M" '{"pregunta":"Metodo para crear?","tipo":"OPCION_MULTIPLE","opciones":["GET","POST","PUT"],"respuestaCorrecta":"POST","valorPuntos":2}' 201 "pregunta opcion multiple"; P1=$(jget "['idPregunta']")
req POST "/examenes/$X1/preguntas?idUsuario=$M" '{"pregunta":"Que es REST?","tipo":"abierta","valorPuntos":5}' 201 "pregunta abierta"; P2=$(jget "['idPregunta']")
req POST "/examenes/$X1/preguntas?idUsuario=$M" '{"pregunta":"X","tipo":"OPCION_MULTIPLE","opciones":["a","b"],"respuestaCorrecta":"c","valorPuntos":1}' 400 "respuesta no esta en opciones"
req POST "/examenes/$X1/preguntas?idUsuario=$M" '{"pregunta":"X","tipo":"VERDADERO","valorPuntos":1}' 400 "tipo invalido"
req POST "/examenes/$X1/preguntas?idUsuario=$A1" '{"pregunta":"X","tipo":"ABIERTA","valorPuntos":1}' 403 "alumno no agrega preguntas"
req POST "/examenes/$X2/preguntas?idUsuario=$M" '{"pregunta":"2+2","tipo":"OPCION_MULTIPLE","opciones":["3","4"],"respuestaCorrecta":"4","valorPuntos":3}' 201 "pregunta quiz 1"; Q1=$(jget "['idPregunta']")
req POST "/examenes/$X2/preguntas?idUsuario=$M" '{"pregunta":"3+3","tipo":"OPCION_MULTIPLE","opciones":["6","7"],"respuestaCorrecta":"6","valorPuntos":2}' 201 "pregunta quiz 2"; Q2=$(jget "['idPregunta']")
req POST "/examenes/$X3/preguntas?idUsuario=$M" '{"pregunta":"Final","tipo":"ABIERTA","valorPuntos":10}' 201 "pregunta examen futuro"
req GET "/examenes/$X1/preguntas?idUsuario=$M" "" 200 "maestro ve preguntas"; check "any(p.get('respuestaCorrecta')=='POST' for p in d)" "maestro ve respuesta correcta"
req GET "/examenes/$X1/preguntas?idUsuario=$A1" "" 200 "alumno ve preguntas"; check "len(d)==2 and all('respuestaCorrecta' not in p for p in d)" "alumno NO ve respuesta correcta"
req GET "/examenes/$X3/preguntas?idUsuario=$A1" "" 403 "examen futuro no disponible"
req GET "/examenes/$X1/preguntas?idUsuario=$AD" "" 403 "no miembro"

echo "=== RESULTADOS"
req POST "/examenes/$X1/resultados?idUsuario=$A1" "{\"respuestas\":[{\"idPregunta\":$P1,\"respuestaDada\":\"POST\"},{\"idPregunta\":$P2,\"respuestaDada\":\"Un estilo de arquitectura\"}]}" 201 "enviar examen con abierta"; R1=$(jget "['resultado']['idResultado']"); check "d['resultado']['estadoRevision']=='PENDIENTE_MANUAL' and len(d['respuestas'])==2" "queda PENDIENTE_MANUAL"; nopass
req POST "/examenes/$X1/resultados?idUsuario=$A1" "{\"respuestas\":[]}" 409 "no se envia dos veces"
req POST "/examenes/$X2/resultados?idUsuario=$A1" "{\"respuestas\":[{\"idPregunta\":$Q1,\"respuestaDada\":\"4\"},{\"idPregunta\":$Q2,\"respuestaDada\":\"7\"}]}" 201 "enviar quiz"; check "d['resultado']['estadoRevision']=='CALIFICADO' and float(d['resultado']['calificacionFinal'])==3.0" "se califica solo: 3 de 5"
req POST "/examenes/$X2/resultados?idUsuario=$A2" "{\"respuestas\":[{\"idPregunta\":$Q1,\"respuestaDada\":\" 4 \"},{\"idPregunta\":$Q2,\"respuestaDada\":\"6\"}]}" 201 "quiz perfecto"; check "float(d['resultado']['calificacionFinal'])==5.0" "5 de 5"
req POST "/examenes/$X2/resultados?idUsuario=$A3" "{\"respuestas\":[{\"idPregunta\":$P1,\"respuestaDada\":\"x\"}]}" 400 "pregunta de otro examen"
req POST "/examenes/$X3/resultados?idUsuario=$A1" '{"respuestas":[]}' 400 "examen futuro"
req POST "/examenes/$X1/resultados?idUsuario=$M" '{"respuestas":[]}' 403 "maestro no presenta"
req GET "/examenes/$X2/resultados?idUsuario=$M" "" 200 "maestro ve resultados"; check "len(d)==2" "2 resultados"; nopass
req GET "/examenes/$X2/resultados?idUsuario=$A1" "" 403 "alumno no ve todos"
req GET "/examenes/$X1/resultados/mio?idUsuario=$A1" "" 200 "mi resultado"; check "all('respuestaCorrecta' not in r['pregunta'] for r in d['respuestas']) and all(r['pregunta']['valorPuntos'] for r in d['respuestas'])" "examen abierto: sin respuesta correcta, con valor de puntos"
req GET "/examenes/$X1/resultados/mio?idUsuario=$A2" "" 404 "aun no presenta"
req GET "/resultados/$R1?idUsuario=$A1" "" 200 "resultado propio con respuestas"; check "len(d['respuestas'])==2" "trae respuestas"
req GET "/resultados/$R1?idUsuario=$M" "" 200 "maestro ve resultado"
req GET "/resultados/$R1?idUsuario=$A2" "" 403 "otro alumno no ve resultado"

echo "=== FILTROS"
TOKEN=${TOK[$AD]}; req GET "/usuarios?rol=alumno" "" 200 "usuarios por rol"; check "len(d)==4 and all(u['rol']=='ALUMNO' for u in d)" "4 alumnos"
TOKEN=${TOK[$AD]}; req GET "/usuarios?q=ANA" "" 200 "buscar usuario"; check "len(d)==1 and d[0]['email']=='ana@uv.mx'" "encuentra a Ana sin importar mayusculas"
TOKEN=${TOK[$AD]}; req GET "/usuarios?rol=JEFE" "" 400 "rol invalido"
req GET "/cursos?q=desarrollo" "" 200 "buscar curso"; check "len(d)==1" "1 curso"
req GET "/cursos?q=python" "" 200 "buscar curso sin resultados"; check "len(d)==0" "0 cursos"
req GET "/cursos?idUsuario=$A1&q=web" "" 200 "mis cursos + busqueda"; check "len(d)==1" "1 curso"
req GET "/cursos?idMaestro=$M" "" 200 "cursos de un maestro"; check "len(d)==1" "1 curso"
req GET "/cursos/$C/alumnos?idUsuario=$M&q=beto" "" 200 "buscar alumno del curso"; check "len(d)==1 and d[0]['alumno']['nombre']=='Beto'" "encuentra a Beto"
req GET "/cursos/$C/tareas?idUsuario=$M&estado=vencida" "" 200 "tareas vencidas"; check "len(d)==1 and d[0]['idTarea']==$T4" "solo la vencida"
req GET "/cursos/$C/tareas?idUsuario=$M&estado=pendiente" "" 200 "tareas pendientes"; check "len(d)==3" "3 pendientes"
req GET "/cursos/$C/tareas?idUsuario=$M&esEquipo=true" "" 200 "tareas por equipo"; check "len(d)==1 and d[0]['idTarea']==$T2" "solo la de equipo"
req GET "/cursos/$C/tareas?idUsuario=$M&q=pr%C3%A1ctica" "" 200 "buscar tarea con acento"; check "len(d)==1 and d[0]['idTarea']==$T1" "practica = práctica"
req GET "/cursos/$C/tareas?idUsuario=$M" "" 200 "orden por fecha limite"; check "d[0]['idTarea']==$T4" "la mas proxima a vencer primero"
req GET "/cursos/$C/tareas?idUsuario=$A1&estado=pendiente" "" 200 "alumno: pendientes"; check "len(d)==2" "2 (no ve la programada)"
req GET "/cursos/$C/tareas?idUsuario=$M&estado=xx" "" 400 "estado invalido"
req GET "/tareas/$T1/entregas?idUsuario=$M&calificada=false" "" 200 "entregas sin calificar"; check "len(d)==1" "1 sin calificar"
req GET "/tareas/$T1/entregas?idUsuario=$M&calificada=true" "" 200 "entregas calificadas"; check "len(d)==0" "0 calificadas"
req GET "/tareas/$T4/entregas?idUsuario=$M&tarde=true" "" 200 "entregas tarde"; check "len(d)==1" "1 entregada tarde"
req GET "/tareas/$T1/entregas?idUsuario=$M&tarde=true" "" 200 "entregas a tiempo no salen"; check "len(d)==0" "0 tarde"
req GET "/tareas/$T4/prorrogas?idUsuario=$M&aprobada=false" "" 200 "prorrogas pendientes"; check "len(d)==1 and not d[0]['aprobada']" "1 pendiente"
req GET "/tareas/$T4/prorrogas?idUsuario=$M&aprobada=true" "" 200 "prorrogas aprobadas"; check "len(d)==1 and d[0]['aprobada']==True" "1 aprobada"
req GET "/cursos/$C/foros?idUsuario=$A1&q=dud" "" 200 "buscar foro"; check "len(d)==1" "1 foro"
req GET "/cursos/$C/foros?idUsuario=$A1&q=nada" "" 200 "buscar foro sin resultados"; check "len(d)==0" "0 foros"
req GET "/foros/$F/comentarios?idUsuario=$A1&idAutor=$M" "" 200 "comentarios de un autor"; check "len(d)==1 and d[0]['contenido']=='Si, en zip'" "solo los del maestro"
req GET "/foros/$F/comentarios?idUsuario=$A1&q=ZIP" "" 200 "buscar en comentarios"; check "len(d)==2" "2 mencionan zip"
req GET "/cursos/$C/examenes?idUsuario=$A1&estado=disponible" "" 200 "examenes disponibles"; check "len(d)==2" "2 disponibles"
req GET "/cursos/$C/examenes?idUsuario=$A1&estado=proximo" "" 200 "examenes proximos"; check "len(d)==1 and d[0]['idExamen']==$X3" "1 proximo"
req GET "/cursos/$C/examenes?idUsuario=$A1&estado=finalizado" "" 200 "examenes finalizados"; check "len(d)==0" "0 finalizados"
req GET "/cursos/$C/examenes?idUsuario=$A1&q=quiz" "" 200 "buscar examen"; check "len(d)==1 and d[0]['idExamen']==$X2" "encuentra el quiz"
req GET "/cursos/$C/examenes?idUsuario=$A1&estado=hoy" "" 400 "estado invalido"
req GET "/examenes/$X1/preguntas?idUsuario=$M&tipo=abierta" "" 200 "preguntas abiertas"; check "len(d)==1 and d[0]['tipo']=='ABIERTA'" "1 abierta"
req GET "/examenes/$X1/preguntas?idUsuario=$A1&tipo=OPCION_MULTIPLE" "" 200 "alumno filtra por tipo"; check "len(d)==1 and 'respuestaCorrecta' not in d[0]" "sigue sin respuesta correcta"
req GET "/examenes/$X1/preguntas?idUsuario=$M&tipo=otra" "" 400 "tipo invalido"
req GET "/examenes/$X2/resultados?idUsuario=$M&estado=calificado" "" 200 "resultados calificados"; check "len(d)==2" "2 calificados"
req GET "/examenes/$X1/resultados?idUsuario=$M&estado=PENDIENTE_MANUAL" "" 200 "resultados pendientes"; check "len(d)==1" "1 pendiente"
req GET "/examenes/$X2/resultados?idUsuario=$M&minCalificacion=4" "" 200 "calificacion minima"; check "len(d)==1 and float(d[0]['calificacionFinal'])==5.0" "solo el de 5"
req GET "/examenes/$X2/resultados?idUsuario=$M&maxCalificacion=4" "" 200 "calificacion maxima"; check "len(d)==1 and float(d[0]['calificacionFinal'])==3.0" "solo el de 3"
req GET "/examenes/$X2/resultados?idUsuario=$M&estado=x" "" 400 "estado invalido"
req GET "/notificaciones?idUsuario=$A1&leido=true" "" 200 "notificaciones leidas"; check "len(d)==0" "0 leidas"

echo "=== OBSERVACIONES: fechas automaticas"
req GET "/usuarios/$A1" "" 200 "usuario"; check "d['fechaRegistro'] is not None" "fechaRegistro se guarda"
req GET "/foros/$F?idUsuario=$A1" "" 200 "foro"; check "d['fechaCreacion'] is not None" "fechaCreacion se guarda"
req GET "/foros/$F/comentarios?idUsuario=$A1" "" 200 "comentarios"; check "all(c['fechaComentario'] is not None for c in d)" "fechaComentario se guarda"
req GET "/notificaciones?idUsuario=$A1" "" 200 "notificaciones"; check "all(n['fechaEnvio'] is not None for n in d)" "fechaEnvio se guarda"
req GET "/resultados/$R1?idUsuario=$A1" "" 200 "resultado"; check "d['resultado']['fechaRealizacion'] is not None" "fechaRealizacion se guarda"

echo "=== OBSERVACIONES: PATCH"
req PATCH "/usuarios/$A3/password" '{"password":"nueva123"}' 403 "maestro no cambia la password de un alumno"
TOKEN=${TOK[$A3]}; req PATCH "/usuarios/$A3/password" '{"password":"nueva123"}' 200 "cambiar mi password"; nopass
req POST /auth/login '{"email":"caro@uv.mx","password":"nueva123"}' 200 "login con la nueva password"
req POST /auth/login '{"email":"caro@uv.mx","password":"123456"}' 401 "la password vieja ya no sirve"
req PUT "/usuarios/$A3/password" '{"password":"x"}' 405 "PUT ya no se acepta en password"
TOKEN=${TOK[$AD]}; req PATCH "/usuarios/$LB/rol" '{"rol":"maestro"}' 200 "admin cambia rol"; check "d['rol']=='MAESTRO'" "rol cambiado"
TOKEN=${TOK[$AD]}; req PATCH "/usuarios/$LB/rol" '{"rol":"ALUMNO"}' 200 "regresar rol"
req PATCH "/entregas/$EN1/calificacion?idUsuario=$M" '{"calificacion":9.5,"comentarioMaestro":"Bien"}' 200 "calificar entrega"; check "float(d['calificacion'])==9.5 and d['comentarioMaestro']=='Bien'" "calificacion guardada"
req PATCH "/entregas/$EN1/calificacion?idUsuario=$A1" '{"calificacion":10}' 403 "alumno no se califica"
req PATCH "/entregas/$EN1/calificacion?idUsuario=$M" '{"calificacion":150}' 400 "calificacion fuera de rango"
req PATCH "/entregas/$EN1/calificacion?idUsuario=$M" '{}' 400 "falta calificacion"
req PATCH "/entregas/999/calificacion?idUsuario=$M" '{"calificacion":5}' 404 "entrega inexistente"
req GET "/notificaciones?idUsuario=$A1" "" 200 "aviso de calificacion"; check "any('fue calificada: 9.5' in n['mensaje'] for n in d)" "el alumno recibe aviso"
req GET "/tareas/$T1/entregas?idUsuario=$M&calificada=true" "" 200 "filtro calificada"; check "len(d)==1" "ahora 1 calificada"
req GET "/tareas/$T4/prorrogas?idUsuario=$M&aprobada=false" "" 200 "prorroga pendiente"; PR=$(jget "[0]['idProrroga']")
req PATCH "/prorrogas/$PR?idUsuario=$M" "{\"aprobada\":true,\"nuevaFechaLimite\":\"$EN2D\"}" 200 "aprobar prorroga (PATCH)"
req GET "/tareas/$T4/prorrogas?idUsuario=$M&aprobada=false" "" 200 "ya no hay pendientes"; check "len(d)==0" "0 pendientes"
req GET "/resultados/$R1?idUsuario=$M" "" 200 "respuestas del examen"; RA=$(python -c "import sys,json; d=json.loads(sys.argv[1]); print(next(r['idRespuesta'] for r in d['respuestas'] if r['puntajeObtenido'] is None))" "$BODY")
req PATCH "/respuestas/$RA?idUsuario=$M" '{"puntajeObtenido":50}' 400 "puntaje mayor al valor de la pregunta"
req PATCH "/respuestas/$RA?idUsuario=$M" '{"puntajeObtenido":-1}' 400 "puntaje negativo"
req PATCH "/respuestas/$RA?idUsuario=$M" '{"puntajeObtenido":4,"comentarioMaestro":"Incompleta"}' 200 "calificar respuesta abierta (PATCH)"
req GET "/resultados/$R1?idUsuario=$A1" "" 200 "resultado recalculado"; check "d['resultado']['estadoRevision']=='CALIFICADO' and float(d['resultado']['calificacionFinal'])==6.0" "pasa a CALIFICADO con 2+4=6"
req GET "/notificaciones?idUsuario=$A1&leido=false" "" 200 "no leidas"; NID=$(jget "[0]['idNotificacion']"); NOL=$(python -c "import sys,json;print(len(json.loads(sys.argv[1])))" "$BODY")
req PATCH "/notificaciones/$NID/leida?idUsuario=$A1" "" 200 "marcar una leida (PATCH)"; check "d['leido']==True" "quedo leida"
req GET "/notificaciones/no-leidas/total?idUsuario=$A1" "" 200 "total baja en 1"; check "d['total']==$NOL-1" "una menos"
req PATCH "/notificaciones/leidas?idUsuario=$A1" "" 200 "marcar todas leidas (PATCH)"
req GET "/notificaciones/no-leidas/total?idUsuario=$A1" "" 200 "total en cero"; check "d['total']==0" "0 no leidas"
req PUT "/notificaciones/leidas?idUsuario=$A1" "" 405 "PUT ya no se acepta en notificaciones"

echo "=== CORRECCION DE DEFECTOS"
req POST "/tareas/$T2/equipos?idUsuario=$A3" '{"nombreEquipo":"Equipo Caro"}' 201 "alumno crea su propio equipo"; check "len(d['integrantes'])==1 and d['integrantes'][0]['idUsuario']==$A3" "queda como integrante"
req POST "/tareas/$T2/equipos?idUsuario=$A3" '{"nombreEquipo":"Otro"}' 409 "no puede crear un segundo equipo"
ARCH=$(mktemp); echo "contenido de prueba" > "$ARCH"
C3=$(curl -s -o "$TMP" -w "%{http_code}" -X POST "$API/tareas/$T1/entregas?idUsuario=$A2" -H "Authorization: Bearer ${TOK[$A2]}" -F "archivo=@$(cygpath -w "$ARCH" 2>/dev/null || echo "$ARCH");filename=tarea.txt" -F "descripcion=Mi tarea"); BODY=$(cat "$TMP")
if [ "$C3" = "201" ]; then PASS=$((PASS+1)); echo "OK   POST entrega con archivo 201"; else FAIL=$((FAIL+1)); echo "FAIL POST entrega con archivo $C3 -> ${BODY:0:200}"; fi
URLA=$(jget "['archivoUrl']"); C4=$(curl -s -o "$TMP" -w "%{http_code}" "$URLA")
if [ "$C4" = "200" ] && grep -q "contenido de prueba" "$TMP"; then PASS=$((PASS+1)); echo "OK   check: el archivo subido se puede descargar"; else FAIL=$((FAIL+1)); echo "FAIL check: descarga $C4 $URLA"; fi
GRANDE=$(mktemp); head -c 21000000 /dev/zero > "$GRANDE"
C5=$(curl -s -o "$TMP" -w "%{http_code}" -X POST "$API/tareas/$T3/entregas?idUsuario=$A2" -H "Authorization: Bearer ${TOK[$A2]}" -F "archivo=@$(cygpath -w "$GRANDE" 2>/dev/null || echo "$GRANDE");filename=grande.zip"); BODY=$(cat "$TMP")
if [ "$C5" = "413" ]; then PASS=$((PASS+1)); echo "OK   archivo de 21 MB rechazado con 413: $BODY"; else FAIL=$((FAIL+1)); echo "FAIL archivo grande $C5 -> ${BODY:0:200}"; fi
rm -f "$ARCH" "$GRANDE"
req GET "/no-existe" "" 404 "ruta inexistente"; if echo "$BODY" | grep -q "trace"; then FAIL=$((FAIL+1)); echo "FAIL check: la respuesta trae el stack trace"; else PASS=$((PASS+1)); echo "OK   check: sin stack trace en el error"; fi
req POST "/cursos/$C/tareas?idUsuario=$M" '{"titulo":"X","fechaLimite":"no-es-fecha"}' 400 "fecha mal escrita"; echo "     -> $BODY"
TOKEN=${TOK[$A3]}; req DELETE "/cursos/$C/alumnos/$A3" "" 204 "alumno se da de baja a si mismo"

echo "=== DEFECTOS REPORTADOS POR EL EQUIPO"
# Kendar: PUT/DELETE de otros controladores regresaban 200 con null o texto
TOKEN=${TOK[$A1]}; req PUT "/tareas/$T1?idUsuario=$A1" '{"titulo":"X"}' 403 "alumno no edita tarea"
TOKEN=${TOK[$A1]}; req DELETE "/tareas/$T1?idUsuario=$A1" "" 403 "alumno no elimina tarea"
req PUT "/tareas/999?idUsuario=$M" '{"titulo":"X"}' 404 "editar tarea inexistente"
req DELETE "/tareas/999?idUsuario=$M" "" 404 "eliminar tarea inexistente"
req PUT "/tareas/$T1?idUsuario=$M" "{\"fechaProgramada\":\"$SEMANA\",\"fechaLimite\":\"$MANANA\"}" 400 "limite antes de programada al editar"
TOKEN=${TOK[$A1]}; req PUT "/examenes/$X1?idUsuario=$A1" '{"titulo":"X"}' 403 "alumno no edita examen"
TOKEN=${TOK[$A1]}; req DELETE "/examenes/$X1?idUsuario=$A1" "" 403 "alumno no elimina examen"
req DELETE "/examenes/999?idUsuario=$M" "" 404 "eliminar examen inexistente"
TOKEN=${TOK[$A1]}; req PUT "/preguntas/$P1?idUsuario=$A1" '{"pregunta":"X"}' 403 "alumno no edita pregunta"
TOKEN=${TOK[$A1]}; req DELETE "/preguntas/$P1?idUsuario=$A1" "" 403 "alumno no elimina pregunta"
TOKEN=${TOK[$A1]}; req PUT "/foros/$F?idUsuario=$A1" '{"titulo":"X"}' 403 "alumno no edita foro"
TOKEN=${TOK[$A1]}; req DELETE "/foros/$F?idUsuario=$A1" "" 403 "alumno no elimina foro"
req DELETE "/foros/999?idUsuario=$M" "" 404 "eliminar foro inexistente"
TOKEN=${TOK[$A2]}; req PUT "/entregas/$EN1?idUsuario=$A2" '{"archivoUrl":"https://drive.example/robo.zip"}' 403 "otro alumno no reemplaza la entrega"
TOKEN=${TOK[$A1]}; req PUT "/entregas/$EN1?idUsuario=$A1" '{"archivoUrl":"no es enlace"}' 400 "reemplazo con enlace invalido"
TOKEN=${TOK[$A2]}; req DELETE "/notificaciones/999?idUsuario=$A2" "" 404 "eliminar notificacion inexistente"
TOKEN=${TOK[$A1]}; req PATCH "/prorrogas/999?idUsuario=$A1" '{"aprobada":true}' 404 "prorroga inexistente"
req PUT "/foros/$F?idUsuario=$M" '{"titulo":"Dudas generales"}' 200 "maestro edita foro"; check "d['titulo']=='Dudas generales'" "titulo cambiado"
req DELETE "/comentarios/999?idUsuario=$M" "" 404 "eliminar comentario inexistente"

# Kendar: borrar un curso con tareas, foros y examenes daba 500 (llaves foraneas)
req POST /cursos "{\"nombre\":\"Curso desechable\",\"idMaestro\":$M}" 201 "curso desechable"; CD=$(jget "['idCurso']"); CODD=$(jget "['codigoInvitacion']")
req POST "/cursos/unirse?idUsuario=$A1" "{\"codigoInvitacion\":\"$CODD\"}" 201 "alumno se inscribe"
req POST "/cursos/$CD/tareas?idUsuario=$M" "{\"titulo\":\"T eq\",\"fechaLimite\":\"$SEMANA\",\"esEquipo\":true}" 201 "tarea por equipo"; TD=$(jget "['idTarea']")
req POST "/tareas/$TD/equipos?idUsuario=$A1" '{"nombreEquipo":"Eq"}' 201 "equipo"
req POST "/tareas/$TD/entregas?idUsuario=$A1" '{"archivoUrl":"https://drive.example/eq.zip"}' 201 "entrega del equipo"
req POST "/tareas/$TD/prorrogas?idUsuario=$A1" "{\"nuevaFechaLimite\":\"$(ts '+10 day')\",\"justificacion\":\"x\"}" 201 "prorroga del equipo"
req POST "/cursos/$CD/foros?idUsuario=$M" '{"titulo":"F"}' 201 "foro"; FD=$(jget "['idForo']")
req POST "/foros/$FD/comentarios?idUsuario=$A1" '{"contenido":"hola"}' 201 "comentario"
req POST "/cursos/$CD/examenes?idUsuario=$M" "{\"titulo\":\"E\",\"fechaInicio\":\"$HACE1H\",\"fechaFin\":\"$EN1H\"}" 201 "examen"; XD=$(jget "['idExamen']")
req POST "/examenes/$XD/preguntas?idUsuario=$M" '{"pregunta":"1+1","tipo":"OPCION_MULTIPLE","opciones":["1","2"],"respuestaCorrecta":"2","valorPuntos":5}' 201 "pregunta"; PD=$(jget "['idPregunta']")
req POST "/examenes/$XD/resultados?idUsuario=$A1" "{\"respuestas\":[{\"idPregunta\":$PD,\"respuestaDada\":\"2\"}]}" 201 "examen resuelto"
TOKEN=${TOK[$A1]}; req DELETE "/cursos/$CD" "" 403 "alumno no borra el curso"
req DELETE "/examenes/$XD?idUsuario=$M" "" 200 "maestro borra examen con preguntas y resultados"
req DELETE "/tareas/$TD?idUsuario=$M" "" 200 "maestro borra tarea con equipo, entrega y prorroga"
req POST "/cursos/$CD/tareas?idUsuario=$M" "{\"titulo\":\"T2\",\"fechaLimite\":\"$SEMANA\"}" 201 "otra tarea"; TD2=$(jget "['idTarea']")
req POST "/tareas/$TD2/entregas?idUsuario=$A1" '{"archivoUrl":"https://drive.example/t2.zip"}' 201 "entrega"
req DELETE "/cursos/$CD" "" 204 "maestro borra curso con tareas, foros, comentarios e inscripciones"
req GET "/cursos/$CD" "" 404 "el curso ya no existe"
req GET "/foros/$FD?idUsuario=$M" "" 404 "su foro tampoco"

# Usuario con informacion relacionada: 409 en lugar de 500
TOKEN=${TOK[$AD]}; req DELETE "/usuarios/$M" "" 409 "no se borra un maestro con cursos"; echo "     -> $BODY"
TOKEN=${TOK[$AD]}; req DELETE "/usuarios/$M2" "" 204 "maestro sin datos si se borra"

echo
echo "TOTAL: $PASS OK, $FAIL FAIL"
rm -f "$TMP"
