# Ejecutable de Callista

Esta carpeta contiene lo necesario para convertir Callista en un **programa
convencional**: un instalador que pone la aplicación en *Aplicaciones* (macOS) o
en el menú Inicio (Windows) con su propio icono. Quien lo instale **no necesita
tener Java**, porque el instalador incluye un Java reducido.

```
packaging/
├── README.md            ← este archivo
├── GenIcons.java        ← genera los íconos a partir del diseño PNG
├── macos/Callista.icns  ← icono de la app en macOS
└── windows/Callista.ico ← icono del programa en Windows
```

## Requisitos

- **JDK 25** (el mismo del proyecto). Trae la herramienta `jpackage`, que es la que
  crea el ejecutable; no hay que instalar nada más en macOS.
- **Windows:** para crear el instalador `.exe` hace falta además el
  [WiX Toolset](https://wixtoolset.org) (gratuito). Para la versión sin instalador no.

## Crear el ejecutable

Desde la raíz del proyecto:

```bash
./gradlew jpackage                          # instalador del sistema actual
./gradlew jpackage -PpackageType=app-image  # solo la aplicación, sin instalador
```

En Windows se usa `gradlew.bat jpackage`. El resultado queda en `build/jpackage/`:

| Sistema | `./gradlew jpackage` | `-PpackageType=app-image` |
|---|---|---|
| macOS | `Callista-1.0.0.dmg` (~31 MB) | `Callista.app` |
| Windows | `Callista-1.0.0.exe` (crea acceso directo y entrada en el menú Inicio) | carpeta `Callista/` con `Callista.exe` |

También se aceptan `-PpackageType=pkg` (macOS) y `-PpackageType=msi` (Windows).

> **Importante:** `jpackage` solo empaqueta para el sistema en el que se ejecuta.
> El `.dmg` se genera en una Mac y el `.exe` en una PC con Windows.

## Instalar y abrir

- **macOS:** abrir el `.dmg` y arrastrar Callista a *Aplicaciones*. Como la app no
  está firmada con una cuenta de desarrollador de Apple, la primera vez macOS puede
  bloquearla: clic derecho sobre Callista → **Abrir** (o *Ajustes del Sistema →
  Privacidad y seguridad → Abrir de todos modos*).
- **Windows:** ejecutar el `.exe` y seguir el asistente. Windows SmartScreen puede
  avisar que el programa no es conocido: *Más información → Ejecutar de todas formas*.

## Cómo funciona

1. La tarea `installDist` de Gradle compila el proyecto y deja en
   `build/install/Callista/lib/` el `.jar` de Callista junto con sus librerías
   (FlatLaf y el runtime de formularios de IntelliJ).
2. La tarea `jpackage` (definida en `build.gradle`) llama a la herramienta `jpackage`
   del JDK con estas opciones principales:

   | Opción | Para qué |
   |---|---|
   | `--input`, `--main-jar`, `--main-class` | Qué ejecutar: `com.callista.Main` y sus librerías. |
   | `--add-modules java.base,java.desktop,java.prefs` | Incluye solo las partes de Java que usa el programa (calculadas con `jdeps`), para que pese menos. |
   | `--java-options --enable-native-access=ALL-UNNAMED` | La misma opción que usa `./gradlew run`; FlatLaf carga una librería nativa. |
   | `--icon` | `macos/Callista.icns` o `windows/Callista.ico`, según el sistema. |
   | `--app-version 1.0.0` | macOS no acepta versiones que empiecen en 0 (el proyecto está en 0.1.0). |
   | `--win-menu --win-shortcut --win-dir-chooser` | Solo Windows: entrada en el menú Inicio, acceso directo y elegir carpeta. |

## El icono

El diseño original está en `src/main/resources/icon/callista.png` (PNG cuadrado).
Cada sistema pide un formato distinto, así que `GenIcons.java` genera el resto:

| Archivo | Uso |
|---|---|
| `src/main/resources/icon/callista.png` | Diseño original. Icono de la ventana y de la barra de tareas. |
| `src/main/resources/icon/callista-macos.png` | Versión con la forma de los íconos de macOS: cuadro redondeado de 824 px centrado en un lienzo de 1024 px con margen transparente. Así no se ve más grande que los demás íconos del Dock. |
| `packaging/macos/Callista.icns` | Icono del `.app`, en todos los tamaños de 16 a 1024 px. |
| `packaging/windows/Callista.ico` | Icono del `.exe`, de 16 a 256 px (diseño cuadrado). |

Además, `MainFrame.installIcon()` pone el icono en la ventana y en el Dock, para que
también se vea al ejecutar desde IntelliJ o con `./gradlew run`.

### Cambiar el icono

1. Reemplazar `src/main/resources/icon/callista.png` por el nuevo diseño
   (cuadrado, idealmente de 1024×1024 px o más).
2. Desde la raíz del proyecto, en una Mac (usa la herramienta `iconutil` de macOS):

   ```bash
   java packaging/GenIcons.java
   ```

3. Volver a crear el ejecutable con `./gradlew jpackage`.
