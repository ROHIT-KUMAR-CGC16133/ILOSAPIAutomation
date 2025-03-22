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

                /// ////

                String assignvendorurl = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/technical/assign-vendor";

                // Construct request body
                String requestBody2 = "{" +
                        "\"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\"," +
                        "\"property_id\": " + (i + 1) + "," +
                        "\"vendor_code\": \"AGT107\"," +
                        "\"vendor_email\": \"rkvaluers@gmail.com\"," +
                        "\"valuer_type\": \"EXTERNAL\"," +
                        "\"vendor_name\": \"R.K KULHAR & CO2\"," +
                        "\"activity_code\": \"TECHNICAL_1\"}";

                // Perform API request
                Response response2 = RestUtils.performPost(assignvendorurl, requestBody2, headers);

                try {
                    Assert.assertEquals(response2.getStatusCode(), 200);
                } catch (AssertionError e) {
                    System.err.println("Assertion failed: Expected status code 200, but got " + response2.getStatusCode());
                }

                System.out.println("Assign Vendor Response: " + response2.prettyPrint());

                /// ////////////
                String reportUrl = "http://capri-core-nlb-pvt-d60aaf691e8006d3.elb.ap-south-1.amazonaws.com:8285/prv/v1/technical/report";

                // Construct request body dynamically
                String requestBody3 = "{\n" +
                        "    \"application_id\": \""+ PropertiesReadWrite.getValue("application_id") +"\",\n" +
                        "    \"activity_code\": \"TECHNICAL_1\",\n" +
                        "    \"unique_request_id\": \""+ PropertiesReadWrite.getValue("application_id") + "_" + (i + 1) +"\", \n" +
                        "    \"techapp_unique_id\": 910904,\n" +
                        "    \"report_file\": \""+PropertiesReadWrite.getValue("technicalReport")+"\",\n" +
                       // "    \"report_file\": \"JVBERi0xLjQKJeLjz9MKMyAwIG9iago8PC9\",\n" +
                        "    \"report_filename\": \"valuation_report.pdf\",\n" +
                        "    \"report_meta\": {\n" +
                        "        \"property_owner_name\": \"Rameshwar Patil\",\n" +
                        "        \"property_ownership\": \"JOINT\",\n" +
                        "        \"is_first_house\": false,\n" +
                        "        \"branch_distance_in_kms\": 5,\n" +
                        "        \"property_type\": \"RESIDENTIAL\",\n" +
                        "        \"property_sub_type\": \"FLAT\",\n" +
                        "        \"occupancy_type\": \"RENTED\",\n" +
                        "        \"occupied_by\": \"RENTED\",\n" +
                        "        \"occupancy_since\": \"1 YEAR\",\n" +
                        "        \"development_authority_type\": \"URBAN DEVELOPMENT AUTHORITY\",\n" +
                        "        \"property_address_line_1\": \"Sanjay Nagar\",\n" +
                        "        \"property_address_line_2\": \"Lane Number 5\",\n" +
                        "        \"property_state\": \"Maharashtra\",\n" +
                        "        \"property_district\": \"Pune\",\n" +
                        "        \"property_taluka\": \"Haveli\",\n" +
                        "        \"property_town\": \"Pune\",\n" +
                        "        \"property_landmark\": \"Sant Tukaram Mandir\",\n" +
                        "        \"population\": \"POPULATION LESS THAN 10000\",\n" +
                        "        \"development_stage\": 123,\n" +
                        "        \"is_under_construction\": false,\n" +
                        "        \"completion_date\": \"2022-06-30\",\n" +
                        "        \"completion_percentage\": 100,\n" +
                        "        \"is_apf\": true,\n" +
                        "        \"apf_projects\": \"projects\",\n" +
                        "        \"apf_wing\": \"wing\",\n" +
                        "        \"apf_block\": \"bloks\",\n" +
                        "        \"is_builder_project\": true,\n" +
                        "        \"builder_name\": \"mayannk\",\n" +
                        "        \"project_classification\": \"COMMERCIAL\",\n" +
                        "        \"builder_project_name\": \"nbuilder_project_name\",\n" +
                        "        \"relation_with_builder\": \"NA\",\n" +
                        "        \"project_type\": \"COMMERCIAL COMPLEX\",\n" +
                        "        \"is_layout_approved\": \"NO\",\n" +
                        "        \"is_building_plan_approved\": \"NA\",\n" +
                        "        \"is_rera_approved\": \"YES\",\n" +
                        "        \"is_rera_applicable\": \"YES\",\n" +
                        "        \"habitation_percentage\": 10,\n" +
                        "        \"property_hold_type\": \"FREEHOLD\",\n" +
                        "        \"is_multi_tenanted\": false,\n" +
                        "        \"property_residual_age\": 15,\n" +
                        "        \"property_structure_type\": \"TIN SHED\",\n" +
                        "        \"property_land_area_sqft\": 1500.0,\n" +
                        "        \"property_land_rate_sqft\": 2500.0,\n" +
                        "        \"property_carpet_area_sqft\": 1200.0,\n" +
                        "        \"construction_area\": 1500.0,\n" +
                        "        \"construction_value\": 2500000.0,\n" +
                        "        \"proposed_construction_area\": 1800.0,\n" +
                        "        \"proposed_construction_value\": 3000000.0,\n" +
                        "        \"build_up_area\": null,\n" +
                        "        \"build_up_value\": null,\n" +
                        "        \"proposed_build_up_area\": null,\n" +
                        "        \"proposed_build_up_value\": null,\n" +
                        "        \"super_build_up_area\": null,\n" +
                        "        \"super_build_up_value\": null,\n" +
                        "        \"proposed_super_build_up_area\": null,\n" +
                        "        \"proposed_super_build_up_value\": null,\n" +
                        "        \"current_market_value\": 25000000,\n" +
                        "        \"stage_percentage_of_construction\": null,\n" +
                        "        \"construction_recommend_percentage\": null,\n" +
                        "        \"collateral_location\": \"RURA\",\n" +
                        "        \"is_commercial_real_state\": false,\n" +
                        "        \"is_converted_residential_use\": false,\n" +
                        "        \"is_building_construction_approval_plan\": false,\n" +
                        "        \"is_product_sahaj\": true,\n" +
                        "        \"is_product_unnati\": true,\n" +
                        "        \"is_property_in_community_dominated_area\": false\n" +
                        "    }\n" +
                        "}";

                // Perform API request
                Response reportResponse = RestUtils.performPost(reportUrl, requestBody3, headers);

                try {
                    Assert.assertEquals(reportResponse.getStatusCode(), 200);
                } catch (AssertionError e) {
                    System.err.println("Assertion failed for property_id " + (i + 1) + ": Expected 200 but got " + reportResponse.getStatusCode());
                }

                System.out.println("Technical Report Response for property_id " + (i + 1) + ": " + reportResponse.prettyPrint());


                /// ///////

                // Construct API URL

                RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");;
                String endPoint2 = "/ilos/v1/technical/self-assign?role=TVET";

                // Construct request body
                String requestBody4 = "{" +
                        "\"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\"," +
                        "\"is_refer\": false" +
                        "}";

                Response selfassignvetterresponse = RestUtils.performPost1(endPoint2, requestBody4, headers);

                // Validate response
                try {
                    Assert.assertEquals(selfassignvetterresponse.getStatusCode(), 200);
                    System.out.println("API request successful!");
                } catch (AssertionError e) {
                    System.err.println("Assertion failed: Expected 200 but got " + selfassignvetterresponse.getStatusCode());
                }

                // Print response
                System.out.println("Response Body1: " + selfassignvetterresponse.prettyPrint());
            //#


///
            }

            System.out.println("Success");
            
        }
    }






}