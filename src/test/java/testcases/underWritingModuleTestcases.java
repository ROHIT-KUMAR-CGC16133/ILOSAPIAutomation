package testcases;

import generic.Generic;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.util.HashMap;
import java.util.Map;

import static payloads.Header.getHeaders;

public class underWritingModuleTestcases {

    Response leadJsonResponse=null;
    String baseUrl =  PropertiesReadWrite.getValue("baseURL");
    String CPuser = PropertiesReadWrite.getValue("CPUser");
    String CPpassword = PropertiesReadWrite.getValue("CPPassword");

@Test(enabled = true,priority = 1)
public void getAllOpenLead(){
    Response response=null;
    Map<String, Object> headers;
    String endPoint = PropertiesReadWrite.getValue("baseURL") +"/ilos/v1/underwriter/lead";

    Map<String, Object> queryParams = new HashMap<>();
    queryParams.put("status", "PENDING_FOR_DDE");
    String token = PropertiesReadWrite.getValue("token");

    headers = getHeaders(CPuser,CPpassword); // Get headers with the token
     response = RestUtils.performGet(endPoint, headers, queryParams);
    validateResponse(response);

   int leadcount = JsonPath.from(response.asString()).getInt("tct");
    if(leadcount>0){
        PropertiesReadWrite.setValue("obj_id", JsonPath.from(response.asString()).getString("dt[0]._id"));
        PropertiesReadWrite.setValue("application_id", JsonPath.from(response.asString()).getString("dt[0].ap_no"));
    }

    System.out.println("all open lead listing");

}
@Test(enabled = true,priority = 2)
    public void assignLeadToCP(){
    String url = PropertiesReadWrite.getValue("baseURL")+"/ilos/v1/underwriter/lead/self_assign/"+PropertiesReadWrite.getValue("obj_id");
    Map<String, Object> headers;
    headers = getHeaders(CPuser, CPpassword);
    Response response=null;
    response=RestUtils.sendPatchRequest(url,headers);
//    if (response.getStatusCode() == 403) { // Token expired
//        String newToken = Generic.getToken(PropertiesReadWrite.getValue("CPUser"),PropertiesReadWrite.getValue("CPPassword"));
//        PropertiesReadWrite.setValue("token", newToken);
//        headers = getHeaders(newToken); // Update headers with the new token
//        response=RestUtils.sendPatchRequest(url,headers);
//    }
   // response.prettyPrint();
    validateResponse(response);
    System.out.println("assign lead to CPA");
}

@Test(enabled = true,priority = 3)
    public void comleteUnderWriting() throws InterruptedException {
    System.out.println("get lead json");
    String url = PropertiesReadWrite.getValue("baseURL")+"/ilos/v1/assignee/lead/"+PropertiesReadWrite.getValue("obj_id");
    Map<String, Object> headers;
    headers = getHeaders(CPuser, CPpassword);
    Response response=null;
    response=RestUtils.performGet(url,headers);
    Assert.assertEquals(response.getStatusCode(), 200);
  //  response.prettyPrint();
    if(JsonPath.from(response.asString()).get("dt.skip_cpu_changes")==null){
        System.out.println("skip_cpu_changes key is not present");
        String url1 = PropertiesReadWrite.getValue("baseURL")+"/ilos/v1/misc/lead-action";
        String requestBody = "{ \"act_typ\": \"SKIP_CPU_CHANGES\", \"app_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\" }";
        Response response1=RestUtils.performPost(url1,requestBody,headers);
        Assert.assertEquals(response1.getStatusCode(), 200);
        response1.prettyPrint();
    }else {
        System.out.println("skip_cpu_changes key is present");
    }
    System.out.println("lead result api call");
    String lead_result_url = baseUrl+"/ilos/v1/underwriter/lead/result/"+PropertiesReadWrite.getValue("obj_id");
    Response response1=RestUtils.performGet(lead_result_url,headers);
    validateResponse(response1);

    System.out.println("generate PDF");
    String url2 = baseUrl+"/ilos/v1/application/generate-pdf/" + PropertiesReadWrite.getValue("application_id");
    Response response2=RestUtils.performGet(url2,headers);
    validateResponse(response2);


    Thread.sleep(5000);

    System.out.println("submit lead to dedupe");
    String url3=baseUrl+"/ilos/v1/underwriter/lead/submit/"+PropertiesReadWrite.getValue("obj_id");
    Response response3=RestUtils.sendPatchRequest(url3,headers);
    validateResponse(response3);


    System.out.println("Underwriting completed ");






}




    private void validateResponse(Response response) {
        if (response.getStatusCode() != 200) {
            response.prettyPrint();
            Assert.assertEquals(response.getStatusCode(), 200);
        }
    }

}
