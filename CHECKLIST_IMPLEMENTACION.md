# ✅ CHECKLIST - Implementar Soft Delete en Recetas

Sigue estos pasos en orden para aplicar la solución.

---

## PARTE 1: CÓDIGO JAVA (COMPLETADO ✓)

- [x] Archivo: `Recipe.java`
  - [x] Importado `LocalDateTime`
  - [x] Importado `@Where` de Hibernate
  - [x] Agregado campo `status` (default ACTIVE)
  - [x] Agregado campo `deletedAt`
  - [x] Agregada anotación `@Where(clause = "status = 'ACTIVE'")`

- [x] Archivo: `RecipeService.java`
  - [x] Importado `LocalDateTime`
  - [x] Modificado método `deleteRecipe()` para soft delete
  - [x] Ahora marca con `status = "DELETED"` y `deletedAt = NOW()`

**Estado:** ✅ LISTO - Código ya modificado, solo compila

---

## PARTE 2: BASE DE DATOS (PENDIENTE ⏳)

### Paso 1: Abrir Supabase
- [ ] Abre [supabase.com](https://supabase.com)
- [ ] Inicia sesión con tu cuenta
- [ ] Abre tu proyecto
- [ ] Ve a SQL Editor

### Paso 2: Copiar y Ejecutar SQL
- [ ] Copia el siguiente SQL:

```sql
-- Fix 1: Eliminar restricción UNIQUE en recipe_id
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;

-- Fix 2: Agregar columnas para soft delete en recipe
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
```

- [ ] Pégalo en el SQL Editor de Supabase
- [ ] Haz clic en "Run" (botón azul)
- [ ] Espera a que termine (debe decir "Success" o similar)

### Paso 3: Verificar que Funcionó
- [ ] Copia y ejecuta este SQL para verificar:

```sql
-- Ver estructura de la tabla recipe
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'recipe'
ORDER BY ordinal_position;
```

**Debe mostrar:**
- `id` - bigint - NO
- `name` - varchar - NO
- `quantity` - integer - YES
- `description` - varchar - YES
- `status` - varchar(20) - YES ✓ **NUEVO**
- `deleted_at` - timestamp - YES ✓ **NUEVO**
- `itemDetailList` - (relación)
- `products` - (relación)

---

## PARTE 3: COMPILAR (TU MÁQUINA)

- [ ] Abre terminal en el directorio del proyecto
- [ ] Ejecuta:
  ```bash
  ./gradlew clean build
  ```
- [ ] Espera a que termine (2-5 minutos)
- [ ] Debería terminar con: `BUILD SUCCESSFUL`

**Si hay errores:**
- [ ] Lee el mensaje de error
- [ ] Revisa que `Recipe.java` esté guardado correctamente
- [ ] Revisa que `RecipeService.java` esté guardado correctamente
- [ ] Intenta `clean build` nuevamente

---

## PARTE 4: REINICIAR SERVIDOR

- [ ] Detén la aplicación (Ctrl+C si está en terminal)
- [ ] Espera 5 segundos
- [ ] Reinicia la aplicación:
  ```bash
  ./gradlew bootRun
  ```
- [ ] Espera a que diga: `Started Application in X.XXX seconds`

---

## PARTE 5: VERIFICAR QUE FUNCIONA

### Opción A: Usando Postman
- [ ] Abre Postman
- [ ] Crea una petición DELETE:
  ```
  DELETE http://localhost:8081/api/admin/recipes/1
  Authorization: Bearer [TU_TOKEN]
  ```
- [ ] Envía
- [ ] Debería obtener: `200 OK`

### Opción B: Verificar en BD
- [ ] Ve a Supabase SQL Editor
- [ ] Ejecuta:
  ```sql
  SELECT id, name, status, deleted_at 
  FROM recipe 
  WHERE id = 1;
  ```
- [ ] Debería mostrar:
  ```
  id | name | status  | deleted_at
  1  | ... | DELETED | 2026-04-13 14:30:45
  ```

### Opción C: Verificar que no aparece en consultas
- [ ] En Postman, GET:
  ```
  GET http://localhost:8081/api/admin/recipes
  ```
- [ ] La receta eliminada NO debe aparecer en la lista

---

## 🎯 ESTADO FINAL

Si completaste todos los pasos arriba:

- ✓ El código Java está actualizado
- ✓ La BD tiene las nuevas columnas
- ✓ El servidor está ejecutando la nueva versión
- ✓ Puedes eliminar recetas sin errores
- ✓ Los datos se preservan (no se pierden)
- ✓ Las recetas eliminadas no aparecen en las consultas

---

## 🆘 SOLUCIONAR PROBLEMAS

### Error: "Receta no encontrada" al eliminar
**Causa:** La receta ID no existe
**Solución:** Usa el ID correcto (ej: 1, 2, 3...)

### Error: "Column 'status' does not exist"
**Causa:** No ejecutaste el SQL en Supabase
**Solución:** Vuelve a PARTE 2 y ejecuta el SQL

### Error: "syntax error" en SQL Editor
**Causa:** Copiaste el SQL incorrectamente
**Solución:** Copia tal cual de `SQL_SUPABASE_COMPLETO.sql`

### La receta aún aparece en /api/admin/recipes
**Causa:** El servidor tiene la versión vieja en caché
**Solución:** Reinicia el servidor (Ctrl+C y bootRun nuevamente)

### No puedo compilar (BUILD FAILED)
**Causa:** Errores en Recipe.java o RecipeService.java
**Solución:**
1. Abre el archivo
2. Revisa que los imports sean correctos
3. Revisa que no haya caracteres especiales
4. Intenta `clean build` nuevamente

---

## 📋 CHECKLIST FINAL

```
✅ Código Java modificado (Recipe.java, RecipeService.java)
⏳ SQL ejecutado en Supabase
⏳ Proyecto compilado (./gradlew clean build)
⏳ Servidor reiniciado
⏳ Verificado en Postman/BD que funciona
```

**Última actualización:** 13 de Abril 2026

---

## 💡 CONSEJOS

1. **Toma tu tiempo:** No corras los pasos
2. **Verifica cada paso:** Asegúrate que funcione antes de continuar
3. **Guarda cambios:** Si editas archivos Java, asegúrate de guardar (Ctrl+S)
4. **Reinicia después de cambios:** Siempre reinicia el servidor después de compilar

**¡Éxito! 🚀**

