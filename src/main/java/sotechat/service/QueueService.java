package sotechat.service;

import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import sotechat.data.ChatLogger;
import sotechat.data.Mapper;
import sotechat.data.Session;
import sotechat.data.SessionRepo;
import sotechat.wrappers.MsgToServer;
import sotechat.wrappers.QueueItem;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Tarjoaa palvelut jonoon lisaamiseen, jonosta
 * poistamiseen ja jonon tarkasteluun.
 * Tata Servicea ei tarvitse synkronisoida, koska queue on jo synkronisoitu.
 */
@Service
public class QueueService {

    /** Jonottajia kuvaavat oliot sailotaan tanne. */
    private List<QueueItem> queue;

    /** Mapper. */
    @Autowired
    private Mapper mapper;

    /** Session Repo. */
    @Autowired
    private SessionRepo sessionRepo;

    /** Database Service. */
    @Autowired
    private DatabaseService databaseService;

    /** Chat Logger. */
    @Autowired
    private ChatLogger chatLogger;

    /** Konstruktori.
     */
  //  @Autowired
    public QueueService() {
        this.queue = new ArrayList<>();
    }

    /** Suoritetaan kayttajan pyynto tulla jonoon (oletettavasti validoitu jo).
     * @param request req
     * @param payload payload
     */
    public final synchronized void joinPool(
            final HttpServletRequest request,
            final JsonObject payload
    ) {
        /** Alustetaan muuttujia. */
        String sessionId = request.getSession().getId();

        Session session = sessionRepo.getSessionObj(sessionId);

        String userId = session.get("userId");
        String channelId = session.get("channelId");
        String category = session.get("category");
        String username = payload.get("username").getAsString();
        String startMessage = payload.get("startMessage").getAsString();

        /** Muistetaan kayttajan valitsema nimimerkki. */
        session.set("username", username);

        /** Kirjataan kayttajalle oikeus kuunnella kanavaa. */
        session.addChannel(channelId);

        /** Mapataan nimimerkki ja userId. */
        mapper.mapUsernameToId(userId, username);

        /** Asetetaan kayttaja jonoon odottamaan palvelua. */
        QueueItem item = new QueueItem(channelId, category, username);
        queue.add(item);
        session.set("state", "queue");

        /** Luodaan tietokantaan uusi keskustelu. */
        try {
            databaseService.createConversation(username, channelId, category);
        } catch (Exception e) {
            System.out.println("Continuing despite db error(1)" + e.toString());
        }

        /** Luodaan aloitusviestista msgToServer-olio. */
        MsgToServer msgToServer = new MsgToServer();
        msgToServer.setUserId(userId);
        msgToServer.setChannelId(channelId);
        msgToServer.setContent(startMessage);

        /** Kirjataan viesti lokeihin, mutta ei laheteta sita viela, koska
         * kanavalla ei ole ketaan. Kun kanavalle liittyy joku,
         * sille lahetetaan lokit. */
        chatLogger.logNewMessage(msgToServer);
    }

    /** Suoritetaan jonosta nostaminen (oletettavasti validoitu jo).
     * @param channelId kanavaId
     * @param accessor taalta autentikaatiotiedot
     * @return tyhja String jos poppaus epaonnistuu,
     *          JSON {"content":"channel activated."} jos poppaus onnistuu.
     */
    public final synchronized String popQueue(
            final String channelId,
            final SimpMessageHeaderAccessor accessor
    ) {
        if (!removeFromQueue(channelId)) {
            /** Poppaus epaonnistui. Ehtiko joku muu popata samaan aikaan? */
            return "";
        }
        String sessionId =  accessor.getSessionAttributes()
                .get("SPRING.SESSION.ID").toString();
        Session session = sessionRepo.getSessionObj(sessionId);

        /** Lisataan popattu kanava poppaajan kanaviin. */
        session.addChannel(channelId);

        /** Muutetaan popattavan kanavan henkiloiden tilaa. */
        changeParticipantsState(channelId);

        /** Lisätään poppaaja tietokannassa olevaan keskusteluun */
        try {
            databaseService.addPersonToConversation(
                    session.get("userId"), channelId);
        } catch (Exception e) {
            System.out.println("Continuing despite db error(1)" + e.toString());
        }

        /** Onnistui, palautetaan JSONi. */
        return "{\"content\":\"channel activated.\"}";
    }

    /** Poistaa jonosta alkion, jonka channelId sama kuin parametrissa.
     * @param channelId haettu channelId
     * @return true jos poisto onnistui, fail jos alkiota ei loytynyt.
     */
    private boolean removeFromQueue(
            final String channelId
    ) {
        /** Etsitaan jonosta oikea alkio. */
        for (int i = 0; i < queue.size(); i++) {
            QueueItem item = queue.get(i);
            if (item.getChannelId().equals(channelId)) {
                /** Loytyi, poistetaan. */
                queue.remove(i);
                return true;
            }
        }
        /** Ei loytynyt. */
        return false;
    }

    /** Muokataan popattavan kanavan sessioiden tilaksi "chat".
     * @param channelId kanavan id
     */
    private void changeParticipantsState(
            final String channelId
    ) {
        String channelIdWithPath = "/toClient/queue/" + channelId;
        Set<Session> list = mapper.getSubscribers(channelIdWithPath);
        for (Session member : list) {
            /** Hoitajan tilan kuuluu aina olla "pro". */
            if (!member.get("state").equals("pro")) {
                member.set("state", "chat");
            }
        }
    }

    /** Palauttaa parametrina annettua kanavaid:ta vastaavaa alkiota
     * edeltavan jonon pituuden parametrina annetussa kategoriassa.
     * @param channelId kanavaid, jota vastaavaa alkiota edeltävän jonon pituus
     *                  halutaan selvittää
     * @param category aihealue, jonka alkiot otetaan laskussa mukaan
     * @return sijainti jonossa, kyseisen kategorian alla, alkaen ykkosesta.
     * jos haettua alkiota ei loydy, palauttaa -1.
     */
    public final int getPositionInQueue(
            final String channelId,
            final String category
    ) {
        int countItemsOfSameCategory = 1;
        for (int i = 0; i < queue.size(); i++) {
            QueueItem item = queue.get(i);
            if (item.getChannelId().equals(channelId)) {
                return countItemsOfSameCategory;
            }
            if (item.getCategory().equals(category)) {
                countItemsOfSameCategory++;
            }
        }
        return -1;
    }

    /** Palauttaa jonon Stringina, joka nayttaa JSON-ystavalliselta taulukolta.
     * Esim: {"jono": [{"channelId": "xyz", "category": "1", "username": "Ra"}]}
     * @return string
     */
    @Override
    public final String toString() {
        StringBuilder output = new StringBuilder("{\"jono\": [");
        for (int i = 0; i < queue.size(); i++) {
            QueueItem item = queue.get(i);
            if (i > 0) {
                output.append(", ");
            }
            output.append(item.toString());
        }
        output.append("]}");
        return output.toString();
    }
}
