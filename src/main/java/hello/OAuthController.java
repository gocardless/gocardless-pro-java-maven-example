package hello;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@RestController
public class OAuthController {

//'https://connect-sandbox.gocardless.com/oauth/authorize?client_id=foo&redirect_uri=https%3A%2F%2Fbbda790a.ngrok.io%2Fdone&access_type=offline&response_type=code&initial_view=signup&scope=read_write'
    private static final String authoriseRedirectUrl =
            "https://connect-sandbox.gocardless.com/oauth/authorize";
    private static final String accessTokenUrl = 
            "https://connect-sandbox.gocardless.com/oauth/access_token";
    private static final String redirectUrl =  "https://bffa1cfd.ngrok.io/redirect";
        // Ngrok url here. See: https://ngrok.com/
    private static final String clientID = System.getenv("GOCARDLESS_CLIENT_ID");
    private static final String clientSecret = System.getenv("GOCARDLESS_CLIENT_SECRET");

    public static final MediaType URLENCODED =
           MediaType.parse("application/x-www-form-urlencoded");

    @RequestMapping("/")
    public ResponseEntity<String> index() throws UnsupportedEncodingException {
        //String encodedURL = URLEncoder.encode(redirectUrl, "UTF-8" );
        String oauthUrl = UriComponentsBuilder
          .fromUriString(authoriseRedirectUrl)
          .queryParam("client_id", clientID)
          .queryParam("redirect_uri", redirectUrl)
          .queryParam("scope", "read_write")
          .queryParam("response_type", "code")
          .queryParam("initial_view", "signup")
          .queryParam("access_type", "offline")
          .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", oauthUrl);
        return new ResponseEntity<String>(oauthUrl, headers, HttpStatus.FOUND);
    }

    private OkHttpClient client = new OkHttpClient();

    private String getAccessToken(String oauthCode) throws IOException {
        String encodedURL;
        //encodedURL = URLEncoder.encode(redirectUrl, "UTF-8" );
            //https://bb314345.ngrok.io/redirect
        String requestbody = UriComponentsBuilder
          .fromUriString(authoriseRedirectUrl)
          .queryParam("client_id", clientID)
          .queryParam("client_secret", clientSecret)
          .queryParam("redirect_uri", redirectUrl)
          .queryParam("scope", "read_write")
          .queryParam("grant_type", "authorization_code")
          .queryParam("code", oauthCode)
          .queryParam("initial_view", "signup")
          .queryParam("access_type", "offline")
          .toUriString();

        RequestBody body = RequestBody.create(URLENCODED, requestbody);
        Request request = new Request.Builder()
                .url("https://connect-sandbox.gocardless.com/oauth/access_token")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    @RequestMapping("/redirect")
    public String loggedIn(@RequestParam("code") String oauthCode) throws IOException {
        String accessToken = getAccessToken(oauthCode);
        System.out.println("Save the access token " +
                accessToken + "to your database.");
        return "Greetings!";
    }
}
