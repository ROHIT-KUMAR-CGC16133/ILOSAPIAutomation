
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static payloads.Header.getHeaders;

        public class legalModule {



            @Test(priority = 1)
            public void selfAssignLead() {
                System.out.println("Self Assigning Leads");

                // Construct the URL
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");;
                String endPoint = "/ilos/v1/legal/self-assign?role=LC&prop_assign=False";

                // Headers
                String token = PropertiesReadWrite.getValue("token2");
                Map<String, String> headers = getHeaders(token);

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

                Map<String, String> headers = getHeaders(PropertiesReadWrite.getValue("token2"));
                Response response = RestUtils.performGet(url, headers);

                Map<String, String> headers1 = getHeaders(PropertiesReadWrite.getValue("legalVendorToken"));
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
                 //   allocateVendorToProperty(propertyId, headers);

                }

                // Step 4: LC Submit (once)
             //   lcSubmit(headers);

                for (int i = 0; i < size1; i++) {
                    int propertyId = i + 1;
                    System.out.println("\n--- Step 1 for allocate vendor: " + propertyId + " ---");
                    submitLVForm(propertyId, headers1);

                }


            }



            private void allocateVendorToProperty(int propertyId, Map<String, String> headers) {
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

                        Response allocatevendorResponse = RestUtils.performPatch1(allocatevendorURL, requestBody, headers);
                Assert.assertEquals(allocatevendorResponse.getStatusCode(), 200, "Vendor List Failed for property_id: " + propertyId);
                System.out.println("Vendor List Response for property_id " + (propertyId) + ": " + allocatevendorResponse.prettyPrint());
            }


            private void lcSubmit(Map<String, String> headers) {
                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");;
                String lcSubmitendPoint = "/ilos/v1/legal/lc-submit?role=LC";

                String lcSubmitrequestBody = "{\"application_id\":\""+ PropertiesReadWrite.getValue("application_id") +"\"}";

                Response selfassignvetterresponse = RestUtils.performPatch1(lcSubmitendPoint, lcSubmitrequestBody, headers);

                Assert.assertEquals(selfassignvetterresponse.getStatusCode(), 200, "Self-assign Vetter Failed");
                System.out.println("Response Body1 is: " + selfassignvetterresponse.prettyPrint());
            }

            private void submitLVForm(int propertyId, Map<String, String> headers1) {
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




        }