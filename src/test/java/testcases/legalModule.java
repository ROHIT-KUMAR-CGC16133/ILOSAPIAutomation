
        package testcases;
//		https://codeshare.io/64K6Bo legal lead


import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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




        }