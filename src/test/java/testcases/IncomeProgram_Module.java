package testcases;

import generic.Generic;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import static io.restassured.RestAssured.given;
import static payloads.Header.getHeaders;

public class IncomeProgram_Module {

    String baseUrl = PropertiesReadWrite.getValue("baseURL");
    Map<String, Object> headers;
    Response response;
    String appId = PropertiesReadWrite.getValue("application_id");
    String lead_url = baseUrl + "/ilos/v1/assignee/lead/" + PropertiesReadWrite.getValue("obj_id");
    String UWUser = PropertiesReadWrite.getValue("UWUser");
    String UWPassword = PropertiesReadWrite.getValue("UWPassword");

    @Test(priority=1)
    public void completeIncomeProgram() throws IOException {
        headers = getHeaders(UWUser, UWPassword);
        Response response = RestUtils.performGet(lead_url, headers);
        Generic.validateResponse(response);

        Map<String, Object> payload = new HashMap<>();
        payload.put("dsn", "INCOME DOCUMENTS=ITR - FINANCIAL");
        payload.put("fn", "bankstatment.pdf");
        payload.put("app_id", appId);
        payload.put("ext", "pdf");
        payload.put("ft", "application/pdf");
        Response generate_upload_res = RestUtils.performPost(baseUrl+"/ilos/v1/misc/generate-upload-url", payload, headers);
        Generic.validateResponse(generate_upload_res);
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

        String document_handler_url = baseUrl + "/ilos/v2/document-handler/" + PropertiesReadWrite.getValue("obj_id");
        int applicantId = JsonPath.from(response.asString()).getInt("dt.applicant.primary.id");
        String special_program_code = JsonPath.from(response.asString()).getString("dt.primary.inquiry_details.special_program_code");
        String income_program = JsonPath.from(response.asString()).getString("dt.primary.inquiry_details.income_program");
        Map<String, Object> jsonPayload = new HashMap<>();
        jsonPayload.put("applicant_id", applicantId);
        jsonPayload.put("applicant_type", "primary");
        jsonPayload.put("doc_sub_type", "ITR - FINANCIAL");
        jsonPayload.put("doc_type", "INCOME DOCUMENTS");
        jsonPayload.put("doc_type_identifier", "income_program-itr-gst");
        jsonPayload.put("section", "borrower");
        jsonPayload.put("sub_section", "ITR - FINANCIAL");
        jsonPayload.put("unique_id", applicantId);
        jsonPayload.put("url", ky);
        Response documentHandlerResponse = RestUtils.performPost(document_handler_url, jsonPayload, headers);
        Generic.validateResponse(documentHandlerResponse);

        Response fetch_bank_accounts_response = RestUtils.performGet(baseUrl+"/ilos/v1/income-program/fetch-bank-accounts/"+PropertiesReadWrite.getValue("obj_id"), headers);
        Generic.validateResponse(fetch_bank_accounts_response);
        String bank_name = fetch_bank_accounts_response.jsonPath().getString("primary.applicantId.bank_name");
        String account_holder = fetch_bank_accounts_response.jsonPath().getString("primary.applicantId.account_holder");
        String account_number = fetch_bank_accounts_response.jsonPath().getString("primary.applicantId.account_number");
        String account_type = fetch_bank_accounts_response.jsonPath().getString("primary.applicantId.account_type");
        Map<String, Object> final_submit_payload;
       if(special_program_code.equalsIgnoreCase("none") && !(income_program.equalsIgnoreCase("cash salaried"))) {
           System.out.println("Income program is NIP");
           String payload_nip = getNIPPayload(applicantId);
           Response save_changes_nip = RestUtils.performPost(baseUrl+"/ilos/v1/income-program/nip/"+PropertiesReadWrite.getValue("obj_id"), payload_nip, headers);
           Generic.validateResponse(save_changes_nip);
           final_submit_payload = Map.of("income_program","income_program","total_eligible_income",14567.25);
       } else {
           String payload_surrogate = getPayload(applicantId, bank_name, account_holder, account_number, account_type);
           Response save_changes_bankingsurogate = RestUtils.performPost(baseUrl+"/ilos/v1/income-program/banking-surrogate/"+PropertiesReadWrite.getValue("obj_id"), payload_surrogate, headers);
           Generic.validateResponse(save_changes_bankingsurogate);
           final_submit_payload = Map.of("income_program","banking_surrogate","total_eligible_income",14567.25);
       }

        String final_submit_url = baseUrl+"/ilos/v1/income-program/final-submit/"+PropertiesReadWrite.getValue("obj_id");
        Response final_submit_response = RestUtils.performPost(final_submit_url, final_submit_payload, headers);
        Generic.validateResponse(final_submit_response);

        Response marksection_res = RestUtils.sendPatchRequest(baseUrl+"/ilos/v1/assignee/lead/mark-section-complete/"+PropertiesReadWrite.getValue("obj_id"),Map.of("section","income-program-analysis"), headers);
        Generic.validateResponse(marksection_res);

    }

    private String getNIPPayload(int applicantId) throws IOException {
        // Step 1: Prepare dynamic data for the Mustache template
        Map<String, Object> dynamicData = new HashMap<>();
        dynamicData.put("applicant_id", applicantId);

        // Step 2: Load the Mustache template
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(new FileReader(new File("src/main/java/payloads/NIP_payload.mustache")), "template");

        // Step 3: Process the template with dynamic data
        StringWriter writer = new StringWriter();
        mustache.execute(writer, dynamicData).flush();

        // The generated payload
        String generatedPayload = writer.toString();
      //  System.out.println(generatedPayload);
        return generatedPayload;
    }

    private String getPayload(int applicantId,String bank_name, String account_holder, String account_number, String account_type) throws IOException {
        Map<String, Object> dynamicData = new HashMap<>();
        dynamicData.put("applicant_id", applicantId);
        dynamicData.put("bank_name", bank_name);
        dynamicData.put("account_holder", account_holder);
        dynamicData.put("account_number", account_number);
        dynamicData.put("account_type", account_type);
        // Step 2: Load Mustache Template
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(new FileReader(new File("src/main/java/payloads/banking_surrogate_payload.mustache")), "template");

        // Step 3: Process Template with Dynamic Data
        StringWriter writer = new StringWriter();
        mustache.execute(writer, dynamicData).flush();

        // The generated payload
        String generatedPayload = writer.toString();
        System.out.println(generatedPayload);
        return generatedPayload;
    }




}
