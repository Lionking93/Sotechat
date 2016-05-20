package sotechat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

/**
 * Konfiguraatioluokka, joka määrittelee, että asiakasohjelmalta(clientiltä) tulleet viestit kulkevat
 * WebSocketin kautta palvelimelle ChatController-luokan käsiteltäväksi. Lisäksi luokka välittää ChatControllerin
 * generoiman vastauksen asiakasohjelmalle.
 * @since 19.5.2016
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

    /**
     * Metodi käyttää MessageBrokerRegistry-luokan metodia enableSimpleBroker välittääkseen WebSocketin kautta
     * asiakasohjelmalle viestin palvelimelta. Palvelimen viestit tulevat ChatController-luokalta.
     *
     * @param config Välittäjäolio, joka välittää viestit WebSocketin kautta palvelimen ChatController-luokalta
     * asiakasohjelmalle
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/toClient");
    }

    /**
     * Metodi käyttää StompEndpointRegistry-luokan metodia addEndpoint määrittääkseen asiakasohjelmalta WebSocketin
     * kautta tulleille viesteille pääteosoitteen. Tässä ohjelmassa viestit ohjautuvat ChatController-luokalle.
     *
     * @param registry Luokka, joka määrittää WebSocketin kautta tulleille asiakasohjelmien viesteille pääteosoitteen.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/toServer").withSockJS();
    }
}
