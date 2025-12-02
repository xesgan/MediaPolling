package cat.dam.roig.roigmediapollingcomponent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EventObject;
import java.util.List;

/**
 * Evento personalizado que se dispara cuando el componente
 * detecta nuevos Media en la DI Media Net.
 * 
 * @author Elias Roig
 */
public class MediaEvent extends EventObject {
    
    private final List<Media> newMedia;
    private final String discoveredAt;
    
        /**
     * Crea un nuevo MediaEvent.
     *
     * @param source       el objeto que genera el evento (normalmente el componente)
     * @param newMedia     lista de Media nuevos detectados
     * @param discoveredAt fecha/hora en la que se detectaron (formato ISO)
     */
    public MediaEvent(Object source, List<Media> newMedia, String discoveredAt) {
        super(source);
        this.newMedia = List.copyOf(newMedia); // copia inmutable
        this.discoveredAt = discoveredAt;
    }
    
    /**
     * Lista de nuevos recursos Media detectados.
     */
    public List<Media> getNewMedia() {
        return newMedia;
    }

    /**
     * Fecha/hora de detección en formato ISO-8601.
     */
    public String getDiscoveredAt() {
        return discoveredAt;
    }

    /**
     * Helper opcional: devuelve discoveredAt como LocalDateTime.
     */
    public LocalDateTime getDiscoveredAtAsLocalDateTime() {
        return LocalDateTime.parse(discoveredAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
