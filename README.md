# 🧩 RoigMediaPollingComponent

**DI03 Media Net Polling JavaBean**

## 👤 Autor

**Elias Roig** (xesgan)  
CFGS DAM · Mòdul DI03 · Curs 2025–2026

## 📖 Descripción general

**RoigMediaPollingComponent** es un componente JavaBean reutilizable que permite conectar cualquier aplicación Swing con la plataforma **DI Media Net**.

**Proporciona:**
- Un motor de polling configurable
- Detección incremental de nuevos recursos multimedia
- Un sistema de eventos personalizado
- Métodos simplificados para subir, descargar y consultar media

El componente se comporta como un `JPanel` con icono, sin interfaz gráfica compleja, diseñado para integrarse fácilmente desde la **Palette de NetBeans**.

## 🧩 Funcionalidades principales

- ✔ Motor de polling basado en `javax.swing.Timer`
- ✔ Detección incremental usando `getMediaAddedSince(...)`
- ✔ Evento personalizado `MediaEvent` para nuevos archivos
- ✔ API wrapper interno (login, getNickName, upload, download, etc.)
- ✔ Gestión automática de `lastChecked` (ISO-8601 con offset)
- ✔ Control por propiedades (apiUrl, token, running, etc.)
- ✔ Integración directa desde la Palette de NetBeans
- ✔ Totalmente empaquetado como fat-jar (shade plugin)

## 🧱 Arquitectura del componente

El componente incluye:

### RoigMediaPollingComponent

- Hereda de `JPanel`
- Contiene un `Timer`, un `ApiClient` y listeners
- Expone propiedades y métodos wrapper
- Lanza eventos cuando detecta media nuevo

### MediaEvent

Clase que representa un evento de nuevos recursos multimedia.

### MediaListener

Interfaz que permite a cualquier clase reaccionar al evento.

### ApiClient (incrustado mediante wrapper)

Maneja todas las llamadas HTTP hacia la DI Media Net.

## 📡 Propiedades del componente

| Propiedad | Tipo | Descripción |
|-----------|------|-------------|
| `apiUrl` | `String` | URL base de la API (ej: `https://dimedianetapi9.azurewebsites.net`) |
| `token` | `String` | Token JWT generado por login |
| `running` | `boolean` | Inicia o pausa el polling |
| `pollingInterval` | `int` | Intervalo en segundos entre peticiones |
| `lastChecked` | `String` | Última fecha en ISO_OFFSET_DATE_TIME |

## 🧠 Métodos públicos (wrappers)

| Método | Descripción |
|--------|-------------|
| `login(email, password)` | Genera un JWT y actualiza la propiedad token |
| `getNickName(userId)` | Devuelve el nickname desde la API |
| `getAllMedia()` | Lista todos los media |
| `getMyMedia()` | Lista media del usuario logado |
| `getMediaByUser(userId)` | Lista media por ID de usuario |
| `uploadFileMultipart(file, url)` | Sube un archivo |
| `download(id, destFile)` | Descarga un archivo |

## 🔁 Polling y detección de media nuevo

El polling funciona así:

1. Cada `pollingInterval` segundos, el Timer ejecuta `checkServerForNewMedia()`
2. Se llama a:
```java
   apiClient.getMediaAddedSince(lastChecked, token)
```
3. Se comparan los IDs con `knownMediaIds`
4. Si hay nuevos → se lanza `fireNewMediaEvent(newItems)`
5. Se actualiza `lastChecked` con un `OffsetDateTime` en formato ISO

## 🎧 Sistema de eventos custom

### MediaEvent

**Incluye:**
- `List<Media> newMedia`
- `String discoveredAt` (ISO con offset)

### MediaListener

**Implementa:**
```java
void onNewMediaFound(MediaEvent event);
```

### Registro
```java
mediaPollingComponent.addMediaListener(evt -> {
    System.out.println("Nuevos media: " + evt.getNewMedia().size());
});
```

## 🔧 A) Instalación y configuración del componente

### 1. Añadir dependencia al proyecto principal

En NetBeans:

1. **Project → Dependencies → Add Dependency…**
2. Completar:
   - **Group Id**: `cat.dam.roig`
   - **Artifact Id**: `roigmediapollingcomponent`
   - **Version**: `1.0-SNAPSHOT`
   - **Scope**: `compile`
3. Luego:
   - **Right click** en la dependencia → **Manually Install Artifact…**
   - Seleccionar `target/roigmediapollingcomponent-1.0-SNAPSHOT.jar`

### 2. Añadirlo a la Palette

1. **NetBeans → Tools → Palette → Swing/AWT → Add from JAR…**
2. Seleccionar el jar sombreado
3. Elegir clase `RoigMediaPollingComponent`

### 3. Configurar desde el Designer

En la ventana de propiedades del componente:

| Propiedad | Valor |
|-----------|-------|
| `apiUrl` | `https://dimedianetapi9.azurewebsites.net` |
| `pollingInterval` | `10` |
| `running` | `false` |
| `token` | vacío (se llenará tras login) |

## 🧪 B) Instrucciones para uso dentro de un JFrame

**Ejemplo básico:**
```java
// login
String token = mediaPollingComponent1.login("email", "password");

// registrar listener
mediaPollingComponent1.addMediaListener(evt -> {
    System.out.println("Nuevos media: " + evt.getNewMedia().size());
});

// iniciar polling
mediaPollingComponent1.setRunning(true);
```

## 🛠 C) Instrucciones de compilación y empaquetado (shade plugin)

### 1. pom.xml
```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <version>3.2.4</version>
      <executions>
        <execution>
          <phase>package</phase>
          <goals><goal>shade</goal></goals>
          <configuration>
            <filters>
              <filter>
                <artifact>*:*</artifact>
                <excludes>
                  <exclude>META-INF/*.SF</exclude>
                  <exclude>META-INF/*.DSA</exclude>
                  <exclude>META-INF/*.RSA</exclude>
                </excludes>
              </filter>
            </filters>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

### 2. Generar el jar

1. **NetBeans → Run → Clean and Build**
2. El jar final está en:
```
   target/roigmediapollingcomponent-1.0-SNAPSHOT.jar
```
   (con dependencias incluidas)

## 🐞 Problemas encontrados y soluciones

| Problema | Solución |
|----------|----------|
| ❌ `NoClassDefFoundError: com.fasterxml.jackson…` | Usar shade plugin y reinstalar el jar |
| ❌ `getResource(...) == null` | Icono mal colocado → movido a `src/main/resources` |
| ❌ `DateTimeParseException` | Se usaba `LocalDateTime` → cambio a `OffsetDateTime` |
| ❌ No se detectaban nuevos media | Fecha sin offset → solucionado con `ISO_OFFSET_DATE_TIME` |

## 📚 Recursos utilizados

- **ChatGPT**: asistencia estructural y corrección de errores
- [Documentación oficial Java Timer](https://docs.oracle.com/javase/8/docs/api/javax/swing/Timer.html)
- [Documentación Jackson](https://github.com/FasterXML/jackson-databind)
- **StackOverflow**: ejemplos de shade plugin
- Apuntes oficiales DI03 Part 2 & Support Notes

---

## 🚀 Instalación rápida
```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/roigmediapollingcomponent.git

# Compilar con Maven
mvn clean package

# El JAR estará en target/roigmediapollingcomponent-1.0-SNAPSHOT.jar
```

## 📦 Dependencias principales

- Java Swing
- Jackson Databind
- HttpClient (Java 11+)
- Maven Shade Plugin

## 🎯 Casos de uso

- Aplicaciones de sincronización multimedia
- Sistemas de notificación en tiempo real
- Dashboards de monitorización de contenido
- Clientes desktop para plataformas de media sharing

## 📝 Licencia

Uso académico para DI03 — Elias Roig.
