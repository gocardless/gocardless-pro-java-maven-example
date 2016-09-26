package hello;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import org.apache.commons.codec.digest.HmacUtils;
import com.google.gson.*;

import java.util.List;
import com.gocardless.resources.Event;
import static com.gocardless.resources.Event.ResourceType.MANDATES;

@RestController
public class HelloController {

    private boolean isValidSignature(String signature, String payload) {
        String secret = System.getenv("GC_WEBHOOK_SECRET");
        String computedSignature = HmacUtils.hmacSha256Hex(secret, payload).toString();
        System.out.println(computedSignature);
        System.out.println(signature);
        return computedSignature.equals(signature);
    }

    private String processMandate(Event event){
        switch (event.getAction()) {
            case "cancelled":
                return "Mandate " + event.getLinks().getMandate() + "has been cancelled\n";
            default:
                return "Do not know how to process an event with action " + event.getAction() + ".";
        }
    }

    private String processEvent(Event event){
        switch (event.getResourceType()) {
            case MANDATES:
                return processMandate(event);
            default:
                return "Do not know how to process an event with resource_type " + event.getResourceType().toString();
        }
    }

    class WebhookPayload {
       private List<Event> events;

        public List<Event> getEvents() {
            return events;
        }
    }

    @PostMapping("/")
    public ResponseEntity <String> handlePost(
            @RequestHeader("Webhook-Signature") String signature,
            @RequestBody String payload) {
        if (isValidSignature(signature, payload)) {
            String responseBody = "";
            WebhookPayload webhookPayload = new Gson().fromJson(payload, WebhookPayload.class);
            for (Event event: webhookPayload.getEvents()){
                responseBody += processEvent(event);
            }
            return new ResponseEntity<String>(responseBody, HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("Incorrect Signature", HttpStatus.BAD_REQUEST);
        }
    }
}
