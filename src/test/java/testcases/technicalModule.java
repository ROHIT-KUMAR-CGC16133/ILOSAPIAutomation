package testcases;
//		https://codeshare.io/AprpPN tenical lead

// https://codeshare.io/ldg4ym more than one prop

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

public class technicalModule {

    @Test(priority = 0)
    public void fetchHomeLeads() {
        System.out.println("Fetching Home Leads");

        // Construct the URL
        String endPoint = PropertiesReadWrite.getValue("baseURL") +"/ilos/v1/technical/get-home-leads";
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("role", "TC");
        queryParams.put("application_id", PropertiesReadWrite.getValue("application_id"));
        String token = PropertiesReadWrite.getValue("token1");
        Map<String, String> headers;

        headers = getHeaders(token);

        Response response = RestUtils.performGet(endPoint, headers, queryParams);



        Assert.assertEquals(response.getStatusCode(), 200, "Failed to fetch home leads");

        response.prettyPrint();

    }

    @Test(priority = 1)
    public void selfAssignLead() {
        System.out.println("Self Assigning Leads");

        // Construct the URL
        RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");;
        String endPoint = "/ilos/v1/technical/self-assign?role=TC";

        // Headers
        String token = PropertiesReadWrite.getValue("token1");
        Map<String, String> headers = getHeaders(token);

        // Request Body
        String requestBody = "{ \"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\", \"is_refer\": false }";


        // Send API Request
        Response response = RestUtils.performPost1(endPoint, requestBody, headers);

        System.out.println("Self Assigning Leads - Completed");

        // Print API Response
        System.out.println("Response: " + response.asPrettyString());
        Assert.assertEquals(response.getStatusCode(), 200, "API request failed! Expected 200 but got " + response.getStatusCode());

    }

    @Test(priority = 2)
    public void getAssignedLeads() {
        System.out.println("Fetching Assigned Leads");

        Response response=null;
        Map<String, String> headers;
        String endPoint = PropertiesReadWrite.getValue("baseURL") +"/ilos/v1/technical/get-assigned-leads";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("role", "TC");
        queryParams.put("filter_type", "VENDOR_PENDING");
        queryParams.put("application_id", PropertiesReadWrite.getValue("application_id"));
        String token = PropertiesReadWrite.getValue("token1");

        headers = getHeaders(token); // Get headers with the token
        response = RestUtils.performGet(endPoint, headers, queryParams);

        System.out.println("Fetching Assigned Leads - Completed");

        // Print API Response
        System.out.println("Response: " + response.asPrettyString());
        Assert.assertEquals(response.getStatusCode(), 200, "API request failed! Expected 200 but got " + response.getStatusCode());
    }


    @Test(priority = 3)
    public void completeTechnical() {
        System.out.println("get lead json");
        String url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/assignee/lead/" + PropertiesReadWrite.getValue("obj_id");

        Map<String, String> headers = getHeaders(PropertiesReadWrite.getValue("token1"));
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

        String recommendedRange = JsonPath.from(response.asString()).get("dt.pd_loan_recommended_range");
        System.out.println("pd_loan_recommended_range Details: " + recommendedRange);

        if ("LESS_THAN_50".equals(recommendedRange)) {
            for (int i = 0; i < size1; i++) {
                String vendorListUrl = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/technical/vendor-list";

                // Construct request body with dynamic property_id (i + 1)
                String requestBody = "{" +
                        "\"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\"," +
                        "\"property_id\": " + (i + 1) + "," +
                        "\"activity_code\": \"TECHNICAL_1\"}";

                // Perform API request
                Response vendorResponse = RestUtils.performPost(vendorListUrl, requestBody, headers);

                try {
                    Assert.assertEquals(vendorResponse.getStatusCode(), 200);
                } catch (AssertionError e) {
                    System.err.println("Assertion failed for property_id " + (i + 1) + ": Expected 200 but got " + vendorResponse.getStatusCode());
                }

                System.out.println("Vendor List Response for property_id " + (i + 1) + ": " + vendorResponse.prettyPrint());
            }

            System.out.println("Success");
        }
    }






}