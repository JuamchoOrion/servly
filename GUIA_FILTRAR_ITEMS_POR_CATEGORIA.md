# 📋 FILTRAR ITEMS POR CATEGORÍA

## ✅ ¿ESTÁ IMPLEMENTADO?

**SÍ.** La funcionalidad ya existe en el backend. Tienes **2 endpoints listos** para usar.

---

## 🔍 ENDPOINTS DISPONIBLES

### **OPCIÓN 1: Sin Paginación**

```http
GET /api/items/category/{categoryId}
Authorization: Bearer {token}
```

**Parámetros:**
- `categoryId`: ID de la categoría (requerido)

**Respuesta:**
```json
[
  {
    "id": 1,
    "name": "Arroz Blanco",
    "description": "Arroz blanco de grano largo",
    "unitOfMeasurement": "kg",
    "expirationDays": 365,
    "categoryId": 11,
    "active": true
  }
]
```

✅ **Uso:** Cuando quieres todos los items de una categoría sin límite

---

### **OPCIÓN 2: Con Paginación (RECOMENDADO)**

```http
GET /api/items/category-paginated/{categoryId}?page=0&size=10&sort=name,asc
Authorization: Bearer {token}
```

**Parámetros:**
- `categoryId`: ID de la categoría (requerido)
- `page`: Número de página (opcional, default: 0)
- `size`: Items por página (opcional, default: 10)
- `sort`: Ordenamiento (opcional, default: id,asc)

**Respuesta:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Arroz Blanco",
      "description": "Arroz blanco de grano largo",
      "unitOfMeasurement": "kg",
      "expirationDays": 365,
      "categoryId": 11,
      "active": true
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 25,
  "totalPages": 3,
  "last": false
}
```

✅ **Uso:** Cuando tienes muchos items (mejor rendimiento)

---

## 📊 CATEGORÍAS DISPONIBLES

| ID | Categoría | Descripción |
|----|-----------|-------------|
| 11 | Granos | Arroz, trigo, avena, quinua |
| 14 | Bebidas | Agua, jugo, bebidas |
| 15 | Carnes | Pollo, res, cerdo, pescado |
| 17 | Lácteos | Leche, queso, yogurt |
| 19 | Verduras | Tomate, lechuga, zanahoria |
| 20 | Frutas | Manzana, plátano, naranja |
| 21 | Condimentos | Sal, pimienta, azúcar |
| 22 | Aceites | Aceite de oliva, girasol |

---

## 🚀 EJEMPLOS DE USO

### Ejemplo 1: Obtener todos los Lácteos

```http
GET http://localhost:8081/api/items/category/17
Authorization: Bearer {token}
```

### Ejemplo 2: Obtener Carnes (1ª página)

```http
GET http://localhost:8081/api/items/category-paginated/15?page=0&size=10
Authorization: Bearer {token}
```

### Ejemplo 3: Obtener Verduras (2ª página, 5 items)

```http
GET http://localhost:8081/api/items/category-paginated/19?page=1&size=5
Authorization: Bearer {token}
```

### Ejemplo 4: Obtener Granos ordenados por nombre

```http
GET http://localhost:8081/api/items/category-paginated/11?sort=name,asc
Authorization: Bearer {token}
```

### Ejemplo 5: Obtener Aceites ordenados por ID descendente

```http
GET http://localhost:8081/api/items/category-paginated/22?sort=id,desc&size=20
Authorization: Bearer {token}
```

---

## 💻 CÓDIGO FRONTEND

### Angular - Servicio

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ItemService {
  private baseUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  /**
   * Obtiene todos los items de una categoría
   */
  getItemsByCategory(categoryId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.baseUrl}/items/category/${categoryId}`
    );
  }

  /**
   * Obtiene items de una categoría con paginación
   */
  getItemsByCategoryPaginated(
    categoryId: number,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<any>(
      `${this.baseUrl}/items/category-paginated/${categoryId}`,
      { params }
    );
  }
}
```

### Angular - Componente

```typescript
import { Component, OnInit } from '@angular/core';
import { ItemService } from './services/item.service';

@Component({
  selector: 'app-items-by-category',
  templateUrl: './items-by-category.component.html',
  styleUrls: ['./items-by-category.component.css']
})
export class ItemsByCategoryComponent implements OnInit {
  items: any[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 10;
  
  selectedCategoryId = 11; // Granos

  constructor(private itemService: ItemService) {}

  ngOnInit() {
    this.loadItems();
  }

  loadItems() {
    this.itemService.getItemsByCategoryPaginated(
      this.selectedCategoryId,
      this.currentPage,
      this.pageSize
    ).subscribe({
      next: (data) => {
        this.items = data.content;
        this.totalElements = data.totalElements;
        this.totalPages = data.totalPages;
      },
      error: (err) => console.error(err)
    });
  }

  selectCategory(categoryId: number) {
    this.selectedCategoryId = categoryId;
    this.currentPage = 0; // Volver a 1ª página
    this.loadItems();
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadItems();
    }
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadItems();
    }
  }
}
```

### JavaScript vanilla

```javascript
// Obtener items de una categoría
const categoryId = 11;
const token = 'tu_token_jwt';

fetch(`http://localhost:8081/api/items/category-paginated/${categoryId}`, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(data => {
  console.log('Items:', data.content);
  console.log('Total:', data.totalElements);
  console.log('Página:', data.pageNumber + 1, 'de', data.totalPages);
});
```

---

## 🎯 PARÁMETROS DE PAGINACIÓN

### Parámetro: `page`
- **Descripción:** Número de página (0-indexed)
- **Default:** 0
- **Ejemplo:** `?page=1` (segunda página)

### Parámetro: `size`
- **Descripción:** Items por página
- **Default:** 10
- **Ejemplo:** `?size=20` (20 items por página)

### Parámetro: `sort`
- **Descripción:** Ordenamiento
- **Formato:** `field,direction`
- **Campos:** id, name, description, expirationDays, active
- **Direcciones:** asc (ascendente), desc (descendente)
- **Ejemplo:** `?sort=name,asc` (A-Z)

### Combinaciones

```
?page=0&size=10
?page=1&size=20&sort=name,asc
?size=50&sort=id,desc
?page=2 (usa size default)
?sort=name,desc
```

---

## 📝 RESPUESTA CON PAGINACIÓN

Campo | Descripción
------|-----
`content` | Array de items en esta página
`pageNumber` | Número de página (0-indexed)
`pageSize` | Items por página
`totalElements` | Total de items en la categoría
`totalPages` | Total de páginas
`last` | ¿Es la última página?

---

## 📁 ARCHIVO DE PRUEBA

Abre: `test-filtrar-items-por-categoria.http`

Contiene ejemplos listos para ejecutar en JetBrains IDE.

---

## ✅ CHECKLIST

- [ ] Abrí el archivo `test-filtrar-items-por-categoria.http`
- [ ] Ejecuté `GET /api/items/category/11`
- [ ] Ejecuté `GET /api/items/category-paginated/11`
- [ ] Probé con diferentes parámetros
- [ ] Integré en mi frontend
- [ ] Probé con diferentes categorías

---

## 🎉 LISTO PARA USAR

Los endpoints ya existen y funcionan. Solo necesitas:

1. Usar el ID correcto de categoría
2. Proporcionar el token JWT
3. Procesar la respuesta JSON

¡Adelante! 🚀

