package generic;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import payloads.LoginPayload;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class Generic {

    public static String getToken(String user,String password){
        String tokenvalue =null;
        String endPoint = PropertiesReadWrite.getValue("baseURL") +"/ilosuser/v1/login";
        Map<String, Object> payload = LoginPayload.getLoginPayloadMap(user, password);
        Response response = RestUtils.performPost(endPoint, payload, new HashMap<>());
        Assert.assertEquals(response.getStatusCode(), 200);
        tokenvalue = JsonPath.from(response.asString()).getString("dt.token");
      //  PropertiesReadWrite.setValue("token",tokenvalue);
        return tokenvalue;
    }

    public static void validateResponse(Response response) {
        if (response.getStatusCode() != 200) {
            response.prettyPrint();
            Assert.assertEquals(response.getStatusCode(), 200);
        }
    }

    public static String generate_upload_url_and_upload_file(String fn, String app_id, String ft, String ext, String dsn,Map<String, Object> headers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("dsn", dsn);
        payload.put("fn", fn);
        payload.put("app_id", app_id);
        payload.put("ext", ext);
        payload.put("ft", ft);
        Response generate_upload_res = RestUtils.performPost(PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/misc/generate-upload-url", payload, headers);
        validateResponse(generate_upload_res);
        String Content_Type = generate_upload_res.jsonPath().getString("fld.Content-Type");
        String key = generate_upload_res.jsonPath().getString("fld.key");
        String x_amz_algorithm = generate_upload_res.jsonPath().getString("fld.x-amz-algorithm");
        String x_amz_credential = generate_upload_res.jsonPath().getString("fld.x-amz-credential");
        String x_amz_date = generate_upload_res.jsonPath().getString("fld.x-amz-date");
        String x_amz_security_token = generate_upload_res.jsonPath().getString("fld.x-amz-security-token");
        String policy = generate_upload_res.jsonPath().getString("fld.policy");
        String x_amz_signature = generate_upload_res.jsonPath().getString("fld.x-amz-signature");
        String ky   = generate_upload_res.jsonPath().getString("ky");
        String aws_url = generate_upload_res.jsonPath().getString("url");
        File file = new File(System.getProperty("user.dir") + "/src/main/resources/file/bankstatment.pdf");
        RestAssured.baseURI = aws_url;
        Response response_awsupload = given()
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                .header("Connection", "keep-alive")
                .header("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryzPVefwfwzmwlm3EE")
                .multiPart("Content-Type", Content_Type)  // Content type for the uploaded file
                .multiPart("key", key)  // Key for file in the S3 bucket
                .multiPart("x-amz-algorithm", x_amz_algorithm)
                .multiPart("x-amz-credential", x_amz_credential)
                .multiPart("x-amz-date", x_amz_date)
                .multiPart("x-amz-security-token", x_amz_security_token)
                .multiPart("policy", policy)
                .multiPart("x-amz-signature", x_amz_signature)
                .multiPart("file", file, "application/pdf")
                .when()
                .post()
                .then() // Validate success response
                .extract()
                .response();
        response_awsupload.getStatusCode();
        return ky;
    }


}
