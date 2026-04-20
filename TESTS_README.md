# Tests Unitarios - Órdenes y Gestión de Mesas

## 📋 Descripción

Este documento contiene tests unitarios para los módulos de **Órdenes** y **Gestión de Mesas** usando **JUnit 5 (Jupiter)** y **Mockito**.

## 🎯 Cobertura de Tests

### 1. **OrderServiceTest** 
Ubicación: `src/test/java/co/edu/uniquindio/servly/service/OrderServiceTest.java`

**Casos de prueba cubiertos:**

#### Crear Órdenes Delivery
- ✅ Crear orden delivery exitosamente
- ✅ Lanzar excepción cuando el producto no existe
- ✅ Lanzar excepción cuando no hay items disponibles

#### Crear Órdenes desde Mesa (Mesero)
- ✅ Crear orden de mesa exitosamente
- ✅ Lanzar excepción cuando la mesa no existe
- ✅ Crear TableSource si no existe
- ✅ Validar disponibilidad de items

#### Actualizar Estado de Órdenes
- ✅ Actualizar estado de una orden
- ✅ Lanzar excepción cuando la orden no existe

#### Cancelar Órdenes
- ✅ Cancelar orden con estado PENDING
- ✅ Lanzar excepción al cancelar orden SERVED
- ✅ Lanzar excepción al cancelar orden PAID
- ✅ Validar que solo se pueden cancelar órdenes pendientes

#### Confirmar Pago
- ✅ Confirmar pago de una orden
- ✅ Cambiar estado a PAID
- ✅ Lanzar excepción si la orden no existe

#### Consultar Órdenes
- ✅ Obtener órdenes por número de mesa
- ✅ Obtener órdenes por estado
- ✅ Obtener órdenes por fecha
- ✅ Retornar lista vacía cuando no hay órdenes

#### Cálculos
- ✅ Calcular total correcto de una orden
- ✅ Sumar subtotales de items

---

### 2. **RestaurantTableServiceTest**
Ubicación: `src/test/java/co/edu/uniquindio/servly/service/RestaurantTableServiceTest.java`

**Casos de prueba cubiertos:**

#### Crear Mesas
- ✅ Crear mesa exitosamente
- ✅ Validar número de mesa requerido y > 0
- ✅ Validar capacidad requerida y > 0
- ✅ Validar que el número de mesa sea único
- ✅ Crear mesa con ubicación

#### Obtener Mesas
- ✅ Obtener mesa por número
- ✅ Lanzar excepción si la mesa no existe
- ✅ Obtener todas las mesas ordenadas
- ✅ Retornar lista vacía cuando no hay mesas

#### Cambiar Estado de Mesa
- ✅ Cambiar estado a OCCUPIED
- ✅ Cambiar estado a MAINTENANCE
- ✅ Cambiar estado a RESERVED
- ✅ Cambiar estado a AVAILABLE

#### Filtrar por Estado
- ✅ Obtener todas las mesas disponibles
- ✅ Obtener todas las mesas ocupadas
- ✅ Obtener todas las mesas en mantenimiento
- ✅ Retornar lista vacía si no hay mesas con ese estado

#### Transiciones de Estado
- ✅ AVAILABLE → OCCUPIED
- ✅ OCCUPIED → AVAILABLE
- ✅ Cualquier estado → MAINTENANCE
- ✅ Cualquier estado → RESERVED

#### Eliminar Mesas
- ✅ Eliminar mesa exitosamente
- ✅ Lanzar excepción si intenta eliminar mesa inexistente

#### Capacidad y Ubicación
- ✅ Mesas con capacidad desde 1 persona
- ✅ Mesas con capacidad grande (20+)
- ✅ Ubicación opcional (null)
- ✅ Ubicación descriptiva

---

### 3. **TableSessionServiceTest**
Ubicación: `src/test/java/co/edu/uniquindio/servly/service/TableSessionServiceTest.java`

**Casos de prueba cubiertos:**

#### Abrir Sesión
- ✅ Abrir sesión cuando la mesa está disponible
- ✅ Lanzar excepción si la mesa no existe
- ✅ Lanzar excepción si ya existe sesión activa
- ✅ Cambiar estado de mesa a OCCUPIED
- ✅ Generar JWT válido
- ✅ Almacenar token en la sesión

#### Cerrar Sesión
- ✅ Cerrar sesión activa
- ✅ Registrar timestamp de cierre
- ✅ Cambiar estado de mesa a AVAILABLE
- ✅ Lanzar excepción si no hay sesión activa

#### Verificar Sesión Activa
- ✅ Retornar true cuando hay sesión activa
- ✅ Retornar false cuando no hay sesión
- ✅ Validar estado de sesión

#### Expiración de Sesión
- ✅ Crear sesión con tiempo de expiración
- ✅ Sesión expira en 4 horas
- ✅ Guardar hora de cierre

#### Token JWT
- ✅ Generar token único por sesión
- ✅ Almacenar token en la sesión

#### Sesiones Múltiples
- ✅ Permitir múltiples mesas con sesiones simultáneas
- ✅ Validar que no haya dos sesiones activas en la misma mesa

#### Ciclo de Vida Completo
- ✅ Abrir → Verificar activa → Cerrar
- ✅ Cambios de estado correctos

---

## 🚀 Cómo Ejecutar los Tests

### Opción 1: Ejecutar todos los tests
```bash
./gradlew test
```

### Opción 2: Ejecutar un archivo de test específico
```bash
./gradlew test --tests OrderServiceTest
./gradlew test --tests RestaurantTableServiceTest
./gradlew test --tests TableSessionServiceTest
```

### Opción 3: Ejecutar un test específico
```bash
./gradlew test --tests OrderServiceTest.CreateDeliveryOrder.shouldCreateDeliveryOrderSuccessfully
./gradlew test --tests RestaurantTableServiceTest.CreateTable.shouldCreateTableSuccessfully
```

### Opción 4: Ejecutar con cobertura de código
```bash
./gradlew test jacocoTestReport
```

El reporte estará en: `build/reports/jacoco/test/html/index.html`

### Opción 5: Desde el IDE (IntelliJ IDEA / Eclipse)
1. Click derecho en el archivo de test
2. Seleccionar `Run 'NombreTest'`
3. O con la combinación: `Ctrl+Shift+F10` (Windows/Linux) o `Ctrl+Shift+R` (Mac)

---

## 📊 Estructura de un Test

Todos los tests siguen el patrón **AAA (Arrange, Act, Assert)**:

```java
@Test
@DisplayName("Descripción clara del test")
void shouldDoSomething() {
    // ARRANGE - Preparar datos
    when(mockRepository.findById(1L)).thenReturn(Optional.of(entity));
    
    // ACT - Ejecutar la acción
    Result result = service.doSomething(1L);
    
    // ASSERT - Verificar el resultado
    assertEquals(expected, result.getValue());
    verify(mockRepository, times(1)).findById(1L);
}
```

---

## 🔧 Dependencias Requeridas

Los tests requieren estas dependencias en `build.gradle`:

```gradle
testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.2'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.9.2'
testImplementation 'org.mockito:mockito-core:5.2.0'
testImplementation 'org.mockito:mockito-junit-jupiter:5.2.0'
```

---

## 📝 Casos de Uso Cubiertos

### Flujo de Órdenes en Mesa
```
1. Mesero crea orden para mesa X
   ✅ ValidarMesa existe
   ✅ Validar productos
   ✅ Crear orden con estado PENDING
   ✅ Cambiar mesa a OCCUPIED

2. Cliente pide cuenta
   ✅ Obtener órdenes de la mesa
   ✅ Calcular total
   ✅ Cambiar estado a READY FOR PAYMENT

3. Confirmar pago
   ✅ Validar orden READY FOR PAYMENT
   ✅ Cambiar estado a PAID
   ✅ Cambiar mesa a AVAILABLE
   ✅ Cerrar sesión

4. Cancelar orden
   ✅ Solo si está en PENDING
   ✅ No si está SERVED o PAID
```

### Flujo de Gestión de Mesas
```
1. Admin crea mesa
   ✅ Validar número único
   ✅ Validar capacidad
   ✅ Estado inicial: AVAILABLE

2. Cliente abre sesión
   ✅ Generar JWT
   ✅ Cambiar mesa a OCCUPIED
   ✅ Guardar sesión

3. Cliente cierra sesión / Paga
   ✅ Cambiar mesa a AVAILABLE
   ✅ Cerrar sesión

4. Admin cambia estado a MAINTENANCE
   ✅ Mesa no disponible para nuevas sesiones
```

---

## ✅ Validaciones Implementadas

### Order Service
- ✓ Producto debe existir
- ✓ Receta debe tener items
- ✓ Mesa debe existir
- ✓ No permitir cancelar órdenes SERVED o PAID
- ✓ No permitir cambios después de PAID
- ✓ Calcular totales correctamente

### Restaurant Table Service
- ✓ Número de mesa requerido y > 0
- ✓ Número de mesa único
- ✓ Capacidad requerida y > 0
- ✓ Estados válidos (AVAILABLE, OCCUPIED, MAINTENANCE, RESERVED)
- ✓ Transiciones de estado permitidas

### Table Session Service
- ✓ Mesa debe existir
- ✓ Solo una sesión activa por mesa
- ✓ JWT válido y único
- ✓ Sesión expira en 4 horas
- ✓ Registro de apertura y cierre

---

## 🎓 Aprendizajes Clave

1. **Mocking con Mockito**: Uso de `@Mock`, `when()`, `verify()`
2. **Nested Tests**: Agrupación de tests relacionados con `@Nested`
3. **Display Names**: Descripción clara de qué prueba cada test
4. **Excepciones**: Validar que se lancen las excepciones correctas
5. **Transaccionalidad**: Tests simulan comportamiento transaccional

---

## 📞 Soporte

Si algún test falla:
1. Verificar que todas las dependencias están instaladas
2. Revisar los logs del test para el error específico
3. Validar que los DTOs y entidades están correctamente configurados
4. Confirmar que los mocks están configurados correctamente

---

**Última actualización**: Abril 2026
**Autor**: Equipo de Testing
**Versión**: 1.0

