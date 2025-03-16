package payloads;

import java.util.HashMap;
import java.util.Map;

public class Header {

    public static Map<String, String> getHeaders(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Authorization", token); // Ensure correct token format
        headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        return headers;
    }
}
