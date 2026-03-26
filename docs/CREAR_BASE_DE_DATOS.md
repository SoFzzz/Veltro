# Guía: Crear Base de Datos PostgreSQL - veltro_db

## Opción 1: Usando pgAdmin (La más fácil)

### Paso 1: Abre pgAdmin
- Busca **"pgAdmin 4"** en el menú de Windows
- O ve a `http://localhost:5050` en tu navegador

### Paso 2: Conéctate al servidor PostgreSQL
- En la izquierda verás "Servers"
- Expande "Servers" y busca tu servidor (probablemente "PostgreSQL 14" o similar)
- Si te pide contraseña: **lolxd676**

### Paso 3: Crea la base de datos
1. Click derecho en **"Databases"**
2. Selecciona **"Create"** → **"Database..."**
3. En el formulario que aparece:
   - **Name**: `veltro_db`
   - **Owner**: `postgres`
   - Deja todo lo demás por defecto
4. Click en **"Save"**

### ✅ Listo! Tu base de datos está creada

---

## Opción 2: Usando SQL Shell (psql) - Línea de comandos

### Paso 1: Abre SQL Shell
- Busca **"SQL Shell (psql)"** en el menú de Windows
- Te aparecerá una ventana negra

### Paso 2: Conecta a PostgreSQL
Presiona **Enter** para aceptar los valores por defecto en cada pregunta:
```
Server [localhost]: (presiona Enter)
Database [postgres]: (presiona Enter)
Port [5432]: (presiona Enter)
Username [postgres]: (presiona Enter)
Password for user postgres: lolxd676
```

### Paso 3: Crea la base de datos
Copia y pega este comando:
```sql
CREATE DATABASE veltro_db;
```

Presiona **Enter**. Deberías ver:
```
CREATE DATABASE
```

### Paso 4: Verifica que se creó
```sql
\l
```

Deberías ver `veltro_db` en la lista.

### Paso 5: Sal de psql
```
\q
```

### ✅ Listo! Tu base de datos está creada

---

## Opción 3: Script automático

Si descargaste el archivo `create_database.bat`:
1. Abre una terminal (Cmd o PowerShell)
2. Ve a la carpeta de Veltro
3. Ejecuta:
```cmd
create_database.bat
```
4. Presiona Enter cuando te pida la contraseña (no se verá mientras escribes)

---

## Verificar que la base de datos existe

Una vez creada, puedes verificar en pgAdmin:
- Abre pgAdmin
- Expande "Databases"
- Deberías ver **"veltro_db"** en la lista

---

## ¿Cuál opción prefieres?

**Recomendado: Opción 1 (pgAdmin)** - Es la más visual y fácil.

Cuando termines, avísame y continuamos con:
- Ejecutar las migraciones Flyway
- Probar la aplicación
- Verificar que los datos se cargan correctamente
