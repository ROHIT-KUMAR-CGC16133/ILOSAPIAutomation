package testcases;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.util.HashMap;
import java.util.Map;

import static payloads.Header.getHeaders;

public class DedupeModule {
    Map<String, Object> headers;
    Response response=null;
    String baseurl=PropertiesReadWrite.getValue("baseURL");
    String obj_id =PropertiesReadWrite.getValue("obj_id");
    String url = baseurl+"/ilos/v1/assignee/lead/"+PropertiesReadWrite.getValue("obj_id");
    String CPuser = PropertiesReadWrite.getValue("CPUser");
    String CPpassword = PropertiesReadWrite.getValue("CPPassword");

    @Test(enabled = true,priority = 1)
    public void complete_UCIC_Dedupe(){
        System.out.println("lead json api");
        headers = getHeaders(CPuser, CPpassword);
        response= RestUtils.performGet(url,headers);

        if(response.getStatusCode()!=200){
              response.prettyPrint();
            Assert.assertEquals(response.getStatusCode(), 200);
        }
        String application_id= JsonPath.from(response.asString()).getString("dt.application_id");
        int applicant_id = JsonPath.from(response.asString()).getInt("dt.applicant.primary.id");

        System.out.println("search customer api for applicant");
        String url1 =PropertiesReadWrite.getValue("baseURL")+"/ilos/v1/customer/posidex/search-customer";

        String requestBody = "{"
                + "\"customer_type\": \"applicant\","
                + "\"application_id\": \"" + application_id + "\","
                + "\"customer_id\": " + applicant_id
                + "}";
        Response searchcustomer_response = RestUtils.performPost(url1,requestBody,headers);
        if(searchcustomer_response.getStatusCode()!=200) {
            searchcustomer_response.prettyPrint();
            Assert.assertEquals(searchcustomer_response.getStatusCode(), 200);
        }
        String createcustomer_url=PropertiesReadWrite.getValue("baseURL")+"/ilos/v1/customer/posidex/create-customer";
        String mapcustomer_url=PropertiesReadWrite.getValue("baseURL")+"/ilos/v1/customer/map";
        if(JsonPath.from(searchcustomer_response.asString()).getList("data").isEmpty()){
            System.out.println("create customer api for applicant");
            Response createcustomer_response = RestUtils.performPost(createcustomer_url,requestBody,headers);
            if(createcustomer_response.getStatusCode()!=200){
                createcustomer_response.prettyPrint();
                Assert.assertEquals(createcustomer_response.getStatusCode(), 200);
            }
        }else {
            String ucic = JsonPath.from(searchcustomer_response.asString()).getString("data[0].UCIC");
            System.out.println("map customer api for applicant");
           // String mapcustomer_url=PropertiesReadWrite.getValue("baseURL")+"/ilos/v1/customer/map";
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("customer_type", "applicant");
            requestData.put("application_id", application_id);
            requestData.put("customer_id", applicant_id);
            requestData.put("ucic", ucic);
            System.out.println(requestData);
            Response mapcustome_response = RestUtils.sendPatchRequest(mapcustomer_url,requestData,headers);
            if(mapcustome_response.getStatusCode()!=200){
                mapcustome_response.prettyPrint();
                Assert.assertEquals(mapcustome_response.getStatusCode(), 200);
            }
        }

        int coapp_count = JsonPath.from(response.asString()).getList("dt.applicant.co_applicant").size();
        for(int i=0;i<coapp_count;i++) {
            int coapp_id = JsonPath.from(response.asString()).getInt("dt.applicant.co_applicant[" + i + "].id");
            System.out.println("coapp_id " + coapp_id);
            System.out.println("search customer api for co app " + (i+1));

            String requestBody_coapp = "{"
                    + "\"customer_type\": \"co_applicant\","
                    + "\"application_id\": \"" + application_id + "\","
                    + "\"customer_id\": " + coapp_id
                    + "}";
            Response search_coapp_response = RestUtils.performPost(url1, requestBody_coapp, headers);
            if (search_coapp_response.getStatusCode() != 200) {
                search_coapp_response.prettyPrint();
                Assert.assertEquals(search_coapp_response.getStatusCode(), 200);
            }
            if (JsonPath.from(search_coapp_response.asString()).getList("data").isEmpty()) {
                System.out.println("create customer api for co applicant "+(i+1));
                Response createcustomer_coapp_response = RestUtils.performPost(createcustomer_url, requestBody_coapp, headers);
                if (createcustomer_coapp_response.getStatusCode() != 200) {
                    createcustomer_coapp_response.prettyPrint();
                    Assert.assertEquals(createcustomer_coapp_response.getStatusCode(), 200);
                }
            } else {
                String ucic = JsonPath.from(search_coapp_response.asString()).getString("data[0].UCIC");
                System.out.println("map customer api for co applicant "+(i+1));
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("customer_type", "co_applicant");
                requestData.put("application_id", application_id);
                requestData.put("customer_id", coapp_id);
                requestData.put("ucic", ucic);
                System.out.println(requestData);
                Response mapcustome_response = RestUtils.sendPatchRequest(mapcustomer_url, requestData, headers);
                if (mapcustome_response.getStatusCode() != 200) {
                    mapcustome_response.prettyPrint();
                    Assert.assertEquals(mapcustome_response.getStatusCode(), 200);
                }
            }


        }
        int guarantor_count = JsonPath.from(response.asString()).getList("dt.applicant.guarantors").size();
        for(int i=0;i<guarantor_count;i++) {
            int guarantor_id = JsonPath.from(response.asString()).getInt("dt.applicant.guarantors[" + i + "].id");
            System.out.println("guarantor_id " + guarantor_id);
            System.out.println("search customer api for guarantor " + (i + 1));

            String requestBody_guarantor = "{"
                    + "\"customer_type\": \"guarantor\","
                    + "\"application_id\": \"" + application_id + "\","
                    + "\"customer_id\": " + guarantor_id
                    + "}";
            Response search_guarantor_response = RestUtils.performPost(url1, requestBody_guarantor, headers);
            if (search_guarantor_response.getStatusCode() != 200) {
                search_guarantor_response.prettyPrint();
                Assert.assertEquals(search_guarantor_response.getStatusCode(), 200);
            }
            if (JsonPath.from(search_guarantor_response.asString()).getList("data").isEmpty()) {
                System.out.println("create customer api for guarantor " + (i + 1));
                Response createcustomer_guarantor_response = RestUtils.performPost(createcustomer_url, requestBody_guarantor, headers);
                if (createcustomer_guarantor_response.getStatusCode() != 200) {
                    createcustomer_guarantor_response.prettyPrint();
                    Assert.assertEquals(createcustomer_guarantor_response.getStatusCode(), 200);
                }
            } else {
                String ucic = JsonPath.from(search_guarantor_response.asString()).getString("data[0].UCIC");
                System.out.println("map customer api for guarantor " + (i + 1));
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("customer_type", "guarantor");
                requestData.put("application_id", application_id);
                requestData.put("customer_id", guarantor_id);
                requestData.put("ucic", ucic);
                System.out.println(requestData);
                Response mapcustome_response = RestUtils.sendPatchRequest(mapcustomer_url, requestData, headers);
                if (mapcustome_response.getStatusCode() != 200) {
                    mapcustome_response.prettyPrint();
                    Assert.assertEquals(mapcustome_response.getStatusCode(), 200);
                }
            }
        }

            System.out.println("section complete api");
            String marksection_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/assignee/lead/mark-section-complete/" + PropertiesReadWrite.getValue("obj_id");
            Response marksection_response = RestUtils.sendPatchRequest(marksection_url, "{\"section\":\"dedupe-posidex\"}", headers);

            if (marksection_response.getStatusCode() != 200) {
                marksection_response.prettyPrint();
                Assert.assertEquals(marksection_response.getStatusCode(), 200);
            }



    }

@Test(enabled = true,priority = 2)
    public void completeFlexcubeDedupe(){
    headers = getHeaders(CPuser, CPpassword);
    response= RestUtils.performGet(url,headers);
   // response.prettyPrint();
   System.out.println("search customer api for applicant");
    String url1 =PropertiesReadWrite.getValue("baseURL")+"/ilos/v1/dedupe/search-customer";
    String application_id= JsonPath.from(response.asString()).getString("dt.application_id");
    int applicant_id = JsonPath.from(response.asString()).getInt("dt.applicant.primary.id");
    int coapp_count = JsonPath.from(response.asString()).getList("dt.applicant.co_applicant").size();



    String requestBody = "{"
            + "\"customer_type\": \"applicant\","
            + "\"application_id\": \"" + application_id + "\","
            + "\"customer_id\": " + applicant_id
            + "}";
    Response searchcustomer_response = RestUtils.performPost(url1,requestBody,headers);

    if(searchcustomer_response.getStatusCode()!=200) {
        searchcustomer_response.prettyPrint();
        Assert.assertEquals(searchcustomer_response.getStatusCode(), 200);
    }


    for(int i=0;i<coapp_count;i++) {
        int coapp_id = JsonPath.from(response.asString()).getInt("dt.applicant.co_applicant[" + i + "].id");
        System.out.println("search customer api for co app " + (i+1));
        String requestBody_coapp = "{"
                + "\"customer_type\": \"co_applicant\","
                + "\"application_id\": \"" + application_id + "\","
                + "\"customer_id\": " + coapp_id
                + "}";
        Response search_coapp_response = RestUtils.performPost(url1, requestBody_coapp, headers);
        if (search_coapp_response.getStatusCode() != 200) {
            search_coapp_response.prettyPrint();
            Assert.assertEquals(search_coapp_response.getStatusCode(), 200);
        }
    }
    int guarantor_count = JsonPath.from(response.asString()).getList("dt.applicant.guarantors").size();
    for(int i=0;i<guarantor_count;i++) {
        int guarantor_id = JsonPath.from(response.asString()).getInt("dt.applicant.guarantors[" + i + "].id");
        System.out.println("search customer api for guarantor " + (i + 1));
        String requestBody_guarantor = "{"
                + "\"customer_type\": \"guarantor\","
                + "\"application_id\": \"" + application_id + "\","
                + "\"customer_id\": " + guarantor_id
                + "}";
        Response search_guarantor_response = RestUtils.performPost(url1, requestBody_guarantor, headers);
        if (search_guarantor_response.getStatusCode() != 200) {
            search_guarantor_response.prettyPrint();
            Assert.assertEquals(search_guarantor_response.getStatusCode(), 200);
        }
    }


        System.out.println("submit dedupe");
        String dedupe_submit_url = baseurl+"/ilos/v1/underwriter/lead/dedupe-submit/"+obj_id;
        Response dedupe_submit_response = RestUtils.performPost(dedupe_submit_url,"{\"dpd\":0}",headers);
        dedupe_submit_response.prettyPrint();
        Assert.assertEquals(dedupe_submit_response.getStatusCode(), 200);



}



}
