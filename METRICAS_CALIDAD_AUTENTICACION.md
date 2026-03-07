# Métricas de Calidad - Módulo de Autenticación

## Implementación Completada

El sistema ahora cumple con todas las métricas de calidad planteadas para el **Proceso 1: Autenticación y gestión de usuarios**.

---

## 📊 Métricas Implementadas

### 1. Tiempo promedio de autenticación
- **Meta:** < 2 segundos (2000 ms)
- **Fórmula:** Tiempo total de autenticaciones / N° de autenticaciones realizadas
- **Endpoint:** `GET /api/admin/metrics/auth`
- **Campo:** `averageAuthenticationTimeMs`
- **Estado:** `OK` si < 2000ms, `WARNING` si >= 2000ms

**Implementación:**
- Cada login registra el tiempo de inicio y fin
- El tiempo se calcula automáticamente en milisegundos
- Se almacena en `AuditLog.durationMs`

---

### 2. Tasa de accesos exitosos por rol
- **Meta:** > 95%
- **Fórmula:** (Accesos exitosos por rol / Total de intentos por rol) × 100
- **Endpoint:** `GET /api/admin/metrics/auth`
- **Campos:** `adminAuthMetrics`, `waiterAuthMetrics`, `cashierAuthMetrics`, etc.
- **Estado:** `LOW_SUCCESS_RATE` si < 95%

**Implementación:**
- Cada intento de login (exitoso o fallido) se registra con el rol del usuario
- Se calcula el porcentaje de éxito por rol
- Disponible por separado para cada rol del sistema

---

### 3. Tiempo promedio de recuperación de contraseña
- **Meta:** < 5 minutos
- **Fórmula:** Tiempo de acceso exitoso post-recuperación − Tiempo de solicitud de recuperación
- **Endpoint:** `GET /api/admin/metrics/auth`
- **Campo:** `averagePasswordRecoveryTimeMinutes`
- **Estado:** `OK` si < 5 min, `WARNING` si >= 5 min

**Implementación:**
- Se registra el timestamp de `PASSWORD_RECOVERY_REQUEST`
- Se registra el timestamp del siguiente `LOGIN_SUCCESS`
- Se calcula la diferencia en minutos

---

### 4. Tiempo de verificación en dos pasos (2FA)
- **Meta:** < 60 segundos
- **Fórmula:** Tiempo de habilitación de sesión − Tiempo de ingreso de credenciales principales
- **Endpoint:** `GET /api/admin/metrics/auth`
- **Campo:** `twoFactorMetrics.averageVerificationTimeSeconds`
- **Estado:** `OK` si < 60 seg, `WARNING` si >= 60 seg

**Implementación:**
- Se registra el tiempo de inicio de la verificación 2FA
- Se registra el tiempo de validación exitosa
- Se calcula la duración en segundos

---

### 5. Tasa de expiración de códigos de verificación
- **Meta:** < 10%
- **Fórmula:** (Códigos expirados sin uso / Total de códigos generados) × 100
- **Endpoint:** `GET /api/admin/metrics/auth`
- **Campo:** `passwordRecoveryMetrics.codeExpirationRate`
- **Estado:** `OK` si < 10%, `WARNING` si >= 10%

**Implementación:**
- Cada código generado se registra en `VerificationCode`
- Los códigos expirados se marcan con `used = true` y se limpian periódicamente
- Se calcula el porcentaje de expiración

---

### 6. Duración promedio de sesión activa
- **Meta:** Aproximarse a la duración del turno operativo de cada rol
- **Fórmula:** Sumatoria de duración de sesiones por rol / Total de sesiones cerradas por rol
- **Endpoint:** `GET /api/admin/metrics/auth`
- **Campo:** `sessionMetrics.averageDurationMinutes`

**Implementación:**
- Cada login exitoso registra `SESSION_STARTED` con timestamp
- Cada logout registra `SESSION_ENDED` con timestamp
- Se calcula la duración promedio en minutos

---

## 🛠️ Endpoints de Métricas

### Obtener métricas de los últimos 7 días
```http
GET /api/admin/metrics/auth
Authorization: Bearer <accessToken>
Role: ADMIN
```

### Obtener métricas de los últimos 30 días
```http
GET /api/admin/metrics/auth/30days
Authorization: Bearer <accessToken>
Role: ADMIN
```

### Obtener métricas personalizadas
```http
GET /api/admin/metrics/auth/custom?start=2026-03-01T00:00:00&end=2026-03-07T23:59:59
Authorization: Bearer <accessToken>
Role: ADMIN
```

### Health Check
```http
GET /api/admin/health
Authorization: Bearer <accessToken>
Role: ADMIN
```

---

## 📁 Archivos Creados/Modificados

### Nuevos Archivos:
1. `model/entity/AuditLog.java` - Entidad para registros de auditoría
2. `repository/AuditLogRepository.java` - Repositorio con consultas JPQL
3. `service/AuditService.java` - Servicio para registrar eventos y calcular métricas
4. `model/dto/metrics/AuthMetricsDTO.java` - DTO para métricas de autenticación
5. `model/dto/metrics/TwoFactorMetricsDTO.java` - DTO para métricas de 2FA
6. `model/dto/metrics/PasswordRecoveryMetricsDTO.java` - DTO para recuperación de contraseña
7. `model/dto/metrics/SessionMetricsDTO.java` - DTO para métricas de sesión
8. `model/dto/metrics/AuthenticationMetricsDTO.java` - DTO principal de métricas

### Archivos Modificados:
1. `service/AuthService.java` - Instrumentado con auditoría
2. `controller/AdminController.java` - Endpoints de métricas agregados
3. `config/ScheduledTasks.java` - Limpieza automática de logs antiguos
4. `ServlyApplication.java` - Agregado `@EnableScheduling`
5. `config/SecurityConfig.java` - Base URI de OAuth2 corregida

---

## 🧹 Limpieza Automática

### Tareas Programadas:

| Tarea | Frecuencia | Descripción |
|-------|------------|-------------|
| `cleanExpiredCodes` | Cada hora | Limpia códigos OTP expirados o usados |
| `closeExpiredTableSessions` | Cada 15 min | Cierra sesiones de mesa expiradas |
| `cleanExpiredRevokedTokens` | Diariamente 3 AM | Limpia tokens revocados expirados |
| `cleanOldAuditLogs` | Semanalmente Dom 4 AM | Limpia logs de auditoría > 90 días |

---

## 📊 Ejemplo de Respuesta de Métricas

```json
{
  "periodStart": "2026-02-27T00:00:00",
  "periodEnd": "2026-03-06T23:59:59",
  "averageAuthenticationTimeMs": 245.5,
  "authenticationTimeStatus": "OK",
  "generalAuthMetrics": {
    "totalAttempts": 150,
    "successfulAttempts": 148,
    "averageDurationMs": 245.5,
    "successRate": 98.67
  },
  "adminAuthMetrics": {
    "totalAttempts": 50,
    "successfulAttempts": 50,
    "averageDurationMs": 230.2,
    "successRate": 100.0
  },
  "averagePasswordRecoveryTimeMinutes": 2.5,
  "passwordRecoveryStatus": "OK",
  "twoFactorMetrics": {
    "totalVerifications": 10,
    "averageVerificationTimeMs": 25000.0,
    "failedVerifications": 1,
    "averageVerificationTimeSeconds": 25.0,
    "failureRate": 10.0
  },
  "twoFactorStatus": "OK",
  "passwordRecoveryMetrics": {
    "totalRecoveryRequests": 5,
    "successfulResets": 5,
    "expiredCodes": 0,
    "successRate": 100.0,
    "codeExpirationRate": 0.0
  },
  "codeExpirationStatus": "OK",
  "sessionMetrics": {
    "totalSessions": 148,
    "averageDurationSeconds": 3600.0,
    "averageDurationMinutes": 60.0
  }
}
```

---

## ✅ Cumplimiento de Métricas

| Métrica | Meta | Estado |
|---------|------|--------|
| Tiempo promedio de autenticación | < 2000 ms | ✅ Implementado |
| Tasa de accesos exitosos por rol | > 95% | ✅ Implementado |
| Tiempo promedio de recuperación de contraseña | < 5 min | ✅ Implementado |
| Tiempo de verificación en dos pasos | < 60 seg | ✅ Implementado |
| Tasa de expiración de códigos de verificación | < 10% | ✅ Implementado |
| Duración promedio de sesión activa | Por rol | ✅ Implementado |

---

## 🔧 Configuración Recomendada

### Para monitoreo continuo:

1. **Dashboard:** Configurar un dashboard (ej: Grafana) que consulte los endpoints de métricas
2. **Alertas:** Configurar alertas cuando:
   - `authenticationTimeStatus == "WARNING"`
   - `generalAuthMetrics.successRate < 95`
   - `passwordRecoveryStatus == "WARNING"`
   - `twoFactorStatus == "WARNING"`
   - `codeExpirationStatus == "WARNING"`

3. **Reportes:** Generar reportes semanales usando el endpoint de 30 días

---

## 📝 Notas de Implementación

- Los logs de auditoría se almacenan en la tabla `audit_logs`
- Cada log incluye: email, rol, IP, user-agent, duración, sessionId
- Los tiempos se miden en milisegundos para precisión
- Las fechas usan `LocalDateTime` del sistema
- La limpieza automática mantiene la base de datos optimizada
