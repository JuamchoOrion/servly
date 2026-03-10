# SOLUCIÓN: MaxClientsInSessionMode - Servly

## El Problema
Supabase limita el número de conexiones simultáneas en "Session Mode" (el pool_size por defecto).

Cuando Hibernate intenta inicializar, realiza múltiples validaciones de DDL/esquema que abren varias conexiones a la vez, superando el límite.

## La Solución Aplicada

### 1. Reducir el Pool de Conexiones (HikariCP)
```properties
spring.datasource.hikari.maximum-pool-size=3      # Solo 3 conexiones activas
spring.datasource.hikari.minimum-idle=1           # Mantener 1 inactiva
```

### 2. Cambiar de `ddl-auto=update` a `ddl-auto=validate`
```properties
spring.jpa.hibernate.ddl-auto=validate  # En lugar de 'update'
```

**Diferencia:**
- `update`: Intenta crear/modificar tablas en cada startup (usa muchas conexiones)
- `validate`: Solo verifica que las tablas existan (usa menos conexiones)

### 3. Desactivar Features Innecesarias
```properties
spring.jpa.show-sql=false                                    # Desactivado
spring.jpa.properties.hibernate.generate_statistics=false    # Sin estadísticas
spring.jpa.properties.hibernate.use_sql_comments=false       # Sin comentarios
```

---

## Verificar que Funciona

Una vez que el servidor inicie (espera ~30-45 segundos), ejecuta:

```powershell
# En la carpeta del proyecto:
.\test-actuator.ps1
```

**Deberías ver:**
- ✅ Health check responde
- ✅ Prometheus metrics disponibles
- ✅ El servidor NO falla con MaxClientsInSessionMode

---

## Si Aún Falla

**Intenta reducir más el pool:**
```properties
spring.datasource.hikari.maximum-pool-size=2
```

O **desactiva la validación de esquema completamente:**
```properties
spring.jpa.hibernate.ddl-auto=none
```

---

## Notas Importantes

1. **Schema ya existe en Supabase**: Cambiar a `validate` es seguro porque la BD ya está creada
2. **No perderás datos**: Solo cambias cómo Hibernate maneja el ciclo de vida del esquema
3. **Para producción**: Usa `validate` siempre (más seguro y eficiente)
4. **Para desarrollo local**: Si necesitas modificar esquemas, usa herramientas como Liquibase o Flyway

---

## Estado del Servidor

Si el `bootRun` está en progreso, verás logs como:
```
2026-03-10T17:xx:xx.xxx-05:00  INFO ... : HikariPool-1 - Starting...
2026-03-10T17:xx:xx.xxx-05:00  INFO ... : Tomcat started on port(s): 8081
2026-03-10T17:xx:xx.xxx-05:00  INFO ... : Started ServlyApplication in X.XXX seconds
```

Cuando veas **"Started ServlyApplication"**, ¡el server está listo!

