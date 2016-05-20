package sotechat.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sotechat.JoinResponse;
import sotechat.MsgToClient;
import sotechat.MsgToServer;

import java.util.Date;
import java.util.Random;

/**
 * Kontrolleriluokka, joka käsittelee asiakasohjelman ja palvelimen väliset chattiin liittyvät viestit.
 *
 * @since 19.5.2016
 */
@RestController
public class ChatController {

    /**
     * Metodi, joka käsittelee /toServer/{id}-polun kautta tulleet asiakasohjelman viestit, ja
     * lähettää asiakasohjelmalle vastauksen.
     *
     * @param message Asiakasohjelman lähettämä viesti, jonka sisältö on paketoitu MsgToServer-olion sisälle
     * @return Palauttaa MsgToClient-olion, joka on palvelimen lähettämä viesti asiakasohjelmalle. Olion sisältö
     * muokataan JSON-muotoon Springin Jackson-kirjaston avulla ennen kuin asiakasohjelma vastaanottaa viestin.
     * @throws Exception
     */
    @MessageMapping("/toServer/{id}")
        @SendTo("/toClient/{id}")
        public MsgToClient greeting(MsgToServer message) throws Exception {
            String username = "Anon";
            String timeStamp = new Date().toString();
            return new MsgToClient(username, message.getChannelId(), timeStamp, message.getContent());
        }

    @RequestMapping("/join")
        public JoinResponse returnJoinResponse() throws Exception {
            Random rand = new Random();
            String username = "Anon" + rand.nextInt(500);
            String userId = "" + rand.nextInt(1000);
            String channel = "" + rand.nextInt(2);
            return new JoinResponse(username, userId, channel);
        }
}

