package sotechat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import sotechat.controller.MessageBroker;
import sotechat.data.Session;
import sotechat.data.SessionRepo;
import sotechat.service.QueueTimeoutService;

import java.security.Principal;

/**
 * Maarittelee mitä tapahtuu, kun <code>WebSocket</code>-yhteys katkeaa.
 *
 * @param <S> Abstrakti olio.
 */
@Component
public class WebSocketDisconnectHandler<S>
        implements ApplicationListener<SessionDisconnectEvent> {

    /**
     * <code>SessionRepo</code>-olio, jonka perusteella voidaan selvittaa, onko
     * tietty sessio aktiivinen vai inaktiivinen.
     */
    @Autowired
    private SessionRepo sessionRepo;

    /**
     * Olio, jonka vastuuseen kuuluu poistaa inaktiiviset kayttajat jonosta.
     */
    @Autowired
    private QueueTimeoutService queueTimeoutService;

    /**
     * Sanomien valittaja.
     */
    @Autowired
    private MessageBroker broker;

    /**
     * Konstruktori.
     */
    public WebSocketDisconnectHandler() {
    }

    /**
     * Toiminnot <code>WebSocket</code>-yhteyden katketessa.
     *
     * Asiakaskayttaja: kaynnistaa odotuksen, jonka jalkeen
     * kayttaja poistetaan jonosta, mikali kayttaja ei tule takaisin ennen sita.
     *
     * Ammattilaiskayttaja: merkitsee ammattilaiskayttajan "poissa" tilaan.
     *
     *
     * @param event Yhteydenkatkeamistapahtuma.
     */
    public final void onApplicationEvent(final SessionDisconnectEvent event) {
       MessageHeaders headers = event.getMessage().getHeaders();
       String sessionId = SimpMessageHeaderAccessor
               .getSessionAttributes(headers)
               .get("SPRING.SESSION.ID").toString();
       Principal professional = SimpMessageHeaderAccessor.getUser(headers);
       if (professional == null) {
           /* Asiakaskayttaja */
           this.queueTimeoutService
                   .initiateWaitBeforeScanningForInactiveUsers(
                           sessionId);
       } else {
           /* Ammattilaiskayttaja */
           String onlineStatus = "false";
           sessionRepo.setOnlineStatus(sessionId, onlineStatus);
           broker.sendJoinLeaveNotices(professional, onlineStatus);
           Session session = sessionRepo.getSessionFromSessionId(sessionId);
       }
        Session userSession = this.sessionRepo
                .getSessionFromSessionId(sessionId);
        if (userSession != null) {
            userSession.set("connectionStatus", "disconnected");
            broker.sendLeaveNotices(userSession);
        }

    }
}
