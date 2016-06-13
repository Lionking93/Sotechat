package sotechat.domainService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sotechat.domain.Conversation;
import sotechat.domain.Message;
import sotechat.domain.Person;
import sotechat.repo.ConversationRepo;
import sotechat.repo.PersonRepo;

import java.util.Date;
import java.util.List;

/**
 * Luokka tietokannassa olevien keskustelujen hallinnoimiseen
 * (CRUD -operaatiot)
 * Created by varkoi on 8.6.2016.
 */
@Service
public class ConversationService {

    /** Keskustelujen tallentamiseen */
    private ConversationRepo conversationRepo;

    @Autowired
    /** Konstruktorissa injektoidaan ConversationRepo ja Personrepo */
    public ConversationService(ConversationRepo conversationRepo,
                               PersonRepo personRepo) {
        this.conversationRepo = conversationRepo;
    }

    /**
     * Lisaa uuden keskustelun tietokantaan, jolle asetetaan aikaleima
     * ja tunnukseksi parametrina annettu kanavaid. Taman jalkeen lisataan
     * keskusteluun parametrina annettu viesti
     * @param message Message luokan olio, jossa kayttajan lahettama viesti
     * @param channelId keskustelun kanavan id
     */
    @Transactional
    public void addConversation(Message message, String channelId)
            throws Exception {
            Conversation conv = new Conversation(new Date(), channelId);
            conversationRepo.save(conv);
            addMessage(message, conv);
    }

    /**
     * Lisää parametrina annettua kanavaid:tä vastaavaan keskusteluun
     * parametrina annetun Person luokan olion. Henkilo lisataan Keskustelun
     * henkiloihin.
     * @param person Person luokan oli, joka halutaan lisata keskusteluun
     * @param channelId keskustelun kanavan id
     * @throws Exception IllegalArgumentException
     */
    public void addPerson(Person person, String channelId)
            throws Exception {
                Conversation conv = conversationRepo.findOne(channelId);
                conv.addPersonToConversation(person);
                conversationRepo.save(conv);
    }

    /**
     * Liittaa parametrina annettulla kanavaid:lla tietokannasta loytyvan
     * keskustelun kategoriaksi parametrina annetun aihealueen
     * @param category keskustelun aihealue
     * @param channelId keskustelun kanavan id
     * @throws Exception IllegalArgumentException
     */
    public void setCategory(String category, String channelId) throws Exception {
            Conversation conv = conversationRepo.findOne(channelId);
            conv.setCategory(category);
            conversationRepo.save(conv);
    }

    /**
     * Lisaa viestin keskusteluun, eli liittaa parametrina annetun Message
     * -olion parametrina annetun kanavaid:n perusteella tietokannasta
     * loytyvan Convertasion -olion listaan.
     * @param message Message luokan olio, jossa kayttajan lahettama viesti
     * @param ChannelId keskustelun kanavan id
     * @return true, jos vietsin lisaaminen tietokantaan onnistui, false jos ei
     * @throws Exception IllegalArgumentException
     */
    public void addMessage(Message message, String ChannelId)
            throws Exception {
            Conversation conv = conversationRepo.findOne(ChannelId);
            addMessage(message, conv);
    }

    /**
     * Lisaa parametrina annetun Message -luokan olion parametrina annetun
     * Conversation -olion listaan, ts liittaa viestin keskusteluun.
     * @param message Message -luokan olio, jossa on kayttajan viesti
     * @param conv Conversation -luokan oli, joka edustaa keskustelua, johon
     *             viesti liittyy
     * @throws Exception NullPointerException
     */
    @Transactional
    private void addMessage(Message message, Conversation conv)
            throws Exception {
            conv.addMessageToConversation(message);
            conversationRepo.save(conv);
    }

    /**
     * Poistaa keskustelusta viestin ts. poistaa parametrina annetun Message
     * olion sen muuttujista loytyvan Conversation olion listasta ja paivittaa
     * muutoksen tietokantaan.
     * @param message Viesti joka halutaan poistaa (taytyy etsia ensin
     *                messageServicesta)
     */
    @Transactional
    public void removeMessage(Message message){
        String channelId = message.getConversation().getChannelId();
        Conversation conv = conversationRepo.findOne(channelId);
        conv.getMessagesOfConversation().remove(message);
        conversationRepo.save(conv);
    }

    /**
     * Poistaa keskustelun tietokannasta ts. poistaa parametrina annettua
     * kanavaid:ta vastaavaa keskustelua edustavan Conversation luokan olion
     * tietokannasta.
     * @param channelId keskustelun kanavan id
     * @throws Exception IllegalArgumentException
     */
    @Transactional
    public void delete(String channelId) throws Exception {
        conversationRepo.delete(channelId);
    }

    /**
     * Palauttaa parametrina annettua channel id:tä vastaavan keskustelun
     * @param channelId haetun keskustelun kanavaid
     * @return Conversation olio, jolla pyydetty kanavaid
     */
    public Conversation getConversation(String channelId){
        return conversationRepo.findOne(channelId);
    }

}
