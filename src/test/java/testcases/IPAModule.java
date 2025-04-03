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
    Response IPA_lead_res;
    @Test(priority = 1)
    public void generateCibil() {
        try {
            System.out.println("Hit IPA lead api");
            headers = getHeaders(PropertiesReadWrite.getValue("token"));
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
                    rerunCibilReport("true", null, null);
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
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        response = RestUtils.performGet(url, headers);
        String generate_upload_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/misc/generate-upload-url";
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
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
        File file = new File(System.getProperty("user.dir") + "/src/main/resources/file/bankstatment.pdf");
        RestAssured.baseURI = "https://s3.ap-south-1.amazonaws.com/cgcl-ilos-uat-ui";
        Response response = given()
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
                .multiPart("Content-Type", "application/pdf")
                .multiPart("key", "upload_docs/user/914109_bankstatment.pdf_1743094433634.pdf")
                .multiPart("x-amz-algorithm", "AWS4-HMAC-SHA256")
                .multiPart("x-amz-credential", "ASIA2WCRTZ2VSKRSL6EO/20250403/ap-south-1/s3/aws4_request")
                .multiPart("x-amz-date", "20250403T123320Z")
                .multiPart("x-amz-security-token", "IQoJb3JpZ2luX2VjEIP//////////wEaCmFwLXNvdXRoLTEiSDBGAiEApx52ucnE2sX/ZS2Pi6w1kEIZaeKrghzP33TfvMdyk+8CIQCLiifxB0aHkOfAgyZIuh7wKbk3XmmLxuwlcWLrOnhGRCqRBAjt//////////8BEAEaDDczNDYxMDU3NTAxOSIMa5TGWnoq+p93E638KuUDDXSh/dWoUxomG7WVztfbrus0vlLqClqeHgGE8Zai/Qo7YrzfX9NB3mGKrMPFr2aP8zs6ScXwIFc4XbNKyA7QaOULC3ANG1c2u635Wx8iZ/WCTjQT3u2hQxr/qxUWGs4mCSVcCQSipGc0JWAQi5fLWDnUGnpsihofmzlN+L8zEuMM4OKMKH8CC6/n9PcMng5I9YTZv/zImt7/37CyHMQNt0gLQMduEjy6ECnGaJsTrMY5noaokoI0isBOpgL+4a1U8fPoE+zoZqbIqI19/ASefoLCdFcjxWhYHFLqQL2w7pXbSc9dlfp2PbX3pchEhN4BUGfO+jSZGuuJ8IJKiej1XtqVYEjOo9j/6z2eRBT4iKD3oR4cAo7xeNNTURs3kh7zBLUqNoOA9dwl2Er3bepyt5ONiAc3A3fFKCnwqfeYz55PPPN4SCnqg/fmeDhF1pFd8tmX1zyrkftaOFG9u27ADP8jR+GUbLKnSUR5XXHJpZHPeampWWD2k9yVN5ev57dTV/tI0ZdoVdFwKO5+mBASvZGEl0LMVcKS2Jj+oxsh5g9fnRW+FCO0JdvsdXv1zOVnaiNS/D68c94g0QFKnLjuQ7zo+qTcualXBYD7LeqSVA3Q9/zD9HHZEWF7J4BUKxZ0icr1FtYw8OC5vwY6pAGzzyLYi/V2UbZ8vxPqITXrKcBxFMhUFnwhNDD2+XXePU7DD1C47ueXxjYJMhgbFpRIbk0LSvfpLNVl58QNMRpfJB5OMOXQ5swd+9ORK5DgafH+G6XAqNMW/fsBYcnUB6zLDoV+bvGfC1JlTHB8ZFz5dOz67CjOFZxSnA6OSyU7gbwTpi2Z43cFA4DaSC4NZI285QS8UUKKQAPrpgUOCIJQst5DfA==")


                .multiPart("policy", "eyJleHBpcmF0aW9uIjogIjIwMjUtMDQtMDNUMTI6MzU6MjBaIiwgImNvbmRpdGlvbnMiOiBbWyJzdGFydHMtd2l0aCIsICIkQ29udGVudC1UeXBlIiwgIiJdLCB7ImJ1Y2tldCI6ICJjZ2NsLWlsb3MtdWF0LXVpIn0sIHsia2V5IjogInVwbG9hZF9kb2NzL3VzZXIvOTE0MTA5X2JhbmtzdGF0bWVudC5wZGZfMTc0MzA5NDQzMzYzNC5wZGYifSwgeyJ4LWFtei1hbGdvcml0aG0iOiAiQVdTNC1ITUFDLVNIQTI1NiJ9LCB7IngtYW16LWNyZWRlbnRpYWwiOiAiQVNJQTJXQ1JUWjJWU0tSU0w2RU8vMjAyNTA0MDMvYXAtc291dGgtMS9zMy9hd3M0X3JlcXVlc3QifSwgeyJ4LWFtei1kYXRlIjogIjIwMjUwNDAzVDEyMzMyMFoifSwgeyJ4LWFtei1zZWN1cml0eS10b2tlbiI6ICJJUW9KYjNKcFoybHVYMlZqRUlQLy8vLy8vLy8vL3dFYUNtRndMWE52ZFhSb0xURWlTREJHQWlFQXB4NTJ1Y25FMnNYL1pTMlBpNncxa0VJWmFlS3JnaHpQMzNUZnZNZHlrKzhDSVFDTGlpZnhCMGFIa09mQWd5Wkl1aDd3S2JrM1htbUx4dXdsY1dMck9uaEdSQ3FSQkFqdC8vLy8vLy8vLy84QkVBRWFERGN6TkRZeE1EVTNOVEF4T1NJTWE1VEdXbm9xK3A5M0U2MzhLdVVERFhTaC9kV29VeG9tRzdXVnp0ZmJydXMwdmxMcUNscWVIZ0dFOFphaS9RbzdZcnpmWDlOQjNtR0tyTVBGcjJhUDh6czZTY1h3SUZjNFhiTkt5QTdRYU9VTEMzQU5HMWMydTYzNVd4OGlaL1dDVGpRVDN1MmhReHIvcXhVV0dzNG1DU1ZjQ1FTaXBHYzBKV0FRaTVmTFdEblVHbnBzaWhvZm16bE4rTDh6RXVNTTRPS01LSDhDQzYvbjlQY01uZzVJOVlUWnYvekltdDcvMzdDeUhNUU50MGdMUU1kdUVqeTZFQ25HYUpzVHJNWTVub2Fva29JMGlzQk9wZ0wrNGExVThmUG9FK3pvWnFiSXFJMTkvQVNlZm9MQ2RGY2p4V2hZSEZMcVFMMnc3cFhiU2M5ZGxmcDJQYlgzcGNoRWhONEJVR2ZPK2pTWkd1dUo4SUpLaWVqMVh0cVZZRWpPbzlqLzZ6MmVSQlQ0aUtEM29SNGNBbzd4ZU5OVFVSczNraDd6QkxVcU5vT0E5ZHdsMkVyM2JlcHl0NU9OaUFjM0EzZkZLQ253cWZlWXo1NVBQUE40U0NucWcvZm1lRGhGMXBGZDh0bVgxenlya2Z0YU9GRzl1MjdBRFA4alIrR1ViTEtuU1VSNVhYSEpwWkhQZWFtcFdXRDJrOXlWTjVldjU3ZFRWL3RJMFpkb1ZkRndLTzUrbUJBU3ZaR0VsMExNVmNLUzJKaitveHNoNWc5Zm5SVytGQ08wSmR2c2RYdjF6T1ZuYWlOUy9ENjhjOTRnMFFGS25ManVRN3pvK3FUY3VhbFhCWUQ3TGVxU1ZBM1E5L3pEOUhIWkVXRjdKNEJVS3haMGljcjFGdFl3OE9DNXZ3WTZwQUd6enlMWWkvVjJVYlo4dnhQcUlUWHJLY0J4Rk1oVUZud2hOREQyK1hYZVBVN0REMUM0N3VlWHhqWUpNaGdiRnBSSWJrMExTdmZwTE5WbDU4UU5NUnBmSkI1T01PWFE1c3dkKzlPUks1RGdhZkgrRzZYQXFOTVcvZnNCWWNuVUI2ekxEb1YrYnZHZkMxSmxUSEI4WkZ6NWRPejY3Q2pPRlp4U25BNk9TeVU3Z2J3VHBpMlo0M2NGQTREYVNDNE5aSTI4NVFTOFVVS0tRQVBycGdVT0NJSlFzdDVEZkE9PSJ9XX0=")

                // Replace with actual policy
                .multiPart("x-amz-signature", "1d5d4abb3564a063c7c4eff8ef93338031d0d3090156232ff2fd1480b2234722")
                .multiPart("file", file, "application/pdf") // Attach file
                .when()
                .post()
                .then()// Validate success response
                .extract()
                .response();

        System.out.println("Response of aws upload: " + response.prettyPrint());


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
        String upload_bank_statement_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/ipa/lead/bank-statement-upload/" + PropertiesReadWrite.getValue("obj_id");
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
        String appId = PropertiesReadWrite.getValue("application_id");
        IPA_lead_res = RestUtils.performGet(url+"?view=true", headers);
        validateResponse(IPA_lead_res);
        String acc_no =JsonPath.from(IPA_lead_res.asString()).getString("dt.applicant.primary.bank_acc_details[0].account_number");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("is_prm_app", true);
        requestBody.put("coapp_id", null);
        requestBody.put("app_id", appId); // Use the variable here
        requestBody.put("acc_no", acc_no);
        requestBody.put("dsn", "BANK DOCUMENTS=LATEST 6 MONTHS SAVINGS ACCOUNT BANK STATEMENT");
        requestBody.put("stmt", "cgcl-ilos-uat-ui/upload_docs/user/" + appId + "_bankstatment.pdf_1742474264457.pdf");

        Response upload_bank_statement_res = RestUtils.sendPatchRequest(upload_bank_statement_url, requestBody, headers);
        validateResponse(upload_bank_statement_res);

    }
    @Test(priority = 4)
    public void initiate_novel(){
        String initiate_novel_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/ipa/lead/initiate-novel/" + PropertiesReadWrite.getValue("obj_id");
        headers = getHeaders(PropertiesReadWrite.getValue("token"));
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
        Response uploadBankStatementRes = RestUtils.sendPatchRequest(initiate_novel_url, requestBody, headers);
        validateResponse(uploadBankStatementRes);
    }

@Test(priority = 5 )
public void submitLead() {
    String submit_url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/ipa/lead/submit/" + PropertiesReadWrite.getValue("obj_id");
    headers = getHeaders(PropertiesReadWrite.getValue("token"));
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