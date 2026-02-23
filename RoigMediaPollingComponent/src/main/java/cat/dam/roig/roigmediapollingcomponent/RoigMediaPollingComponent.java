package cat.dam.roig.roigmediapollingcomponent;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * <h2>Componente JavaBean para realizar polling contra la DI Media Net y
 * detectar nuevos Media.</h2>
 *
 * <h3>Funciones principales:</h3>
 * <ul>
 * <li>Login y gestión interna del token JWT.</li>
 * <li>Polling periódico con <code>javax.swing.Timer</code>.</li>
 * <li>Consulta incremental usando
 * <code>getMediaAddedSince(lastChecked)</code>.</li>
 * <li>Registro de IDs conocidos para evitar notificar duplicados.</li>
 * <li>Emisión de eventos <code>MediaEvent</code> mediante
 * <code>MediaListener</code>.</li>
 * <li>Wrappers simplificados de ApiClient (login, nickname, upload,
 * download…).</li>
 * </ul>
 *
 * <h3>Uso básico:</h3>
 * <ol>
 * <li>Agregar el componente al formulario desde la Palette.</li>
 * <li>Configurar <code>apiUrl</code> y <code>pollingInterval</code>.</li>
 * <li>Hacer login desde código (el token se guarda automáticamente).</li>
 * <li>Registrar listeners: <code>addMediaListener(...)</code>.</li>
 * <li>Activar polling: <code>setRunning(true)</code>.</li>
 * </ol>
 *
 * <h3>Notas:</h3>
 * <ul>
 * <li>El Timer solo funciona si <code>running = true</code>.</li>
 * <li><code>lastChecked</code> usa formato ISO_OFFSET_DATE_TIME.</li>
 * <li><code>ApiClient</code> se inicializa automáticamente (lazy).</li>
 * <li><code>knownMediaIds</code> evita eventos repetidos.</li>
 * <li>El icono se carga desde <code>/images/poller.png</code>.</li>
 * </ul>
 */
public class RoigMediaPollingComponent extends JPanel implements Serializable {

    private String apiUrl;
    private boolean running;
    private int pollingInterval;
    private String token;
    private String lastChecked;
    private transient Timer pollingTimer;

    // IDs ya notificados
    private final Set<Integer> knownMediaIds = new HashSet<>();

    // Cliente hacia la API
    private ApiClient apiClient;

    // Listeners registrados
    private final List<MediaListener> mediaListeners = new ArrayList<>();

    // ===================== CONSTRUCTOR =====================
    public RoigMediaPollingComponent() {
        super();
        initLayoutAndIcon();

        if (pollingInterval <= 0) {
            pollingInterval = 10;
        }
        if (lastChecked == null) {
            updateLastChecked();
        }
    }

    // ===================== GETTERS / SETTERS =====================
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
        boolean prev = this.running;
        this.running = running;

        if (prev == running) {
            return;
        }

        if (running) {
            if (lastChecked == null || lastChecked.isBlank()) {
                updateLastChecked();
            }
            initTimer();
            if (!pollingTimer.isRunning()) {
                pollingTimer.start();
            }
        } else {
            if (pollingTimer != null && pollingTimer.isRunning()) {
                pollingTimer.stop();
            }
        }
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
        if (pollingTimer != null) {
            int seconds = pollingInterval > 0 ? pollingInterval : 10;
            pollingTimer.setDelay(seconds * 1000);
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

    // ===================== ICONO =====================
    private void initLayoutAndIcon() {
        setLayout(new java.awt.BorderLayout());
        javax.swing.JLabel label = new javax.swing.JLabel();
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        try {
            var iconUrl = getClass().getResource("/images/poller.png");
            if (iconUrl != null) {
                label.setIcon(new javax.swing.ImageIcon(iconUrl));
            } else {
                System.err.println("No se encontró /images/poller.png");
                label.setText("MediaPoller");
            }
        } catch (Exception ex) {
            System.err.println("Error cargando icono: " + ex);
            label.setText("MediaPoller");
        }

        add(label, java.awt.BorderLayout.CENTER);
    }

    // ===================== TIMER =====================
    /**
     * Crea el Timer si no existe.
     */
    private void initTimer() {
        if (pollingTimer != null) {
            return;
        }

        int seconds = (pollingInterval > 0) ? pollingInterval : 10;
        pollingTimer = new Timer(seconds * 1000, e -> checkServerForNewMedia());
        pollingTimer.setRepeats(true);
    }

    // ===================== POLLING =====================
    /**
     * Método llamado periódicamente por el Timer. Obtiene media nuevos y lanza
     * el evento si procede.
     */
    private void checkServerForNewMedia() {
        if (!running) {
            return;
        }
        if (token == null || token.isBlank()) {
            return;
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            return;
        }

        System.out.println("[POLL] tick ");

        try {
            ensureApiClient();

            List<Media> server = apiClient.getMediaAddedSince(lastChecked, token);

            if (server == null || server.isEmpty()) {
                updateLastChecked();
                return;
            }

            List<Media> fresh = new ArrayList<>();

            for (Media m : server) {
                int id = m.id; // o m.getId() si tu clase lo tiene
                if (!knownMediaIds.contains(id)) {
                    knownMediaIds.add(id);
                    fresh.add(m);
                }
            }

            if (!fresh.isEmpty()) {
                fireNewMediaEvent(fresh);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            updateLastChecked();
        }
    }

    // ===================== API CLIENT =====================
    /**
     * Inicializa ApiClient si aún no existe.
     */
    private void ensureApiClient() {
        if (apiClient == null) {
            apiClient = new ApiClient(apiUrl);
        }
    }

    /**
     * Actualiza lastChecked con la hora actual (UTC) en ISO_OFFSET_DATE_TIME.
     */
    private void updateLastChecked() {
        this.lastChecked = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    // ===================== EVENTOS =====================
    public void addMediaListener(MediaListener l) {
        if (l != null && !mediaListeners.contains(l)) {
            mediaListeners.add(l);
        }
    }

    public void removeMediaListener(MediaListener l) {
        mediaListeners.remove(l);
    }

    /**
     * Notifica a los listeners que se ha detectado nuevo media.
     */
    protected void fireNewMediaEvent(List<Media> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            return;
        }
        if (mediaListeners.isEmpty()) {
            return;
        }

        String ts = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        MediaEvent evt = new MediaEvent(this, newItems, ts);

        for (MediaListener ml : new ArrayList<>(mediaListeners)) {
            try {
                ml.onNewMediaFound(evt);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ===================== WRAPPERS PÚBLICOS =====================
    public String login(String email, String password) throws Exception {
        ensureApiClient();
        String jwt = apiClient.login(email, password);
        setToken(jwt);
        return jwt;
    }

    public String getNickName(int userId) throws Exception {
        ensureApiClient();
        return apiClient.getNickName(userId, token);
    }

    public List<Media> getAllMedia() throws Exception {
        ensureApiClient();
        return apiClient.getAllMedia(token);
    }

    public void download(int mediaId, File destFile) throws Exception {
        ensureApiClient();
        apiClient.download(mediaId, destFile, token);
    }

    public String uploadFileMultipart(File f, String fromUrl) throws Exception {
        ensureApiClient();
        return apiClient.uploadFileMultipart(f, fromUrl, token);
    }
}
