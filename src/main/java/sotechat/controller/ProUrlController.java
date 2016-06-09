package sotechat.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/** Uudelleenohjaus polkuun /pro.
 */
@Controller
public class ProUrlController {

    /** Uudelleenohjaus /pro to /proCp.html .
     * @return proCP.html
     */
    @RequestMapping(value = "/pro")
    public final String pro() {
        return "forward:/proCP.html";
    }

}