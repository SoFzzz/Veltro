-- Script para crear la base de datos veltro_db
-- Ejecuta esto en pgAdmin o psql

CREATE DATABASE veltro_db 
  WITH 
  ENCODING = 'UTF8'
  LC_COLLATE = 'C'
  LC_CTYPE = 'C'
  TEMPLATE = template0;

-- Conectar a la nueva base de datos y crear extensiones si es necesario
-- \c veltro_db

-- Crear schema público (generalmente ya existe)
-- CREATE SCHEMA IF NOT EXISTS public;

-- Dar permisos al usuario postgres
GRANT ALL PRIVILEGES ON DATABASE veltro_db TO postgres;
