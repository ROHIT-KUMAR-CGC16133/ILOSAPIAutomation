package testcases;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import payloads.Header;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static payloads.Header.getHeaders;

public class IPAModule {
    String baseUrl = PropertiesReadWrite.getValue("baseURL");
    Map<String, Object> headers;
    Response response;
    String appId = PropertiesReadWrite.getValue("application_id");
    String url = baseUrl + "/ilos/v1/ipa/lead/" + PropertiesReadWrite.getValue("obj_id");

    @Test(priority = 1)
    public void generateCibil() {
        try {
            System.out.println("Hit IPA lead api");
            headers = getHeaders(PropertiesReadWrite.getValue("token"));
            Map<String, Object> queryParams = Map.of("view", "true");
            Response IPA_lead_res = RestUtils.performGet(url, headers, queryParams);
            validateResponse(IPA_lead_res);

            response = RestUtils.performGet(url, headers);
            validateResponse(response);

            System.out.println("hit generate_cibil for applicant api");
            String cibil_pdf_url = JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bureau_check.pdfUrl");
            System.out.println("cibil_pdf_url : " + cibil_pdf_url);
            String generate_presigned_url = baseUrl + "/ilos/v1/misc/generate-presigned-url";
            Map<String, Object> generate_presigned_queryparam = Map.of("object_id", cibil_pdf_url);
            Response generate_cibil_app_res = RestUtils.performGet(generate_presigned_url, headers, generate_presigned_queryparam);
            validateResponse(generate_cibil_app_res);

            long createdAt = JsonPath.from(response.asString()).getLong("dt.applicant.primary.bureau_check.created_at");
            long daysAgo = System.currentTimeMillis() - (Integer.parseInt(PropertiesReadWrite.getValue("cibil_rerun_days")) * 24L * 60 * 60 * 1000);
            System.out.println("daysAgo " + daysAgo);

            if (daysAgo > createdAt) {
                rerunCibilReport("true", null, null);
            }

            processCoApplicants();
            processGuarantors();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Test failed due to exception: " + e.getMessage());
        }
    }

    @Test(priority = 2)
    public void complete_bank_novel(){
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        response = RestUtils.performGet(url, headers);
        String generate_upload_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/misc/generate-upload-url";
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        String appId = PropertiesReadWrite.getValue("application_id");
        String requestBody = """
        {
            "dsn": "BANK DOCUMENTS=LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT",
            "fn": "Banking.pdf",
            "app_id": "%s",
            "ext": "pdf",
            "ft": "application/pdf"
        }
    """.formatted(appId);

        Response generate_upload_res = RestUtils.performPost(generate_upload_url, requestBody, headers);
        validateResponse(generate_upload_res);
        String url = "https://s3.ap-south-1.amazonaws.com/cgcl-ilos-uat-ui";
        //headers = getHeaders(PropertiesReadWrite.getValue("token"));

       // Map<String, Object> headers = getHeaders(PropertiesReadWrite.getValue("token"));
        Map<String, Object> headers1 = new HashMap<>();
        headers1.put("Accept", "application/json, text/plain, */*");
        headers1.put("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8");
        headers1.put("Connection", "keep-alive");
        headers1.put("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryonu2u6HSdqoLVx4v");
        headers1.put("Origin", "https://ilos-uat.capriglobal.in");
        headers1.put("Referer", "https://ilos-uat.capriglobal.in/");
        headers1.put("Sec-Fetch-Dest", "empty");
        headers1.put("Sec-Fetch-Mode", "cors");
        headers1.put("Sec-Fetch-Site", "cross-site");
        headers1.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36");
        headers1.put("sec-ch-ua", "\"Chromium\";v=\"134\", \"Not:A-Brand\";v=\"24\", \"Google Chrome\";v=\"134\"");
        headers1.put("sec-ch-ua-mobile", "?0");
        headers1.put("sec-ch-ua-platform", "\"macOS\"");

        Map<String, String> formParams = new HashMap<String,String>();
        formParams.put("key", "upload_docs/user/914365_bankstatment.pdf_1742474264457.pdf");
        formParams.put("Content-Type", "application/pdf");
        formParams.put("x-amz-algorithm", "AWS4-HMAC-SHA256");
        formParams.put("x-amz-credential", "ASIA2WCRTZ2V3IBPSU6E/20250320/ap-south-1/s3/aws4_request");
        formParams.put("x-amz-date", "20250320T103413Z");
        formParams.put("x-amz-signature", "12b8017c5b0eb2c23984385da0bedaadb4983e2411379d3530c01f12ab86c65b");
        File file = new File(System.getProperty("user.dir") + "/src/main/resources/file/bankstatment.pdf");

        Response awsUploadResponse = RestAssured.given()
                .headers(headers1)
                .multiPart("file", file, "application/pdf")
                .formParams(formParams)
                .post(url);
        awsUploadResponse.prettyPrint();
        System.out.println("awsUploadResponse "+awsUploadResponse.getStatusCode());
       // validateResponse(awsUploadResponse);

        String endPoint = PropertiesReadWrite.getValue("baseURL") + "/ilos/v2/document-handler/67b31bf13d40b17c547b4fcb";
        Map<String, Object> headers = Header.getHeaders(PropertiesReadWrite.getValue("token"));
        String applicantId = response.jsonPath().getString("dt.applicant.primary.id");

        Map<String, Object> payload = Map.of(
                "section", "Banking.pdf",
                "sub_section", "BANK DOCUMENTS",
                "url", "cgcl-ilos-uat-ui/upload_docs/user/914365_Banking.pdf_1742466853211.pdf",
                "doc_type", "BANK DOCUMENTS",
                "doc_sub_type", "LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT",
                "applicant_id", applicantId, // Ensure applicantId is of the correct type
                "applicant_type", "primary",
                "doc_type_identifier", "LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT",
                "unique_id", "undefined"
        );

        Response document_handler_res = RestUtils.performPost(endPoint, payload, headers);
        document_handler_res.prettyPrint();
        validateResponse(document_handler_res);





    }

    @Test(priority = 3)
    public void uploadBankStatement() {
        String url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/ipa/lead/bank-statement-upload/" + PropertiesReadWrite.getValue("obj_id");
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        String appId = PropertiesReadWrite.getValue("application_id");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("is_prm_app", true);
        requestBody.put("coapp_id", null);
        requestBody.put("app_id", appId); // Use the variable here
        requestBody.put("acc_no", "0437010100151");
        requestBody.put("dsn", "BANK DOCUMENTS=LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT");
        requestBody.put("stmt", "cgcl-ilos-uat-ui/upload_docs/user/" + appId + "_bankstatment.pdf_1742474264457.pdf");

        Response upload_bank_statement_res = RestUtils.sendPatchRequest(url, requestBody, headers);
        validateResponse(upload_bank_statement_res);

    }
    @Test(priority = 4)
    public void initiate_novel(){
        String url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/ipa/lead/initiate-novel/" + PropertiesReadWrite.getValue("obj_id");
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("is_prm_app", true);
        requestBody.put("coapp_id", null);
        requestBody.put("app_id", appId);
        requestBody.put("acc_no", "0437010100151");
        requestBody.put("dsn", "BANK DOCUMENTS=LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT");
        requestBody.put("stmt", "cgcl-ilos-uat-ui/upload_docs/user/" + appId + "_bankstatment.pdf_1742474264457.pdf");
        requestBody.put("fn", "AXIS BANK (0437010100151)");
        Response uploadBankStatementRes = RestUtils.sendPatchRequest(url, requestBody, headers);
        validateResponse(uploadBankStatementRes);
    }

@Test(priority = 5)
public void submitLead() {
    // Constructing the API URL
    String url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/ipa/lead/submit/" + PropertiesReadWrite.getValue("obj_id");

    // Setting up headers
    headers = getHeaders(PropertiesReadWrite.getValue("token"));

    // Creating request body
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("is_prm_app", true);
    requestBody.put("coapp_id", null);
    requestBody.put("app_id", appId);
    requestBody.put("acc_no", "0437010100151");
    requestBody.put("dsn", "BANK DOCUMENTS=LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT");
    requestBody.put("stmt", "cgcl-ilos-uat-ui/upload_docs/user/" + appId + "_bankstatment.pdf_1742474264457.pdf");
    requestBody.put("fn", "AXIS BANK (0437010100151)");

    // Sending PATCH request
    Response response = RestUtils.sendPatchRequest(url, requestBody, headers);

    // Validate Response
    validateResponse(response);
}

    private void validateResponse(Response response) {
        if (response.getStatusCode() != 200) {
            response.prettyPrint();
            Assert.assertEquals(response.getStatusCode(), 200);
        }
    }

    private void rerunCibilReport(String isPrimary, Integer coappId, Integer guarantorId) {
        String rerun_cibil_url = baseUrl + "/ilos/v1/ipa/lead/fetch-cibil/" + PropertiesReadWrite.getValue("obj_id");
        Map<String, Object> rerun_cibil_queryparam = Map.of("is_primary", isPrimary, "coapp_id", coappId, "guarantor_id", guarantorId);
        Response rerun_cibil_res = RestUtils.performGet(rerun_cibil_url, headers, rerun_cibil_queryparam);
        validateResponse(rerun_cibil_res);
    }

    private void processCoApplicants() {
        int co_app_count = response.jsonPath().getInt("dt.applicant.co_applicant.size()");
        if (co_app_count > 0) {
            for (int i = 0; i < co_app_count; i++) {
                long createdAtCoapp = response.jsonPath().getLong("dt.applicant.co_applicant[" + i + "].bureau_check.created_at");
                int coapp_id = response.jsonPath().getInt("dt.applicant.co_applicant[" + i + "].id");
                long daysAgoCoapp = System.currentTimeMillis() - (Integer.parseInt(PropertiesReadWrite.getValue("cibil_rerun_days")) * 24L * 60 * 60 * 1000);
                if (daysAgoCoapp > createdAtCoapp) {
                    rerunCibilReport("false", coapp_id, null);
                }
                generateCibilForEntity("co_applicant", i);
            }
        }
    }

    private void processGuarantors() {
        int guarantor_count = response.jsonPath().getInt("dt.applicant.guarantors.size()");
        if (guarantor_count > 0) {
            for (int i = 0; i < guarantor_count; i++) {
                long createdAtGuarantor = response.jsonPath().getLong("dt.applicant.guarantors[" + i + "].bureau_check.created_at");
                int guarantor_id = response.jsonPath().getInt("dt.applicant.guarantors[" + i + "].id");
                long daysAgoGuarantor = System.currentTimeMillis() - (Integer.parseInt(PropertiesReadWrite.getValue("cibil_rerun_days")) * 24L * 60 * 60 * 1000);
                if (daysAgoGuarantor > createdAtGuarantor) {
                    rerunCibilReport("false", null, guarantor_id);
                }
                generateCibilForEntity("guarantors", i);
            }
        }
    }

    private void generateCibilForEntity(String entityType, int index) {
        System.out.println("hit generate_cibil for " + entityType + (index + 1) + " api");
        String cibil_pdf_url = JsonPath.from(response.asString()).getString("dt.applicant." + entityType + "[" + index + "].bureau_check.pdfUrl");
        String generate_presigned_url = baseUrl + "/ilos/v1/misc/generate-presigned-url";
        Map<String, Object> generate_presigned_queryparam = Map.of("object_id", cibil_pdf_url);
        Response generate_cibil_res = RestUtils.performGet(generate_presigned_url, headers, generate_presigned_queryparam);
        validateResponse(generate_cibil_res);
    }
}