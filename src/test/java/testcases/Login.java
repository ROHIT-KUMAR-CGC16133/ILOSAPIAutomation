package testcases;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import payloads.LoginPayload;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.net.StandardSocketOptions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Login {
    String baseUrl = "https://loanxpress-api.caprihomeloans.com";
    @Test(enabled = false)
    public void loginTestwithStringPayload(String username,String password) {
        String endPoint = baseUrl+"/ilosuser/v1/login";
        String payload = "{\n" +
                "    \"app\": \"XLX\",\n" +
                "    \"user\": \"rohit.kumar4@capriglobal.in\",\n" +
                "    \"pswd\": \"CgC16133@949$\",\n" +
                "    \"l_t\": \"capri_user\"\n" +
                "}";
        Response response = RestUtils.performPost(endPoint, payload, new HashMap<>());
        Assert.assertEquals(response.getStatusCode(), 200);
    }
    @Test(enabled = true)
    public void loginTestwithMapPayload() {
        String endPoint = PropertiesReadWrite.getValue("baseURL") + "/ilosuser/v1/login";

        Map<String, Object> payload = LoginPayload.getLoginPayloadMap(PropertiesReadWrite.getValue("CPUser"), PropertiesReadWrite.getValue("CPPassword"));
        Response response = RestUtils.performPost(endPoint, payload, new HashMap<>());
       // response.prettyPrint();
        Assert.assertEquals(response.getStatusCode(), 200);
        String tokenvalue =JsonPath.from(response.asString()).getString("dt.token");
        PropertiesReadWrite.setValue("token",tokenvalue);
        System.out.println("token "+tokenvalue);
    }
}
