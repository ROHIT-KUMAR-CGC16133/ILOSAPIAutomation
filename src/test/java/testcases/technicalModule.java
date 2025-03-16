package testcases;
//		https://codeshare.io/AprpPN tenical lead

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.util.HashMap;
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




    }