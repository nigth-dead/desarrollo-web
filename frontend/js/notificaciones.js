const contenedor = document.getElementById("tablaNotificaciones");
const usuario = Store.usuarioActual();

if (!usuario) {
    window.location.href = "../index.html";
}

async function renderNotificaciones() {
    const lista = await Store.notificaciones();
    if (lista.length === 0) {
        contenedor.innerHTML = `<p class="vacio">Sin notificaciones.</p>`;
        return;
    }
    const filas = lista.map((n) => {
        const badge = n.leido
            ? `<span class="badge badge-gris">Leída</span>`
            : `<span class="badge badge-azul">No leída</span>`;
        const botonLeida = n.leido ? "" : `<button type="button" class="btn btn-secundario btn-sm" data-marcar-leida="${n.id_notificacion}">Marcar leída</button>`;
        return `
            <tr>
                <td class="celda-texto" data-campo="mensaje">${Store.escaparHTML(n.mensaje)}</td>
                <td data-campo="fechaEnvio">${Store.formatearFecha(n.fecha_envio)}</td>
                <td data-campo="leido">${badge}</td>
                <td class="acciones">
                    ${botonLeida}
                    <button type="button" class="btn btn-peligro btn-sm" data-eliminar-notificacion="${n.id_notificacion}">Eliminar</button>
                </td>
            </tr>`;
    }).join("");
    contenedor.innerHTML = `
        <div class="tabla-contenedor">
            <table class="tabla" data-endpoint="/api/notificaciones">
                <thead><tr>
                    <th data-campo="mensaje">Mensaje</th>
                    <th data-campo="fechaEnvio">Fecha de envío</th>
                    <th data-campo="leido">Leído</th>
                    <th class="acciones">Acciones</th>
                </tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

renderNotificaciones();
contenedor.addEventListener("click", async (evento) => {
    const botonLeida = evento.target.closest("[data-marcar-leida]");
    if (botonLeida) {
        await Store.marcarNotificacionLeida(Number(botonLeida.dataset.marcarLeida));
        await renderNotificaciones();
        return;
    }
    const botonEliminar = evento.target.closest("[data-eliminar-notificacion]");
    if (botonEliminar) {
        await Store.eliminarNotificacion(Number(botonEliminar.dataset.eliminarNotificacion));
        await renderNotificaciones();
        return;
    }
});
