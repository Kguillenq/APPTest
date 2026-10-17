# Libreta de notas — app de ejemplo para seguridad de contenedores

Aplicación de consola en Java (Maven). Guarda notas por usuario en la carpeta indicada por `DATA_DIR`.

Incluye, a propósito, la dependencia antigua `log4j 1.2.17` (con CVEs conocidos) para que las herramientas de escaneo tengan hallazgos reales.

## Archivos

| Archivo | Para qué sirve |
|---|---|
| `src/main/java/cl/demo/App.java` | La aplicación |
| `pom.xml` | Compilación con Maven y dependencia log4j 1.2.17 |
| `Dockerfile.inseguro` | "Antes": malas prácticas |
| `Dockerfile.seguro` | "Después": controles básicos aplicados |

## Paso a paso (cada paso = una captura de pantalla de evidencia)

### 1. Imagen insegura
```bash
docker build -f Dockerfile.inseguro -t libreta:inseguro .
docker run --rm -it libreta:inseguro
docker run --rm --entrypoint id libreta:inseguro      # muestra uid=0(root)
```

### 2. Escanear la imagen insegura
```bash
trivy image libreta:inseguro
trivy image --severity HIGH,CRITICAL libreta:inseguro
hadolint Dockerfile.inseguro
```

### 3. Imagen endurecida
```bash
docker build -f Dockerfile.seguro -t libreta:seguro .
docker run --rm -it libreta:seguro
docker run --rm --entrypoint id libreta:seguro        # muestra un usuario NO root
docker images | grep libreta                          # compara los tamaños
```

### 4. Escanear la imagen endurecida
```bash
trivy image libreta:seguro
trivy image --severity HIGH,CRITICAL libreta:seguro
hadolint Dockerfile.seguro
```

### 5. Control extra: que falle si hay vulnerabilidades críticas
```bash
trivy image --exit-code 1 --severity CRITICAL libreta:seguro
echo $?      # 0 = pasa, 1 = bloquea el despliegue
```

### 6. (Opcional) Ejecutar con más restricciones
```bash
docker run --rm -it --read-only --cap-drop ALL --security-opt no-new-privileges \
  -v libreta_datos:/app/datos libreta:seguro ana "mi primera nota"
```

## Controles que se demuestran

1. Imagen base mínima y con versión fija (no `latest`).
2. Build multi-etapa: la imagen final no lleva Maven ni el código fuente.
3. Ejecución con usuario sin privilegios (no root).
4. Escaneo de vulnerabilidades de la imagen (Trivy).
5. Revisión del Dockerfile (Hadolint).
6. Bloqueo del despliegue si hay vulnerabilidades críticas.
7. Restricciones en tiempo de ejecución (`--read-only`, `--cap-drop ALL`).

## Para bajar los hallazgos de log4j (opcional, "tercera versión")

Cambiar en `pom.xml` la dependencia `log4j:log4j:1.2.17` por una librería de logging actualizada (por ejemplo `org.slf4j:slf4j-simple`) y ajustar el logger en `App.java`. Volver a construir y escanear: los CVEs de la librería deberían desaparecer.
