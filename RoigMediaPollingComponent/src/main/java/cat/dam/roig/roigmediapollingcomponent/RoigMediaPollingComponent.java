package cat.dam.roig.roigmediapollingcomponent;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 *
 * @author metku
 */
public class RoigMediaPollingComponent extends JPanel implements Serializable {

    private String apiUrl;
    private boolean running;
    private int pollingInterval;
    private String token;
    private String lastChecked;
    private transient Timer pollingTimer;

    // Conjunto de IDs de media que el componente ya conoce
    private final Set<Integer> knownMediaIds = new HashSet<>();

    // ApiClient instancia
    private ApiClient apiClient;

    // Lista de objetos que recibiran eventos nuevos Media
    private final List<MediaListener> mediaListeners = new ArrayList<>();

    public static void main(String[] args) {

    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        boolean oldRunning = this.running;
        this.running = running;

        // Si el estado no ha cambiado no hacemos nada
        if (oldRunning == running) {
            return;
        }

        if (running) {
            // Esto asegura que la primera llamada a la API no envie null como fecha
            if (lastChecked == null || lastChecked.isBlank()) {
                updateLastChecked();
            }

            // Nos aseguramos de que el timer exista
            initTimer();
            if (!pollingTimer.isRunning()) {
                pollingTimer.start();
            } else {
                if (pollingTimer != null && pollingTimer.isRunning()) {
                    pollingTimer.stop();
                }
            }
        }
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;

        // Si ya tenemos un timer creado, actualizamos su delay
        if (pollingTimer != null) {
            int intervalSeconds = (pollingInterval > 0) ? pollingInterval : 10;
            pollingTimer.setDelay(intervalSeconds * 1000);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(String lastChecked) {
        this.lastChecked = lastChecked;
    }

    // ==== METODOS TIMER ====
    /**
     * Init timer solo se asegura de que el Timer exista,
     */
    private void initTimer() {
        // Evitamos recrear el Timer si ya existe
        if (pollingTimer != null) {
            return;
        }

        // Si el intervalo es menor o igual a 0, ponemos un valor por defecto ponemos por ejemplo 10s
        int intervalSeconds = (pollingInterval > 0) ? pollingInterval : 10;
        int delayMillis = intervalSeconds * 1000;

        pollingTimer = new Timer(delayMillis, (e) -> {
            // Aqui luego llamaremos al metodo que consultara el servidor
            checkServerForNewMedia();
        });
        pollingTimer.setRepeats(true);
    }

    private void checkServerForNewMedia() {
        // Implementar mas adelante
        // 1. Validaciones basicas
        if (!running) {
            return; // Si el componente no esta activo, no hacemos nada
        }
        if (token == null || token.isBlank()) {
            return; // No tenemos token valido, no podemos llamar a la api
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            return; // No tenemos url de la api
        }
        try {
            // 2. Aseguramos el tener ApiClient
            ensureApiClient();

            // 3. Llamamos a la API para obtener todos los media
            // Convertirmos lastChecked al formato esperado por la API (ISO-8601)
            String from = lastChecked;

            // 3.1 Pedimos solo lo nuevo
            List<Media> newFromServer = apiClient.getMediaAddedSince(from, token);

            if (newFromServer == null || newFromServer.isEmpty()) {
                updateLastChecked();
                return; // No hay nada nuevo
            }

            // 3.2 Detectamos realmente nuevos
            List<Media> newItems = new ArrayList<>();

            for (Media m : newFromServer) {
                int id = m.id;

                if (!knownMediaIds.contains(id)) {
                    knownMediaIds.add(id);
                    newItems.add(m);
                }

                // 3.3 Emitimos evento
                if (!newItems.isEmpty()) {
                    fireNewMediaEvent(newItems);
                }
            }

            // 5. Siguientes pasos:
            // - compararemos allMedia con knownMediaIds
            // - detectaremos cuales son nuevos
            // - actualizaremos kownmediaIds
            // - lanzaremos el evento si hay nuevos
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            // 4. Siempre actualizamos
            updateLastChecked();
        }
    }

    // ==== METODOS APICLIENT ====
    private void ensureApiClient() {
        if (apiClient != null) {
            return;
        }

        apiClient = new ApiClient(apiUrl);
    }

    private void updateLastChecked() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.lastChecked = now;
    }

    // ==== METODOS ADD/REMOVE LISTENERS ====
    /**
     * Registra un nuevo listener para eventos de nuevos Media.
     *
     * @param listener listener a registrar
     */
    public void addMediaListener(MediaListener listener) {

        if (listener == null) {
            return;  // No hay nadie escuchando
        }
        if (!mediaListeners.contains(listener)) {
            mediaListeners.add(listener);
        }
    }

    /**
     * Elimina un listener previamente registrado.
     *
     * @param listener listener a eliminar
     */
    public void removeMediaListener(MediaListener listener) {
        mediaListeners.remove(listener);
    }

    /**
     * Lanza un evento MediaEvent a todos los listeners registrados.
     *
     * @param newItems lista de nuevos Media detectados
     */
    protected void fireNewMediaEvent(List<Media> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            return;
        }

        if (mediaListeners.isEmpty()) {
            return;
        }

        // Fecha/hora actual en formato ISO-8601
        String discoverAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        MediaEvent event = new MediaEvent(this, newItems, discoverAt);

        // Hacemos una copia para evitar ConcurrentModificationException
        List<MediaListener> snapshot = new ArrayList<>(mediaListeners);

        for (MediaListener listener : snapshot) {
            try {
                listener.onNewMediaFound(event);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ==== METODOS WRAPPERS APICLIENT ====
    /**
     * Envuelve la llamada a ApiClient.login y actualiza la propiedad token.
     *
     * @param email email del usuario
     * @param password contraseña del usuario
     * @return el token JWT devuelto por la API
     * @throws Exception si falla el login
     */
    public String login(String email, String password) throws Exception {
        ensureApiClient();
        String jwt = apiClient.login(email, password);
        // Actualizamos la propiedad del componente
        setToken(jwt);
        return jwt;
    }

    /**
     * Devuelve el nickname de un usuario a partir de su id.
     *
     * @param userId id del usuario
     * @return nickname del usuario
     * @throws Exception si la llamada a la API falla
     */
    public String getNickName(int userId) throws Exception {
        ensureApiClient();
        return apiClient.getNickName(userId, token);
    }

    /**
     * Obtiene todos los recursos Media disponibles en la DI Media Net.
     *
     * @return lista de Media
     * @throws Exception si la llamada a la API falla
     */
    public List<Media> getAllMedia() throws Exception {
        ensureApiClient();
        return apiClient.getAllMedia(token);
    }

    /**
     * Descarga un recurso Media al fichero indicado.
     *
     * @param mediaId id del Media a descargar
     * @param destFile fichero de destino en disco
     * @throws Exception si la descarga falla
     */
    public void download(int mediaId, File destFile) throws Exception {
        ensureApiClient();
        apiClient.download(mediaId, destFile, token);
    }

    /**
     * Sube un fichero a la DI Media Net.
     *
     * @param file fichero a subir
     * @param downloadedFromUrl URL original desde donde se descargó (opcional)
     * @return respuesta de la API en formato String
     * @throws Exception si la subida falla
     */
    public String uploadFileMultipart(File file, String downloadedFromUrl) throws Exception {
        ensureApiClient();
        return apiClient.uploadFileMultipart(file, downloadedFromUrl, token);
    }
}
