# 📋 FRONTEND INTEGRATION CONTRACT - QUICK REFERENCE

Este es un documento resumido y rápido. Para detalles completos, ver: `FRONTEND_INTEGRATION_CONTRACT.md`

---

## 🎯 El Endpoint

```
POST /api/auth/login
```

**Base URL:**
- Desarrollo: `http://localhost:8081`
- Producción: `https://api.servly.com`

---

## 📤 QUÉ ENVIAR (Request)

```json
{
  "email": "usuario@example.com",
  "password": "MiPassword123!",
  "recaptchaToken": "03AOLTBLTVEy5C3...token_de_google..."
}
```

**Notas:**
- Email: válido, no puede estar vacío
- Password: 8+ caracteres
- recaptchaToken: obtenido de `grecaptcha.getResponse()`

---

## 📥 QUÉ RECIBIR (Success 200 OK)

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "usuario@example.com",
  "name": "Juan Pérez",
  "roles": ["STAFF", "ADMIN"],
  "mustChangePassword": false,
  "message": null
}
```

**Qué hacer:**
```typescript
// Guardar tokens
sessionStorage.setItem('accessToken', response.token);
sessionStorage.setItem('refreshToken', response.refreshToken);

// Redirigir según rol
if (response.roles.includes('ADMIN')) {
  this.router.navigate(['/admin']);
} else {
  this.router.navigate(['/staff']);
}
```

---

## ⚠️ ERRORES POSIBLES

| Code | Causa | Qué hacer |
|------|-------|-----------|
| **400** | reCAPTCHA inválido | Resetear: `grecaptcha.reset()` |
| **401** | Email/pass incorrectos | Mostrar error, limpiar password |
| **403** | Cuenta deshabilitada | Mostrar error, contactar soporte |
| **422** | Validación fallida | Mostrar errores de campo |
| **500** | Error servidor | Reintentar o contactar soporte |

---

## 🔐 USAR EL TOKEN EN REQUESTS POSTERIORES

```
Header: Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Ejemplo HTTP:**
```http
GET /api/staff/tables HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json
```

**Ejemplo TypeScript/Fetch:**
```typescript
const token = sessionStorage.getItem('accessToken');
fetch('/api/staff/tables', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

**Ejemplo Angular HttpClient:**
```typescript
const headers = new HttpHeaders({
  'Authorization': `Bearer ${token}`
});
this.http.get('/api/staff/tables', { headers });
```

---

## 🔄 CUANDO EL TOKEN EXPIRE (401)

```typescript
// 1. Obtener refresh token
const refreshToken = sessionStorage.getItem('refreshToken');

// 2. Renovar
POST /api/auth/refresh-token
{
  "refreshToken": "..."
}

// 3. Guardar nuevo token
// Recibir nueva respuesta LoginResponse
sessionStorage.setItem('accessToken', response.token);

// 4. Reintentar request original
GET /api/staff/tables
Authorization: Bearer {nuevo-token}
```

---

## 📲 INTEGRACIÓN CON HTML

```html
<!-- Cargar reCAPTCHA -->
<script src="https://www.google.com/recaptcha/api.js"></script>

<!-- Formulario -->
<form (ngSubmit)="login()">
  <input [(ngModel)]="email" type="email" placeholder="Email">
  <input [(ngModel)]="password" type="password" placeholder="Contraseña">
  
  <!-- Widget reCAPTCHA -->
  <div class="g-recaptcha" 
       data-sitekey="YOUR_SITE_KEY"
       #recaptcha>
  </div>
  
  <button type="submit">Iniciar Sesión</button>
</form>
```

---

## 💻 INTEGRACIÓN CON ANGULAR

```typescript
import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

declare var grecaptcha: any;

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent {
  email = '';
  password = '';
  errorMessage = '';

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login() {
    // 1. Obtener token del reCAPTCHA
    const recaptchaToken = grecaptcha.getResponse();
    if (!recaptchaToken) {
      this.errorMessage = 'Por favor completa el reCAPTCHA';
      return;
    }

    // 2. Enviar al backend
    this.http.post('/api/auth/login', {
      email: this.email,
      password: this.password,
      recaptchaToken
    }).subscribe({
      next: (response: any) => {
        // 3. Guardar tokens
        sessionStorage.setItem('accessToken', response.token);
        sessionStorage.setItem('refreshToken', response.refreshToken);

        // 4. Redirigir
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        // 5. Manejar errores
        if (error.status === 401) {
          this.errorMessage = 'Email o contraseña incorrectos';
        } else if (error.status === 400) {
          this.errorMessage = 'reCAPTCHA inválido. Intenta de nuevo.';
        } else {
          this.errorMessage = 'Error al iniciar sesión';
        }
        grecaptcha.reset();
      }
    });
  }
}
```

---

## 🔧 ANGULAR HTTP INTERCEPTOR

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler } from '@angular/common/http';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = sessionStorage.getItem('accessToken');
    
    if (token) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }

    return next.handle(req);
  }
}
```

**Registrar en AppModule:**
```typescript
import { HTTP_INTERCEPTORS } from '@angular/common/http';

@NgModule({
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ]
})
export class AppModule { }
```

---

## 🧪 TESTING CON cURL

```bash
# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"admin@example.com",
    "password":"AdminPassword123!",
    "recaptchaToken":"test_token"
  }'

# Guardar token
TOKEN=$(curl -s -X POST ... | jq -r '.token')

# Usar en endpoint protegido
curl -X GET http://localhost:8081/api/staff/tables \
  -H "Authorization: Bearer $TOKEN"
```

---

## ✅ CHECKLIST

- [ ] Importar `HttpClientModule` en AppModule
- [ ] Crear LoginComponent
- [ ] Crear AuthService
- [ ] Crear JwtInterceptor
- [ ] Implementar login form HTML
- [ ] Agregar reCAPTCHA v2 widget
- [ ] Probar login exitoso
- [ ] Probar errores (401, 400, etc)
- [ ] Guardar tokens en sessionStorage
- [ ] Incluir token en requests posteriores
- [ ] Manejar renovación de token (401)
- [ ] Proteger rutas con AuthGuard

---

## 📞 SOPORTE

**Documento completo:** `FRONTEND_INTEGRATION_CONTRACT.md`  
**Ejemplos cURL:** `TESTING_GUIDE_CURL.md`  
**Código Angular:** `EJEMPLO_ANGULAR_SERVICES.ts`

---

## 🚀 LISTO

Todo lo necesario está documentado. El backend está listo. Ahora es turno del frontend.

**Fecha:** Marzo 5, 2026  
**Estado:** ✅ Producción

