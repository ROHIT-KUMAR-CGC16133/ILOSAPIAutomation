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

import static io.restassured.RestAssured.given;
import static payloads.Header.getHeaders;

public class IPAModule {
    String baseUrl = PropertiesReadWrite.getValue("baseURL");
    Map<String, Object> headers;
    Response response;
    String appId = PropertiesReadWrite.getValue("application_id");
    String url = baseUrl + "/ilos/v1/assignee/lead/" + PropertiesReadWrite.getValue("obj_id");
    String CPuser = PropertiesReadWrite.getValue("CPUser");
    String CPpassword = PropertiesReadWrite.getValue("CPPassword");
    Response IPA_lead_res;
    @Test(priority = 1)
    public void generateCibil() {
        try {
            System.out.println("Hit IPA lead api");
            headers = getHeaders(CPuser, CPpassword);
            Map<String, Object> queryParams = Map.of("view", "true");
            IPA_lead_res = RestUtils.performGet(url, headers, queryParams);
            validateResponse(IPA_lead_res);
            response = RestUtils.performGet(url, headers);
            validateResponse(response);
            System.out.println("hit generate_cibil for applicant api");
            String entity_type = JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.entity_type");
            if(!entity_type.equals("Organization")) {
                String cibil_pdf_url = JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bureau_check.pdfUrl");
                System.out.println("cibil_pdf_url : " + cibil_pdf_url);
                String generate_presigned_url = baseUrl + "/ilos/v1/misc/generate-presigned-url";
                Map<String, Object> generate_presigned_queryparam = Map.of("object_id", cibil_pdf_url);
                Response generate_cibil_app_res = RestUtils.performGet(generate_presigned_url, headers, generate_presigned_queryparam);
                validateResponse(generate_cibil_app_res);
                long createdAt = JsonPath.from(response.asString()).getLong("dt.applicant.primary.bureau_check.created_at");
                long daysAgo = System.currentTimeMillis() - (Integer.parseInt(PropertiesReadWrite.getValue("cibil_rerun_days")) * 24L * 60 * 60 * 1000);
                if (daysAgo > createdAt) {
                    System.out.println("re run cibil for applicant");
                    String rerun_cibil_url = baseUrl + "/ilos/v1/ipa/lead/fetch-cibil/" + PropertiesReadWrite.getValue("obj_id");
                    Map<String, Object> rerun_cibil_queryparam = Map.of("is_primary", true);
                    Response rerun_cibil_res = RestUtils.performGet(rerun_cibil_url, headers, rerun_cibil_queryparam);
                    validateResponse(rerun_cibil_res);
                }
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
        headers = getHeaders(CPuser, CPpassword);
        response = RestUtils.performGet(url, headers);
        String generate_upload_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/misc/generate-upload-url";
        headers = getHeaders(CPuser, CPpassword);
        String appId = PropertiesReadWrite.getValue("application_id");
        System.out.println("hit generate-upload-url api");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fn", "Banking.pdf");
        requestBody.put("ext", "pdf");
        requestBody.put("app_id", appId); // Use the variable here
        requestBody.put("dsn", "BANK DOCUMENTS=LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT");
        requestBody.put("ft", "application/pdf");

        Response generate_upload_res = RestUtils.performPost(generate_upload_url, requestBody, headers);
        validateResponse(generate_upload_res);
        String Content_Type = generate_upload_res.jsonPath().getString("fld.Content-Type");
        String key = generate_upload_res.jsonPath().getString("fld.key");
        String x_amz_algorithm = generate_upload_res.jsonPath().getString("fld.x-amz-algorithm");
        String x_amz_credential = generate_upload_res.jsonPath().getString("fld.x-amz-credential");
        String x_amz_date = generate_upload_res.jsonPath().getString("fld.x-amz-date");
        String x_amz_security_token = generate_upload_res.jsonPath().getString("fld.x-amz-security-token");
        String policy = generate_upload_res.jsonPath().getString("fld.policy");

        File file = new File(System.getProperty("user.dir") + "/src/main/resources/file/bankstatment.pdf");
        RestAssured.baseURI = "https://s3.ap-south-1.amazonaws.com/cgcl-ilos-uat-ui";
        Response response_awsupload = given()
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                .header("Connection", "keep-alive")
                .header("Origin", "https://ilos-uat.capriglobal.in")
                .header("Referer", "https://ilos-uat.capriglobal.in/")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "cross-site")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                .header("sec-ch-ua", "\"Chromium\";v=\"134\", \"Not:A-Brand\";v=\"24\", \"Google Chrome\";v=\"134\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .multiPart("Content-Type", Content_Type)
                .multiPart("key", key)
                .multiPart("x-amz-algorithm", x_amz_algorithm)
                .multiPart("x-amz-credential", x_amz_credential)
                .multiPart("x-amz-date", x_amz_date)
                .multiPart("x-amz-security-token", x_amz_security_token)
                .multiPart("policy", policy) // Replace with actual policy

                // Replace with actual policy
                .multiPart("x-amz-signature", generate_upload_res.jsonPath().getString("fld.x-amz-signature"))
                .multiPart("file", file, "application/pdf") // Attach file
                .when()
                .post()
                .then()// Validate success response
                .extract()
                .response();

       // System.out.println("Response of aws upload: " + response_awsupload.prettyPrint());


        String endPoint = PropertiesReadWrite.getValue("baseURL") + "/ilos/v2/document-handler/67b31bf13d40b17c547b4fcb";
        Map<String, Object> headers = Header.getHeaders(CPuser, CPpassword);
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
        String upload_bank_statement_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/ipa/lead/bank-statement-upload/" + PropertiesReadWrite.getValue("obj_id");
        headers = getHeaders(CPuser, CPpassword);
        String appId = PropertiesReadWrite.getValue("application_id");
        IPA_lead_res = RestUtils.performGet(url+"?view=true", headers);
        validateResponse(IPA_lead_res);
        String acc_no =JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bank_acc_details[0].account_number");
        String statement = JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bank_acc_details[0].statement");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("is_prm_app", true);
        requestBody.put("coapp_id", null);
        requestBody.put("app_id", appId); // Use the variable here
        requestBody.put("acc_no", acc_no);
        requestBody.put("dsn", "BANK DOCUMENTS=LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT");
        requestBody.put("stmt", statement);

        Response upload_bank_statement_res = RestUtils.sendPatchRequest(upload_bank_statement_url, requestBody, headers);
        validateResponse(upload_bank_statement_res);

    }
    @Test(priority = 4)
    public void initiate_novel(){
        String initiate_novel_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/ipa/lead/initiate-novel/" + PropertiesReadWrite.getValue("obj_id");
        headers = getHeaders(CPuser, CPpassword);
        IPA_lead_res = RestUtils.performGet(url+"?view=true", headers);
        String acc_no =JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bank_acc_details[0].account_number");
        String bank_name_with_account_no =JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bank_acc_details[0].bank_name")+" ("+acc_no+")";
        String statement = JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bank_acc_details[0].statement");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("is_prm_app", true);
        requestBody.put("coapp_id", null);
        requestBody.put("app_id", appId);
        requestBody.put("acc_no", acc_no);
        requestBody.put("dsn", "BANK DOCUMENTS=LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT");
        requestBody.put("stmt", statement);
        requestBody.put("fn", bank_name_with_account_no);
        Response uploadBankStatementRes = RestUtils.sendPatchRequest(initiate_novel_url, requestBody, headers);
        validateResponse(uploadBankStatementRes);
    }

@Test(priority = 5 )
public void submitLead() {
    String submit_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/ipa/lead/submit/" + PropertiesReadWrite.getValue("obj_id");
    headers = getHeaders(CPuser, CPpassword);
    IPA_lead_res = RestUtils.performGet(url+"?view=true", headers);
    String acc_no =JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bank_acc_details[0].account_number");
    String bank_name_with_account_no =JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bank_acc_details[0].bank_name")+" ("+acc_no+")";
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("is_prm_app", true);
    requestBody.put("coapp_id", null);
    requestBody.put("app_id", appId);
    requestBody.put("acc_no", acc_no);
    requestBody.put("dsn", "BANK DOCUMENTS=LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT");
    requestBody.put("stmt", "cgcl-ilos-uat-ui/upload_docs/user/" + appId + "_bankstatment.pdf_1742474264457.pdf");
    requestBody.put("fn", bank_name_with_account_no);

    // Sending PATCH request
    Response response = RestUtils.sendPatchRequest(submit_url, requestBody, headers);

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
                    System.out.println("run cibil for co app"+coapp_id);
                    String rerun_cibil_url = baseUrl + "/ilos/v1/ipa/lead/fetch-cibil/" + PropertiesReadWrite.getValue("obj_id");
                    Map<String, Object> rerun_cibil_queryparam = Map.of("is_primary", false, "coapp_id", coapp_id);
                    Response rerun_cibil_res = RestUtils.performGet(rerun_cibil_url, headers, rerun_cibil_queryparam);
                    validateResponse(rerun_cibil_res);
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
                    String rerun_cibil_url = baseUrl + "/ilos/v1/ipa/lead/fetch-cibil/" + PropertiesReadWrite.getValue("obj_id");
                    Map<String, Object> rerun_cibil_queryparam = Map.of("is_primary", false, "guarantor_id", guarantor_id);
                    Response rerun_cibil_res = RestUtils.performGet(rerun_cibil_url, headers, rerun_cibil_queryparam);
                    validateResponse(rerun_cibil_res);
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