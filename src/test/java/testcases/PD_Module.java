package testcases;

import generic.Generic;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.util.List;
import java.util.Map;

import static payloads.Header.getHeaders;

public class PD_Module {

    String baseUrl = PropertiesReadWrite.getValue("baseURL");
    Map<String, Object> headers;
    Response response;
    String appId = PropertiesReadWrite.getValue("application_id");
    String lead_url = baseUrl + "/ilos/v1/assignee/lead/" + PropertiesReadWrite.getValue("obj_id");

    @Test(priority = 1)
    public void getPD_HomeBranch_Lead() {
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        String endPoint = baseUrl + "/pd/application/list/home/0/10";
        Response get_Homebranch_lead = RestUtils.performGet(endPoint, headers);
        Generic.validateResponse(get_Homebranch_lead);
        Assert.assertTrue(get_Homebranch_lead.getBody().asString().contains(appId), "appId is not present in the response");

    }
    @Test(priority = 2)
    public void assignPD_ToSELF() {
        String url = baseUrl + "/pd/application/assign";
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        Map<String, Object> payload = Map.of( "appId", appId);
        Response response = RestUtils.performPost(url, payload, headers);
        Generic.validateResponse(response);
        Response assign_to_me_application = RestUtils.performGet(baseUrl+"/pd/application/list/myApplication/PROCESSOR/0/10?", headers);
        Generic.validateResponse(assign_to_me_application);
        Assert.assertTrue(assign_to_me_application.getBody().asString().contains(appId), "appId is not present in the assign to me response");
    }
    @Test(priority = 3)
    public void assignPD_ToProcessor() {
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        Response lead_details = RestUtils.performGet(baseUrl+"/ilos/v1/lead/lead-detail?application_id="+appId, headers);
        Generic.validateResponse(lead_details);
        Response pdmeta_list = RestUtils.performGet(baseUrl+"/pd/pdmeta/list/"+appId+"?user=PROCESSOR ", headers);
        Generic.validateResponse(pdmeta_list);
        Response pd_initiation = RestUtils.performGet(baseUrl+"/ilos/v1/misc/lead-config?section=pd_initiation", headers);
        Generic.validateResponse(pd_initiation);
        Response assignee_lead = RestUtils.performGet(lead_url,headers);
        Generic.validateResponse(assignee_lead);
        String branch_code = lead_details.jsonPath().getString("dt.inquiry_details.branch_code");
        System.out.println("branch_code: "+branch_code);
        Response user_available = RestUtils.performGet(baseUrl+"/ilosuser/v1/user/available?additional_branch_code="+branch_code+"&role=UNDERWRITER&login_status=0&email=&sbu=cghfl", headers);
        Generic.validateResponse(user_available);
        String applicantId = pdmeta_list.jsonPath().getString("result[0].applicantId");
        boolean isReferredBranchView = pdmeta_list.jsonPath().getBoolean("result[0].isReferred");
        List<Map<String, Object>> pdList = pdmeta_list.jsonPath().getList("result");
        JSONArray pdRequestList = new JSONArray();
        for (Map<String, Object> pd : pdList) {
            JSONObject pdItem = new JSONObject();
            pdItem.put("pdId", pd.get("id"));
            pdItem.put("empId", "BU0010");
            pdItem.put("empName", "Jaipragatiuser20");
            pdRequestList.put(pdItem);
        }
        JSONObject requestBody = new JSONObject();
        requestBody.put("applicationId", appId);
        requestBody.put("applicantId", applicantId);
        requestBody.put("isReferredBranchView", isReferredBranchView);
        requestBody.put("pdRequestList", pdRequestList);

        Response saveRequest = RestUtils.performPost(baseUrl+"/pd/pdmeta/saveRequest", requestBody.toString(), headers);
        Generic.validateResponse(saveRequest);
        Response submitRequest = RestUtils.performPost(baseUrl+"/pd/pdmeta/submitRequest", requestBody.toString(), headers);
        Generic.validateResponse(submitRequest);


    }


}
