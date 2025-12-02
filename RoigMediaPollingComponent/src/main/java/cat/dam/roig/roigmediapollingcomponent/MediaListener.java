package cat.dam.roig.roigmediapollingcomponent;

import java.util.EventListener;

/**
 * Listener para recibir notificaciones cuando el componente
 * detecta nuevos Media en la DI Media Net.
 * 
 * @author Elias Roig
 */
public interface MediaListener extends EventListener {
    
    /**
     * Se llama cuando el componente detecta nuevos recursos Media.
     *
     * @param event evento con la lista de nuevos Media y la fecha/hora de detección
     */
    void onNewMediaFound(MediaEvent event);
}
