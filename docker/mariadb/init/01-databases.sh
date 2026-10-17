#!/bin/bash
# Se ejecuta una sola vez, cuando el volumen de MariaDB esta vacio.
# Crea las bases de cada microservicio y da permisos al usuario de la aplicacion.
set -e

mariadb -uroot -p"${MARIADB_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS vidasalud_appointments CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS vidasalud_catalog      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON vidasalud_appointments.* TO '${MARIADB_USER}'@'%';
GRANT ALL PRIVILEGES ON vidasalud_catalog.*      TO '${MARIADB_USER}'@'%';
FLUSH PRIVILEGES;
SQL
