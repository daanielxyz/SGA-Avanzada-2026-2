---
id: reservas-listado
titulo: Buscar y filtrar reservas
pantalla: /reservas
roles: RECEPCIONISTA, ADMINISTRADOR
---

# Buscar y filtrar reservas

Pantalla «Reservas» del menú. Muestra las reservas de la más reciente a la más antigua, de a 10 por página.

## Buscar una reserva por su código
El primer campo de los filtros es «Código»: se escribe el código (por ejemplo RES-2026-00042) y se pulsa «Filtrar». La lista muestra solo esa reserva; se abre tocándola. Mientras haya un código, los demás filtros no se aplican: vuelven a valer al quitar el código con la ✕ de su etiqueta. Si el código no existe, la lista lo dice («No existe la reserva…») y ofrece «Quitar el código».

## Filtrar
1. Pulsa «Filtros» (en el celular están plegados).
2. Llena los que necesites: Código, Desde y Hasta (fechas), Estado, Apartamento, Canal y Titular (su id, por ejemplo TIT-12).
   - **Estado, Apartamento y Canal admiten varios valores**: se abre la lista, se marcan las casillas y «Listo». Sin ninguna marcada es «Todos». En Estado hay dos atajos: **Activas** (pendiente, confirmada y en curso) y **Terminadas** (finalizada, cancelada y no se presentó).
3. Pulsa «Filtrar». Dentro de un filtro vale cualquiera de los valores marcados; entre filtros distintos se tienen que cumplir todos.

Los filtros aplicados aparecen como etiquetas; cada una se quita con su ✕, y «Limpiar» quita todos. Si el rango de fechas está mal (Desde después de Hasta), el mensaje sale junto a los filtros.

## La tabla
Cada fila tiene código, titular, entrada y salida, y estado. Se abre la reserva tocándola; al volver, el listado queda en la misma página y con los mismos filtros.

## Crear una reserva
El botón «+ Nueva reserva», debajo del título.
