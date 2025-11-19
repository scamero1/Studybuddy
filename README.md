# StudyBuddy

Aplicación de escritorio en Java para gestionar notas de estudio con soporte opcional de IA (Groq) para resumen, palabras clave y ejercicios. Incluye autenticación básica por usuario, notas privadas/públicas y persistencia local en JSON.

## Características
- Inicio de sesión y registro con hash `SHA-256`.
- CRUD de notas, selector y editor con interfaz `Swing`.
- Marcar notas como públicas y ver notas públicas de todos.
- IA opcional: resumen, palabras clave y ejercicios usando Groq (`llama-3.3-70b-versatile`).
- Persistencia local en `data/*.json` y migración automática desde `.dat` si existe.

## Requisitos
- Java 8+ (se recomienda Java 11 o superior).
- Conexión a internet solo si se usa la IA.
- Para IA: recomendar usar variable de entorno `GROQ_API_KEY` (no guardar la clave en el repo).

## Ejecución
Desde la raíz del repositorio:

```bash
javac -d java/bin java/studybuddy/StudyBuddy.java
java -cp java/bin studybuddy.StudyBuddy
```

## Pruebas
El proyecto trae pruebas básicas de funcionalidad:

```bash
javac -d java/bin java/studybuddy/StudyBuddy.java java/studybuddy/StudyBuddyTests.java
java -cp java/bin studybuddy.StudyBuddyTests
```

## Tecnologías usadas
- Java SE (`Swing`, `AWT`, `java.net.HttpURLConnection`, `MessageDigest`).
- Persistencia local en JSON con utilidades propias.
- Groq API (chat completions) para funciones de IA.

## Integración con datos/nube
- Datos locales en `data/users.json`, `data/notes_<usuario>.json` y `data/public_notes.json`.
- IA en la nube (Groq) si se configura la clave mediante `GROQ_API_KEY`, propiedad `-Dgroq.api.key` o archivos soportados (`groq.key`, `keys/groq.key`, `../groq.key`, `../keys/groq.key`).

## Configuración de IA
- Windows PowerShell (solo en la sesión actual):
  - `$env:GROQ_API_KEY = "<tu-clave>"`
- Windows PowerShell (persistente para el usuario):
  - `[Environment]::SetEnvironmentVariable("GROQ_API_KEY", "<tu-clave>", "User")`
- Linux/macOS (temporal):
  - `export GROQ_API_KEY="<tu-clave>"`
- Alternativa: propiedad del sistema Java `-Dgroq.api.key=<tu-clave>` al ejecutar.

### Ejemplos de ejecución con IA
- Variable de entorno:
  - `$env:GROQ_API_KEY = "<tu-clave>"`
  - `java -cp java/bin studybuddy.StudyBuddy`
- Propiedad del sistema:
  - `java -Dgroq.api.key=<tu-clave> -cp java/bin studybuddy.StudyBuddy`

### CI/CD (GitHub Actions)
- Definir `GROQ_API_KEY` en `Repository secrets` y exponerlo como variable del job:
  - `env:\n    GROQ_API_KEY: ${{ secrets.GROQ_API_KEY }}`
- Ejecutar pruebas: `java -cp java/bin studybuddy.StudyBuddyTests`

## Roles del equipo
- Responsable 1 — descripción de contribución
- Responsable 2 — descripción de contribución
- Responsable 3 — descripción de contribución

Edite esta sección con los nombres y responsabilidades reales del grupo.

## Transparencia sobre uso de IA
Consulte `Transparencia-IA-StudyBuddy.txt` para detalles de qué partes fueron asistidas por IA, qué modificaciones humanas se aplicaron y cómo se verificó el funcionamiento.


## Criterios de evaluación (guía)
- Funcionalidad y cumplimiento técnico: autenticación, CRUD, persistencia y IA opcional.
- Diseño algorítmico y POO: clases `Note`, `User`, `UserStore`, `NoteStore`, `AiService` y `GroqAiService`.
- Interfaz gráfica y usabilidad: `StudyBuddyFrame` en `Swing`, tema y acciones claras.
- Integración con datos/nube: JSON local y Groq para IA.
- Pruebas y documentación: `StudyBuddyTests` y este README.
- Colaboración y GitHub: mantener historial de commits y, si es posible, PRs.


