
        package testcases;
//		https://codeshare.io/64K6Bo legal lead


import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static payloads.Header.getHeaders;

        public class legalModule {

        String TechUser = PropertiesReadWrite.getValue("TechUser");
        String CPPassword = PropertiesReadWrite.getValue("CPPassword");

            @Test(priority = 1)
            public void selfAssignLead() { //api 1
                System.out.println("Self Assigning Leads");

                // Construct the URL
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");;
                String endPoint = "/ilos/v1/legal/self-assign?role=LC&prop_assign=False";

                // Headers
                String token = PropertiesReadWrite.getValue("token2");
                Map<String, Object> headers = getHeaders(TechUser, CPPassword);

                // Request Body
                String requestBody = "{ \"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\" }";


                // Send API Request
                Response response = RestUtils.performPost1(endPoint, requestBody, headers);


                // Print API Response
                System.out.println("Response: " + response.asPrettyString());
                Assert.assertEquals(response.getStatusCode(), 200, "API request failed! Expected 200 but got " + response.getStatusCode());
                System.out.println("Self Assigning Leads - Completed");
            }

            @Test(priority = 2)
            public void completeLegal() {
                System.out.println("get lead json");
                String url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/assignee/lead/" + PropertiesReadWrite.getValue("obj_id");

                Map<String, Object> headers = getHeaders(TechUser, CPPassword);
                Response response = RestUtils.performGet(url, headers);

                Map<String, Object> headers1 = getHeaders(TechUser, CPPassword);
                Response response1 = RestUtils.performGet(url, headers);

                try {
                    Assert.assertEquals(response.getStatusCode(), 200);
                } catch (AssertionError e) {
                    System.err.println("Assertion failed: Expected status code 200, but got " + response.getStatusCode());
                }

                System.out.println("get lead response " + response.prettyPrint());

                // Get property_details and check its size
                List<Object> propertyDetails = JsonPath.from(response.asString()).getList("dt.applicant.primary.property_details");
                int size1 = propertyDetails.size();
                System.out.println("Property Details Size1: " + size1);

                for (int i = 0; i < size1; i++) {
                    int propertyId = i + 1;
                    System.out.println("\n--- Step 1 for allocate vendor: " + propertyId + " ---");
                    allocateVendorToProperty(propertyId, headers);

                }

                // Step 4: LC Submit (once)
                lcSubmit(headers);

                for (int i = 0; i < size1; i++) {
                    int propertyId = i + 1;
                    System.out.println("\n--- Step 1 for allocate vendor: " + propertyId + " ---");
                   submitLVForm(propertyId, headers1);
                    updateLegalDocument(propertyId, headers1);
                    submitLVForm1(propertyId, headers1);


                }

                lmSelfAssign(headers);

                List<Integer> propertyIds = new ArrayList<>();
                for (int i = 0; i < size1; i++) {
                    propertyIds.add(i + 1);
                }
               submitLegalRecommendation(propertyIds, headers);

                creditApproval(headers);
            }



            private void allocateVendorToProperty(int propertyId, Map<String, Object> headers) {
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL"); // example: https://ilosapi-uat.capriglobal.in
                String allocatevendorURL = "/ilos/v1/legal/property_allocation?role=LC&allocate_vendor=True";
                System.out.println("allocatevendorURL url is " + allocatevendorURL);


                String requestBody ="{\n" +
                        "    \"property_id\":" +propertyId+",\n" +
                        "    \"application_id\": \""+ PropertiesReadWrite.getValue("application_id") +"\",\n" +
                        "    \"vendor_email\": \"vijay.jain_Ext@capriapps.in\",\n" +
                        "    \"vendor_id\": \"659e8f979189024c4f4c7661\",\n" +
                        "    \"vendor_name\": \"vijay jain\"\n" +
                        "}";

                        Response allocatevendorResponse = RestUtils.sendPatchRequest(allocatevendorURL, requestBody, headers);
                Assert.assertEquals(allocatevendorResponse.getStatusCode(), 200, "Vendor List Failed for property_id: " + propertyId);
                System.out.println("Vendor List Response for property_id " + (propertyId) + ": " + allocatevendorResponse.prettyPrint());
            }

            private void lcSubmit(Map<String, Object> headers) {
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");;
                String lcSubmitendPoint = "/ilos/v1/legal/lc-submit?role=LC";

                String lcSubmitrequestBody = "{\"application_id\":\""+ PropertiesReadWrite.getValue("application_id") +"\"}";

                Response selfassignvetterresponse = RestUtils.sendPatchRequest(lcSubmitendPoint, lcSubmitrequestBody, headers);

                Assert.assertEquals(selfassignvetterresponse.getStatusCode(), 200, "Self-assign Vetter Failed");
                System.out.println("Response Body1 is: " + selfassignvetterresponse.prettyPrint());
            }

            private void submitLVForm(int propertyId, Map<String, Object> headers1) {
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");
                String lvSubmitURL = "/ilos/v1/legal/lv-form-submit?role=LV";
                System.out.println("LV Submit URL is " + lvSubmitURL);

                File pfxFile = new File("src/test/resources/client-identity.pfx"); // Update path as needed

                Response response = given()
                        .headers(headers1)
                        .multiPart("property_id", String.valueOf(propertyId))
                        .multiPart("application_id", PropertiesReadWrite.getValue("application_id"))
                        .multiPart("password", "Qwerty%40123")
                        .multiPart("pfx_file", pfxFile)
                        .multiPart("logo_url", "cgcl-ilos-uat-ui/upload_docs/user/910370_CLONE1_dummy.png_1743098400000.png")
                        .multiPart("letterhead", "test")
                        .multiPart("date", "1743311618423")
                        .multiPart("report_status", "positive")
                        .multiPart("lop_person_name", "test")
                        .multiPart("proposed_owner_names", "[\"test\"]")
                        .multiPart("payment_made_favor_of", "test")
                        .multiPart("property_owners", "[{\"owner_id\":920195,\"owner_name\":\"TEJPAL CHOUDHARY\",\"owner_type\":\"Primary Applicant\"}]")
                        .multiPart("property_boundaries", "{\"south\":\"test\",\"north\":\"test\",\"east\":\"test\",\"west\":\"test\"}")
                        .multiPart("property_details", "{\"addressLine1\":\"test\",\"addressLine2\":\"test\",\"addressLine3\":\"test\",\"addressLine4\":\"test\",\"pincode\":\"110038\",\"tehsil\":\"test\",\"district\":\"NEW DELHI\",\"state\":\"DELHI\",\"country\":\"India\"}")
                        .multiPart("nature_of_property_text", "{\"land_type\":\"Freehold\",\"property_type\":\"Residential\"}")
                        .multiPart("examined_doc_text", "<ol><li>test</li></ol>")
                        .multiPart("legal_intervention_text", "{\"type\":\"no\",\"summary\":\"\"}")
                        .multiPart("conclusion_text", "{\"type\":\"no\",\"summary\":\"\"}")
                        .multiPart("pre_disbursal_text", "<ol><li>test</li></ol>")
                        .multiPart("legal_firm_name", "test")
                        .multiPart("authorized_person_name", "test")
                        .multiPart("authorized_person_designation", "test")
                        .multiPart("footer_text", "test")
                        .multiPart("receipt_date", "1743311618423")
                        .multiPart("receipt_no", "2345543")
                        .multiPart("pdd_text", "<ol><li>test</li></ol>")
                        .multiPart("enclosure_type", "<p>test</p>")
                        .when()
                        .post(lvSubmitURL)
                        .then()
                        .extract().response();

                try {
                    Assert.assertEquals(response.getStatusCode(), 200);
                } catch (AssertionError e) {
                    System.err.println("Assertion failed: Expected status code 200, but got " + response.getStatusCode());
                }

                System.out.println("LV Form Submit Response for property_id " + propertyId + ": " + response.prettyPrint());
            }

            private void updateLegalDocument(int propertyId, Map<String, Object> headers) {
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");
                String documentPatchURL = "/ilos/v1/legal/document-patch?role=LV";
                System.out.println("Document Patch URL: " + documentPatchURL);

                String requestBody = "{\n" +
                        "    \"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\",\n" +
                        "    \"property_id\": " + propertyId + ",\n" +
                        "    \"document_type\": \"VERIFICATION_DOCUMENT\",\n" +
                        "    \"document_url\": \"cgcl-ilos-uat-ui/upload_docs/user/910370_CLONE1_axis-bank-file_2.pdf_1743098400000.pdf\",\n" +
                        "    \"is_search_report\": true\n" +
                        "}";

                Response documentPatchResponse = RestUtils.performPost1(documentPatchURL, requestBody, headers);
                Assert.assertEquals(documentPatchResponse.getStatusCode(), 200, "Legal Document Patch Failed for property_id: " + propertyId);
                System.out.println("Legal Document Patch Response for property_id " + propertyId + ": " + documentPatchResponse.prettyPrint());
            }

            private void submitLVForm1(int propertyId, Map<String, Object> headers) {
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL"); // ex: https://ilosapi-uat.capriglobal.in
                String lvSubmitURL = "/ilos/v1/legal/lv-submit?role=LV";
                System.out.println("LV Submit URL: " + lvSubmitURL);

                String requestBody = "{\n" +
                        "    \"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\",\n" +
                        "    \"property_id\": " + propertyId + ",\n" +
                        "    \"vendor_remarks\": \"ok\"\n" +
                        "}";

                Response lvSubmitResponse = RestUtils.sendPatchRequest(lvSubmitURL, requestBody, headers);
                Assert.assertEquals(lvSubmitResponse.getStatusCode(), 200, "LV Submit Failed for property_id: " + propertyId);
                System.out.println("LV Submit one Response for property_id " + propertyId + ": " + lvSubmitResponse.prettyPrint());
            }

            private void lmSelfAssign(Map<String, Object> headers) {
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");;
                String lmAssignendPoint = "/ilos/v1/legal/self-assign?role=LM&prop_assign=False";

                String lmAssignrequestBody = "{\"application_id\":\""+ PropertiesReadWrite.getValue("application_id") +"\"}";

                Response selfassignLMresponse = RestUtils.performPost1(lmAssignendPoint, lmAssignrequestBody, headers);

                Assert.assertEquals(selfassignLMresponse.getStatusCode(), 200, "Self-assign Vetter Failed");
                System.out.println("Response Body1 is: " + selfassignLMresponse.prettyPrint());
            }

            private void submitLegalRecommendation(List<Integer> propertyIds, Map<String, Object> headers) {
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL"); // ex: https://ilosapi-uat.capriglobal.in
                String submitURL = "/ilos/v1/legal/submit-recommend?role=LM";
                System.out.println("Legal Submit Recommendation URL: " + submitURL);

                StringBuilder propertyIdArray = new StringBuilder("[");
                for (int i = 0; i < propertyIds.size(); i++) {
                    propertyIdArray.append(propertyIds.get(i));
                    if (i < propertyIds.size() - 1) {
                        propertyIdArray.append(", ");
                    }
                }
                propertyIdArray.append("]");

                String requestBody = "{\n" +
                        "    \"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\",\n" +
                        "    \"remarks\": \"ok\",\n" +
                        "    \"status\": \"APPROVE\",\n" +
                        "    \"is_referred\": false,\n" +
                        "    \"designation\": \"\",\n" +
                        "    \"reject_reasons\": \"\",\n" +
                        "    \"property_id\": " + propertyIdArray.toString() + ",\n" +
                        "    \"is_single_prop\": false\n" +
                        "}";

                Response response = RestUtils.sendPatchRequest(submitURL, requestBody, headers);
                Assert.assertEquals(response.getStatusCode(), 200, "Submit Recommendation Failed");
                System.out.println("Submit Recommendation Response: " + response.prettyPrint());
            }

            private void creditApproval(Map<String, Object> headers) {
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");;
                String creditApprovalendPoint = "/ilos/v1/legal/credit-approval";

                String creditApprovalrequestBody = "{\"application_id\":\""+ PropertiesReadWrite.getValue("application_id") +"\"}";

                Response creditApprovalresponse = RestUtils.performPost1(creditApprovalendPoint, creditApprovalrequestBody, headers);

                Assert.assertEquals(creditApprovalresponse.getStatusCode(), 200, "Self-assign Vetter Failed");
                System.out.println("creditApprovalrequestBody Response Body is: " + creditApprovalresponse.prettyPrint());
            }

        }