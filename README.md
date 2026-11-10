# DiviXpress — Plataforma Móvil de Cambio y Auditoría de Divisas

**DiviXpress** es una aplicación móvil nativa para Android desarrollada en **100% Kotlin** y **100% Jetpack Compose (Material 3)**, diseñada para la compra, venta y auditoría financiera de divisas en tiempo real bajo el patrón de arquitectura **MVVM estricto** y persistencia en la nube con **Firebase**.

---

## 1. Arquitectura y Stack Tecnológico

| Componente | Tecnología Implementada |
| :--- | :--- |
| **Nombre de la App** | `DiviXpress` |
| **Application ID / Package** | `com.samuel.retodivisas` |
| **Lenguaje** | **100% Kotlin** (cero archivos Java) |
| **Interfaz de Usuario (UI)** | **100% Jetpack Compose** con Material 3 (cero layouts XML) |
| **Patrón Arquitectónico** | **MVVM (Model - View - ViewModel)** con `StateFlow` y `Flow` reactivos (`collectAsStateWithLifecycle`) |
| **Consumo de API REST** | **Finnhub API** (`https://finnhub.io/api/v1/`) vía **Retrofit 2 + Gson** |
| **Autenticación y Base de Datos** | **Firebase Authentication** + **Cloud Firestore** (transacciones atómicas `runTransaction`) |
| **Multimedia en la Nube** | **Firebase Storage** + **Coil Compose** (`AsyncImage`) |
| **Integración de Hardware** | Cámara nativa y Motor Háptico (`Vibrator` / `VibratorManager`) |

### Estructura de Capas (MVVM)
- **`data/`**: Modelos de dominio (`AppUser`, `TransactionModel`, `AuditRule`), gestor de sesión y repositorios (`AuthRepository`, `FirestoreTransactionRepository`, `RatesRepository`, `MediaRepository`).
- **`network/`**: Cliente y servicio REST para cotizaciones bursátiles en vivo (`FinnhubApiService`, `FinnhubClient`).
- **`viewmodel/`**: Gestión de estado reactivo y lógica de presentación (`AuthViewModel`, `OperationsViewModel`, `AdminAuditViewModel`, `ProfileViewModel`, `HardwareViewModel`).
- **`ui/screens/` y `ui/theme/`**: Pantallas declarativas en Jetpack Compose y sistema de diseño oscuro financiero (`RetoDivisasTheme`).
- **`hardware/`**: Abstracciones para permisos/captura de cámara (`CameraHelper`) y retroalimentación háptica diferenciada (`VibrationHelper`).

---

## 2. Flujo de Pantallas y Roles de Usuario

La aplicación implementa navegación declarativa (`Navigation Compose`) con diferenciación por rol (**Usuario Tradicional** y **Administrador**):

1. **Autenticación (`LoginScreen` y `RegisterScreen`)**
   - Inicio de sesión y registro con cuentas reales en **Firebase Authentication**.
   - Selector de perfil al registrarse (**Usuario Tradicional** o **Administrador**) con asignación automática de capital inicial de `$10,000.00 USD` en Cloud Firestore.
2. **Mesa de Compra de Divisas (`BuyCurrencyScreen`)**
   - Consulta en tiempo real de la cotización oficial en **Finnhub** para los pares `USD/COP`, `USD/EUR`, `USD/GBP` y `USD/JPY`.
   - Ingreso de monto en USD y tasa pactada, con cálculo instantáneo del equivalente en moneda extranjera y proyección del nuevo saldo (`+USD`).
   - Adjunto opcional de comprobante fotográfico desde **Cámara** o **Galería**.
3. **Mesa de Venta de Divisas (`SellCurrencyScreen`)**
   - Validación en tiempo real de fondos disponibles (bloqueo preventivo si el monto supera el balance en USD) y proyección del saldo restante (`-USD`).
4. **Perfil de Usuario (`ProfileScreen`)**
   - Visualización de correo, rol activo, balance actualizado en tiempo real y actualización de foto de perfil con subida a Firebase Storage y renderizado con Coil.
5. **Panel de Auditoría del Administrador (`AdminAuditScreen`)**
   - Vista exclusiva para cuentas con rol `ADMIN`.
   - Tarjetas KPI en tiempo real (*Total*, *En Norma ≤ 2.5%* y *En Alerta > 2.5%*).
   - Filtros dinámicos y listado global de transacciones de todos los usuarios sincronizado desde Firestore.
6. **Diagnóstico de Hardware (`HardwareScreen`)**
   - Módulo de verificación y prueba del motor háptico y captura de cámara del dispositivo.

---

## 3. Regla de Negocio Crítica: Auditoría del 2.5%

En el instante exacto en que un usuario confirma una compra o venta, la aplicación consulta la cotización actual de mercado en **Finnhub (`marketRate`)** y la compara con la tasa pactada por el usuario (**`agreedRate`**):

$$\text{Desfase (\%)} = \frac{|\text{agreedRate} - \text{marketRate}|}{\text{marketRate}} \times 100$$

- **Operación Normal ($\le 2.5\%$):** Se procesa con confirmación háptica estándar y estado `Verificada`.
- **Operación en Alerta ($> 2.5\%$):** Si la desviación supera estrictamente el **2.5%**, la transacción se marca automáticamente con `requiresAudit = true`, dispara un patrón de vibración de alerta, notifica al usuario mediante diálogo y se resalta en rojo en el **Panel de Auditoría** del Administrador.

---

## 4. Autor e Información Académica

- **Autor:** Samuel Pérez (`@SamuPV05`)
- **Curso:** Programación Móvil I
- **Institución:** Universidad Católica Luis Amigó
