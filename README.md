<div align="center">

<img src="study.png" alt="StudyBuddy" width="96" height="96" />

# StudyBuddy

Aplicación de escritorio para gestionar notas de estudio con una interfaz clara y soporte opcional de IA (Groq) para generar resúmenes, palabras clave y ejercicios.

</div>

## Descripción
- Autenticación simple por usuario con contraseñas hash `SHA-256`.
- Notas privadas y públicas, con edición rápida y filtrado en tiempo real.
- Integración opcional de IA para acelerar el estudio (resumen, claves y ejercicios).
- Persistencia local en JSON, con migración automática desde `.dat` si existe.

## Tecnologías Usadas
- Java SE (Swing, AWT, `HttpURLConnection`, `MessageDigest`).
- Interfaz gráfica en `Swing` con estilos personalizados.
- Groq API (modelo `llama-3.1-8b-instant`) para funcionalidades de IA.
- Formato de datos: JSON local en `data/*.json`.

## Requisitos
- Java 11+ (recomendado por compatibilidad de APIs usadas).
- Internet solo si utilizas la IA.

## Instrucciones de Ejecución
Compila y ejecuta desde la raíz del proyecto:

```bash
# Opción A: salida de clases en la misma raíz
javac -d . java/studybuddy/StudyBuddy.java
java studybuddy.StudyBuddy

# Opción B: salida en directorio dedicado
mkdir -p java/bin
javac -d java/bin java/studybuddy/StudyBuddy.java
java -cp java/bin studybuddy.StudyBuddy
```

### Configuración de IA (Opcional)
- Define la clave como variable de entorno:
  - Windows PowerShell: `$env:GROQ_API_KEY = "<tu-clave>"`
  - Linux/macOS: `export GROQ_API_KEY="<tu-clave>"`
- Alternativa: propiedad del sistema Java `-Dgroq.api.key=<tu-clave>`.

## Roles de Cada Integrante
Actualiza esta tabla con nombres y responsabilidades reales.

| Integrante | Rol | Responsabilidades |
|---|---|---|
| Sebastián Camero | Líder del proyecto | Coordinación, arquitectura, entregables y Integración Groq|
| Juan David Giraldo | Lógica/IA |  utilidades, persistencia |
| Manuela Posso | UI/UX y QA/Docs | Interfaz Swing, usabilidad, pruebas y documentación |

## Transparencia sobre uso de IA
Consulta `Transparencia-IA-StudyBuddy.txt` para conocer qué partes fueron asistidas por IA y cómo se verificó el funcionamiento.

---


