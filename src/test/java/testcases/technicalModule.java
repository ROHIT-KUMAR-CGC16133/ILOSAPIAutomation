
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
    String TechUser = PropertiesReadWrite.getValue("CPUser");
    String CPpassword = PropertiesReadWrite.getValue("CPPassword");

    @Test(priority = 0)
    public void fetchHomeLeads() {
        System.out.println("Fetching Home Leads");

        // Construct the URL
        String endPoint = PropertiesReadWrite.getValue("baseURL") +"/ilos/v1/technical/get-home-leads";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("role", "TC");
        queryParams.put("application_id", PropertiesReadWrite.getValue("application_id"));
        String token = PropertiesReadWrite.getValue("token1");
        Map<String, Object> headers;

        headers = getHeaders(TechUser,CPpassword);

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
        Map<String, Object> headers =getHeaders(TechUser,CPpassword);

        // Request Body
        String requestBody = "{ \"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\", \"is_refer\": false }";


        // Send API Request
        Response response = RestUtils.performPost1(endPoint, requestBody, headers);


        // Print API Response
        System.out.println("Response: " + response.asPrettyString());
        Assert.assertEquals(response.getStatusCode(), 200, "API request failed! Expected 200 but got " + response.getStatusCode());
        System.out.println("Self Assigning Leads - Completed");
    }

    @Test(priority = 2)
    public void getAssignedLeads() {
        System.out.println("Fetching Assigned Leads");

        Response response=null;
        Map<String, Object> headers;
        String endPoint = PropertiesReadWrite.getValue("baseURL") +"/ilos/v1/technical/get-assigned-leads";

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("role", "TC");
        queryParams.put("filter_type", "VENDOR_PENDING");
        queryParams.put("application_id", PropertiesReadWrite.getValue("application_id"));
        String token = PropertiesReadWrite.getValue("token1");

        headers = getHeaders(TechUser,CPpassword);; // Get headers with the token
        response = RestUtils.performGet(endPoint, headers, queryParams);


        // Print API Response
        System.out.println("Response: " + response.asPrettyString());
        Assert.assertEquals(response.getStatusCode(), 200, "API request failed! Expected 200 but got " + response.getStatusCode());
        System.out.println("Fetching Assigned Leads - Completed");

    }



    @Test(priority = 3)
    public void completeTechnical() {
        System.out.println("get lead json");
        String url = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/assignee/lead/" + PropertiesReadWrite.getValue("obj_id");

        Map<String, Object> headers = getHeaders(TechUser,CPpassword);
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

            //$$$#
            // Step 1, 2 & 3: Get vendor list,  assign vendor and report upload
            for (int i = 0; i < size1; i++) {
                int propertyId = i + 1;
                System.out.println("\n--- Step 1/2 for Property ID: " + propertyId + " ---");
                getVendorList(propertyId, headers);
                assignVendor(propertyId, headers);
                uploadTechnicalReport(propertyId, headers);

            }

            // Step 4: Self-assign vetter (once)
            selfAssignVetter(headers);

            // Step 5 & 6: Update property and submit valuation
            for (int i = 0; i < size1; i++) {
                int propertyId = i + 1;
                System.out.println("\n--- Step 4/5 for Property ID: " + propertyId + " ---");
                updateProperty(propertyId, headers);
                submitPropertyValuation(propertyId, headers);
            }

            // Step 7 & 8: Approvals
            technicalApproval(headers);
            creditApproval(headers);
            System.out.println("✅ Technical flow completed successfully.");
            System.out.println("Success");

        }
    }

    private void getVendorList(int propertyId, Map<String, Object> headers) {
        String vendorListUrl = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/technical/vendor-list";
        String requestBody = "{ \"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\","
                + "\"property_id\": " + propertyId + ","
                + "\"activity_code\": \"TECHNICAL_1\" }";

        Response vendorResponse = RestUtils.performPost(vendorListUrl, requestBody, headers);
        Assert.assertEquals(vendorResponse.getStatusCode(), 200, "Vendor List Failed for property_id: " + propertyId);
        System.out.println("Vendor List Response for property_id " + (propertyId) + ": " + vendorResponse.prettyPrint());
    }

    private void assignVendor(int propertyId, Map<String, Object> headers) {
        String assignvendorurl = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/technical/assign-vendor";

        String requestBody2 = "{" +
                "\"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\"," +
                "\"property_id\": " + propertyId + "," +
                "\"vendor_code\": \"AGT107\"," +
                "\"vendor_email\": \"rkvaluers@gmail.com\"," +
                "\"valuer_type\": \"EXTERNAL\"," +
                "\"vendor_name\": \"R.K KULHAR & CO2\"," +
                "\"activity_code\": \"TECHNICAL_1\"}";



        Response response2 = RestUtils.performPost(assignvendorurl, requestBody2, headers);
        Assert.assertEquals(response2.getStatusCode(), 200, "Assign Vendor Failed for property_id: " + propertyId);
        System.out.println("Assign Vendor Response: " + response2.prettyPrint());

    }

    private void uploadTechnicalReport(int propertyId, Map<String, Object> headers) {
        String reportUrl = "http://capri-core-nlb-pvt-d60aaf691e8006d3.elb.ap-south-1.amazonaws.com:8285/prv/v1/technical/report";


        String requestBody3 = "{\n" +
                "    \"application_id\": \""+ PropertiesReadWrite.getValue("application_id") +"\",\n" +
                "    \"activity_code\": \"TECHNICAL_1\",\n" +
                "    \"unique_request_id\": \""+ PropertiesReadWrite.getValue("application_id") + "_" + propertyId +"\", \n" +
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


        Response reportResponse = RestUtils.performPost(reportUrl, requestBody3, headers);
        Assert.assertEquals(reportResponse.getStatusCode(), 200, "Upload Report Failed for property_id: " + propertyId);
        System.out.println("Technical Report Response for property_id " + propertyId + ": " + reportResponse.prettyPrint());
    }

    private void selfAssignVetter(Map<String, Object> headers) {
        RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");;
        String endPoint2 = "/ilos/v1/technical/self-assign?role=TVET";


        String requestBody4 = "{" +
                "\"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\"," +
                "\"is_refer\": false" +
                "}";

        Response selfassignvetterresponse = RestUtils.performPost1(endPoint2, requestBody4, headers);

        Assert.assertEquals(selfassignvetterresponse.getStatusCode(), 200, "Self-assign Vetter Failed");
        System.out.println("Response Body1: " + selfassignvetterresponse.prettyPrint());
    }

    private void updateProperty(int propertyId, Map<String, Object> headers) {

        String UpdatePropertyUrl = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/technical/update-property";
        String requestBody5= "{"
                + "\"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\","
                + "\"property_id\": " + propertyId+ ","
                + "\"property_data\": {"
                +     "\"property_locality\": \"Urban\""
                + "}"
                + "}";

        Response UpdatePropertyResponse = RestUtils.performPost(UpdatePropertyUrl, requestBody5, headers);


        Assert.assertEquals(UpdatePropertyResponse.getStatusCode(), 200, "Update Property Failed for property_id: " + propertyId);
        System.out.println("UpdateProperty Response for property_id " + propertyId + ": " + UpdatePropertyResponse.prettyPrint());
    }

    private void submitPropertyValuation(int propertyId, Map<String, Object> headers) {
        String submitPropertyValuationUrl = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/technical/submit-property-valuation";

        String requestBodySubmitValuation = "{"
                + "\"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\","
                + "\"property_id\": " + propertyId
                + "}";

        Response submitValuationResponse = RestUtils.performPost(submitPropertyValuationUrl, requestBodySubmitValuation, headers);
        Assert.assertEquals(submitValuationResponse.getStatusCode(), 200, "Submit Valuation Failed for property_id: " + propertyId);
        System.out.println("SubmitPropertyValuation Response for property_id " +propertyId + ": " + submitValuationResponse.prettyPrint());
    }

    private void technicalApproval(Map<String, Object> headers) {
        String technicalApprovalUrl = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/technical/technical-approval";
        String requestBody6 = "{"
                + "\"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\","
                + "\"remarks\": \"ok\","
                + "\"status\": \"POSITIVE\","
                + "\"last_comment_id\": null"
                + "}";
        Response technicalApprovalResponse = RestUtils.performPost(technicalApprovalUrl, requestBody6, headers);
        Assert.assertEquals(technicalApprovalResponse.getStatusCode(), 200, "Technical Approval Failed");
        System.out.println("Technical Approval Response: " + technicalApprovalResponse.prettyPrint());
    }

    private void creditApproval(Map<String, Object> headers) {
        String creditApprovalUrl = PropertiesReadWrite.getValue("baseURL") + "/ilos/v1/technical/credit-approval";

        String requestBody7 = "{"
                + "\"application_id\": \"" + PropertiesReadWrite.getValue("application_id") + "\""
                + "}";

        Response creditApprovalResponse = RestUtils.performPost(creditApprovalUrl, requestBody7, headers);
        Assert.assertEquals(creditApprovalResponse.getStatusCode(), 200, "Credit Approval Failed");
        System.out.println("CreditApproval Response: " + creditApprovalResponse.prettyPrint());
    }


}