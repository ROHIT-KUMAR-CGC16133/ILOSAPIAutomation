package testcases;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.util.HashMap;
import java.util.Map;

import static payloads.Header.getHeaders;

public class ObligationModule {

    String baseUrl = PropertiesReadWrite.getValue("baseURL");
    Map<String, Object> headers;
    Response response;
    String appId = PropertiesReadWrite.getValue("application_id");
    String url = baseUrl + "/ilos/v1/assignee/lead/" + PropertiesReadWrite.getValue("obj_id");

    @Test(priority = 1)
    public void LoanObligation() {
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        response = RestUtils.performGet(url, headers);
        validateResponse(response);
        String obligation_lead_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/obligation/lead/" + PropertiesReadWrite.getValue("obj_id");
        Map<String, Object> queryParams = Map.of("view", "true");
        Response obligation_lead_res = RestUtils.performGet(url, headers, queryParams);
        validateResponse(obligation_lead_res);


    }

    @Test(priority = 2)
    public void Obligation_Submit(){
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        String submit_url = baseUrl+"/ilos/v1/obligation/lead/submit/"+PropertiesReadWrite.getValue("obj_id");
        Map<String, Object> payload = Map.of("is_prm_app", true,"roi",10.11,"rt","SEMI-FIXED");
        Response submit_res = RestUtils.sendPatchRequest(submit_url,payload,headers);
        validateResponse(submit_res);

    }





    private void validateResponse(Response response) {
        if (response.getStatusCode() != 200) {
            response.prettyPrint();
            Assert.assertEquals(response.getStatusCode(), 200);
        }
    }

}
