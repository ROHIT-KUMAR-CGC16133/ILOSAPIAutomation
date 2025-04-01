package payloads;

import java.util.Map;
import java.util.Objects;

public class LoginPayload {
    public static String getLoginPayload(String username, String password) {
        return "{\n" +
                "    \"app\": \"XLX\",\n" +
                "    \"user\": \"rohit.kumar4@capriglobal.in\",\n" +
                "    \"pswd\": \"CgC16133@949$\",\n" +
                "    \"l_t\": \"capri_user\"\n" +
                "}";
    }

    public static Map<String, Object> getLoginPayloadMap(String username, String password) {
        return Map.of(
                "app", "XLX",
                "user", username,
                "pswd", password,
                "l_t", "capri_user"
        );
    }

    public static Map<String, Object> getLoginPayloadMapVendor(String username, String password) {
        return Map.of(
                "user", username,
                "pswd", password,
                "l_t", "vendor"
        );
    }
}
