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
    Map<String, Object> headers;
    Response response;
    String url = baseUrl+"/ilos/v1/assignee/lead/"+PropertiesReadWrite.getValue("obj_id");
    @Test
    public void generateCibil() {
        System.out.println("Hit IPA lead api");
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        String url = baseUrl+"/ilos/v1/ipa/lead/"+PropertiesReadWrite.getValue("obj_id");
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        Map<String, Object> queryParams = Map.of("view", "true");
        Response IPA_lead_res = RestUtils.performGet(url, headers, queryParams);
       if(IPA_lead_res.getStatusCode()!=200){
           IPA_lead_res.prettyPrint();
           Assert.assertEquals(IPA_lead_res.getStatusCode(), 200);
       }
        response= RestUtils.performGet(url,headers);
       if(response.getStatusCode()!=200){
           response.prettyPrint();
           Assert.assertEquals(response.getStatusCode(), 200);
       }
       System.out.println("hit generate_presigned api");
       String cibil_pdf_url = JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bureau_check.pdfUrl");
        System.out.println("cibil_pdf_url : "+cibil_pdf_url);
        String generate_presigned_url = baseUrl+"/ilos/v1/misc/generate-presigned-url";
        Map<String, Object> generate_presigned_queryparam = Map.of("object_id", cibil_pdf_url);
        Response generate_presigned_res = RestUtils.performGet(generate_presigned_url, headers,generate_presigned_queryparam);
       if(generate_presigned_res.getStatusCode()!=200){
           generate_presigned_res.prettyPrint();
           Assert.assertEquals(generate_presigned_res.getStatusCode(), 200);
       }
//        String presigned_url = JsonPath.from(generate_presigned_res.asString()).getString("dt.url");
//        System.out.println("presigned_url : "+presigned_url);
//        String download_pdf = baseUrl+"/ilos/v1/misc/download-presigned-url?url="+presigned_url;
//        Response download_pdf_res = RestUtils.performGet(download_pdf, headers);
//        if(download_pdf_res.getStatusCode()!=200){
//            download_pdf_res.prettyPrint();
//            Assert.assertEquals(download_pdf_res.getStatusCode(), 200);
//        }
        int createdAtTime = JsonPath.from(response.asString()).getInt("dt.applicant.primary.bureau_check.created_at");
        long createdAt = Long.valueOf(createdAtTime);
        // Get current timestamp in milliseconds
        long now = System.currentTimeMillis();
        int daysLimit = Integer.parseInt(PropertiesReadWrite.getValue("cibil_rerun_days"));
        long daysAgo = now - (daysLimit * 24L * 60 * 60 * 1000);
        System.out.println("daysAgo "+daysAgo);
        if(daysAgo<createdAt){
            System.out.println("Cibil report is older than "+daysLimit+" days. Need to rerun the cibil report");
            String rerun_cibil_url = baseUrl+"/ilos/v1/ipa/lead/fetch-cibil/"+PropertiesReadWrite.getValue("obj_id");
            Map<String, Object> rerun_cibil_queryparam = Map.of("is_primary", "true");
            Response rerun_cibil_res = RestUtils.performGet(rerun_cibil_url, headers, rerun_cibil_queryparam);
            if(rerun_cibil_res.getStatusCode()!=200){
                rerun_cibil_res.prettyPrint();
                Assert.assertEquals(rerun_cibil_res.getStatusCode(), 200);
            }
        }
        int co_app_count = response.jsonPath().getInt("dt.applicant.co_applicant.size()");
        if(co_app_count>0) {
            for (int i = 0; i < co_app_count; i++) {
                long createdAtCoapp = response.jsonPath().getLong("dt.applicant.co_applicant[" + i + "].bureau_check.created_at");
                int coapp_id = response.jsonPath().getInt("dt.applicant.co_applicant[" + i + "].id");
                long daysAgoCoapp = now - (daysLimit * 24L * 60 * 60 * 1000);
                if (daysAgoCoapp > createdAtCoapp) {
                    System.out.println("Cibil report is older than " + daysLimit + " days. Need to rerun the cibil report");
                    String rerun_cibil_url = baseUrl + "/ilos/v1/ipa/lead/fetch-cibil/" + PropertiesReadWrite.getValue("obj_id");
                    Map<String, Object> rerun_cibil_queryparam = Map.of("is_primary", "false", "coapp_id", coapp_id);
                    Response rerun_cibil_res = RestUtils.performGet(rerun_cibil_url, headers, rerun_cibil_queryparam);
                    if (rerun_cibil_res.getStatusCode() != 200) {
                        rerun_cibil_res.prettyPrint();
                        Assert.assertEquals(rerun_cibil_res.getStatusCode(), 200);
                    }
                }
            }
        }
        int guarantor_count = response.jsonPath().getInt("dt.applicant.guarantors.size()");
        if (guarantor_count>0){
            for (int i = 0; i < guarantor_count; i++) {
                long createdAtGuarantor = response.jsonPath().getLong("dt.applicant.guarantors[" + i + "].bureau_check.created_at");
                int guarantor_id = response.jsonPath().getInt("dt.applicant.guarantors[" + i + "].id");
                long daysAgoGuarantor = now - (daysLimit * 24L * 60 * 60 * 1000);
                if (daysAgoGuarantor > createdAtGuarantor) {
                    System.out.println("Cibil report is older than " + daysLimit + " days. Need to rerun the cibil report");
                    String rerun_cibil_url = baseUrl + "/ilos/v1/ipa/lead/fetch-cibil/" + PropertiesReadWrite.getValue("obj_id");
                    Map<String, Object> rerun_cibil_queryparam = Map.of("is_primary", "false", "guarantor_id", guarantor_id);
                    Response rerun_cibil_res = RestUtils.performGet(rerun_cibil_url, headers, rerun_cibil_queryparam);
                    if (rerun_cibil_res.getStatusCode() != 200) {
                        rerun_cibil_res.prettyPrint();
                        Assert.assertEquals(rerun_cibil_res.getStatusCode(), 200);
                    }
                }
            }
        }
    }

}
