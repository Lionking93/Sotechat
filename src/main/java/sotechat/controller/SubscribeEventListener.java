package sotechat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import sotechat.data.ChatLogger;
import sotechat.data.Mapper;
import sotechat.data.Session;
import sotechat.data.SessionRepo;

import java.util.*;
import static sotechat.config.StaticVariables.QUEUE_BROADCAST_CHANNEL;

/** Kuuntelee WebSocket subscribe/unsubscribe -tapahtumia
 *  - pitaa kirjaa, ketka kuuntelevat mitakin kanavaa.
 *  - kun joku subscribaa QBCC kanavalle, pyytaa QueueBroadcasteria castaamaan.
 *  HUOM: Spring hajoaa, jos kaytetaan Autowired konstruktoria tassa luokassa!
 */
@Component
public class SubscribeEventListener
        implements ApplicationListener<ApplicationEvent> {

    /** Session Repository. */
    @Autowired
    private SessionRepo sessionRepo;

    /** Queue Broadcaster. */
    @Autowired
    private QueueBroadcaster queueBroadcaster;

    /** Taikoo viestien lahetyksen. */
    @Autowired
    private SimpMessagingTemplate broker;

    /** Chat Logger (broadcastaa). */
    @Autowired
    private ChatLogger chatLogger;

    /** Mapper. */
    @Autowired
    private Mapper mapper;

    /** Siirtaa tehtavat "kasittele sub" ja "kasittele unsub" oikeille
     * metodeille. Timeria kaytetaan, jotta subscribe -tapahtuma ehditaan
     * suorittamaan loppuun ennen mahdollisia broadcasteja. Ilman timeria
     * kay usein niin, etta juuri subscribannut kayttaja ei saa broadcastia.
     * @param applicationEvent kaikki applikaatioEventit aktivoivat taman.
     */
    @Override
    public final void onApplicationEvent(
            final ApplicationEvent applicationEvent
    ) {

            /** Ei kaynnisteta turhia timereita muista applikaatioeventeista. */
            if (applicationEvent.getClass() != SessionSubscribeEvent.class
                    && applicationEvent.getClass() != SessionUnsubscribeEvent.class) {
                return;
            }

            /** Kaynnistetaan timer, joka kasittelee eventin, kunhan
             * subscribe-tapahtuma on suoritettu loppuun. */
            Timer timer = new Timer();
            int delay = 1; // milliseconds
            timer.schedule(new TimerTask() {
                @Override
                public void run() {        try {
                    delayedEventHandling(applicationEvent);
                }catch(Exception e){

                }
                }
            }, delay);
    }

    /** Timerin avulla kutsuttu metodi, joka vain
     * haarauttaa sub/unsub pyynnot oikeaan metodiin.
     * @param applicationEvent appEvent
     */
    private void delayedEventHandling(
            final ApplicationEvent applicationEvent
    ) throws Exception {
        if (applicationEvent.getClass() == SessionSubscribeEvent.class) {
            handleSubscribe((SessionSubscribeEvent) applicationEvent);
        } else if
                (applicationEvent.getClass() == SessionUnsubscribeEvent.class) {
            handleUnsubscribe((SessionUnsubscribeEvent) applicationEvent);
        }
    }

    /** Kasittelee subscribe -tapahtumat.
     * @param event event
     */
    private synchronized void handleSubscribe(
            final SessionSubscribeEvent event
    ) throws Exception {
        MessageHeaders headers = event.getMessage().getHeaders();

        /** Interceptor estaa subscribet, joista puuttuu sessionId.
         * Siksi allaoleva ei voi heittaa nullpointteria. */
        String sessionId = SimpMessageHeaderAccessor
                .getSessionAttributes(headers)
                .get("SPRING.SESSION.ID").toString();

        String channelIdWithPath = SimpMessageHeaderAccessor
                .getDestination(headers);
        Session session = sessionRepo.getSessionObj(sessionId);

        System.out.println("Subscribing someone to " + channelIdWithPath);
        if (channelIdWithPath.isEmpty()) {
            return;
        }

        /** Add session to list of subscribers to channelId. */
        mapper.addSessionToChannel(channelIdWithPath, session);

        /** Jos subscribattu QBCC (jonotiedotuskanava), broadcastataan jono. */
        String qbcc = "/toClient/" + QUEUE_BROADCAST_CHANNEL;
        if (channelIdWithPath.equals(qbcc)) {
            queueBroadcaster.broadcastQueue();
        }

        /** Jos subscribattu /chat/kanavalle, lahetetaan kanavan
         * viestihistoria kaikille kanavan subscribaajille. */
        String chatPrefix = "/toClient/chat/";
        if (channelIdWithPath.startsWith(chatPrefix)) {
            String channelId = channelIdWithPath.substring(chatPrefix.length());
            chatLogger.broadcast(channelId, broker);
        }
    }

    /** TODO: Kasittelee unsubscribe -tapahtumat.
     * @param event event
     */
    private synchronized void handleUnsubscribe(
            final SessionUnsubscribeEvent event
    ) {
        System.out.println("UNSUB = " + event.toString());
    }

    /** Vaaditaan dependency injektion toimimiseen tassa tapauksessa.
     * @param repo repo
     */
    private synchronized void setSessionRepo(final SessionRepo repo) {
        this.sessionRepo = repo;
    }

    /** Vaaditaan dependency injektion toimimiseen tassa tapauksessa.
     *  @return SessionRepo sessionRepo
     * */
    private synchronized SessionRepo getSessionRepo() {
        return this.sessionRepo;
    }
}
