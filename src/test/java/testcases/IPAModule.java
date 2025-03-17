package testcases;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.util.HashMap;
import java.util.Map;

import static payloads.Header.getHeaders;

public class IPAModule {
    String baseUrl = PropertiesReadWrite.getValue("baseURL");
    Map<String, String> headers;
    @Test
    public void generateCibil() {
        String url = baseUrl+"/ilos/v1/ipa/lead/"+PropertiesReadWrite.getValue("obj_id");
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        Map<String, String> queryParams = Map.of("view", "true");
        Response IPA_lead_res = RestUtils.performGet(url, headers, queryParams);
       if(IPA_lead_res.getStatusCode()!=200){
           IPA_lead_res.prettyPrint();
           Assert.assertEquals(IPA_lead_res.getStatusCode(), 200);
       }
       String cibil_pdf_url = JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bureau_check.pdfUrl");

        String url1 = baseUrl+"/ilos/v1/ipa/cibil/"+PropertiesReadWrite.getValue("obj_id");
        Response cibil_res = RestUtils.performGet(url1, headers);
        if(cibil_res.getStatusCode()!=200){
            cibil_res.prettyPrint();
        }

    }

}
