
        package testcases;
//		https://codeshare.io/64K6Bo legal lead


        import io.restassured.RestAssured;
        import io.restassured.response.Response;
        import org.testng.Assert;
        import org.testng.annotations.Test;
        import utils.PropertiesReadWrite;
        import utils.RestUtils;

        import java.util.Map;

        import static payloads.Header.getHeaders;

        public class CommercialApprovalModule {
            String BM_User = PropertiesReadWrite.getValue("BM_User");
            String CPPassword = PropertiesReadWrite.getValue("CPPassword");



            @Test(priority = 1)
            public void globalReAssignLead() {
                { //api 1
                    System.out.println("Global Re-Assignment Leads");

                    // Construct the URL
                    RestAssured.baseURI = PropertiesReadWrite.getValue("baseURL");;
                    String endPoint = "/ilos/v1/search/reassign";

                    // Headers
                    Map<String, Object> headers = getHeaders(BM_User, CPPassword);


                    // Request Body

                    String requestBody = "{\n" +
                            "    \"emp_name\": \"BM_User\",\n" +
                            "    \"emp_email\": \"BM_User@capriglobal.in\",\n" +
                            "    \"emp_code\": \"BUS001\",\n" +
                            "    \"application_id\": \""+ PropertiesReadWrite.getValue("application_id") +"\",\n" +
                            "    \"module_name\": \"COMMERCIAL_APPROVAL\"\n" +
                            "}";


                    // Send API Request
                    Response response = RestUtils.performPatch1(endPoint, requestBody, headers);


                    // Print API Response
                    System.out.println("Response: " + response.asPrettyString());
                    Assert.assertEquals(response.getStatusCode(), 200, "API request failed! Expected 200 but got " + response.getStatusCode());
                    System.out.println("Global Re-Assignment Leads");
                }

            }
}