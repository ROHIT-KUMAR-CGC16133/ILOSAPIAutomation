package read_api;

import generic.Generic;
import io.restassured.response.Response;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static payloads.Header.getHeaders;

public class Global_read_view_api {
    String base_url = PropertiesReadWrite.getValue("baseURL");
    Map<String, Object> headers;
    String appId = PropertiesReadWrite.getValue("application_id");
    String user_id = PropertiesReadWrite.getValue("global_readOnly_user");
    String user_pass = PropertiesReadWrite.getValue("global_readOnly_password");


    @Test(priority = 1)
    public void global_search_api() throws IOException {
        headers=getHeaders(user_id,user_pass);
        String endPoint = base_url + "/ilos/v1/search";
        Map<String, Object> queryParams = Map.of(
                "application_id", appId,
                "page", 1,
                "pageSize", 5
        );
        Response response = RestUtils.performGet(endPoint, headers, queryParams);
        Generic.validateResponse(response);
        response.then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/global_search_res.json"));
        String expectedJson = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("response_validation/global_search.json"),
                StandardCharsets.UTF_8
        );

        String actualJson = response.getBody().asString();

        JSONAssert.assertEquals(expectedJson, actualJson, true);

    }
    @Test(priority = 2)
    public void verify_designation_list_api() throws IOException {
        headers=getHeaders(user_id,user_pass);
        String endPoint = base_url + "/ilos/v1/misc/designation-list";
        Response response = RestUtils.performGet(endPoint, headers);
        Generic.validateResponse(response);
        response.then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/designation-list_schema.json"));
        String expectedJson = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("response_validation/designation-list_res.json"),
                StandardCharsets.UTF_8
        );
        String actualJson = response.getBody().asString();
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }
    @Test(priority = 3,enabled = false)
    public void verify_master_data_api() throws IOException {
        headers=getHeaders(user_id,user_pass);
        Response response = RestUtils.performGet(base_url+"/ilos/v1/proxy/pragati/master-data", headers);
        Generic.validateResponse(response);
       // response.prettyPrint();
        response.then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/master-data_schema.json"));
        String expectedJson = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("response_validation/master-data_res.json"),
                StandardCharsets.UTF_8
        );
        String actualJson = response.getBody().asString();
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }
    @Test(priority = 4)
    public void verify_dms_master_data_api() throws IOException {
        headers=getHeaders(user_id,user_pass);
        Response response = RestUtils.performGet(base_url+"/ilos/v1/misc/dms-master-data", headers);
        Generic.validateResponse(response);
        response.then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/dms_master_data_schema.json"));
        String expectedJson = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("response_validation/dms_master_data_res.json"),
                StandardCharsets.UTF_8
        );
        String actualJson = response.getBody().asString();
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }
    @Test(priority = 5)
    public void verify_branch_mapping_api() throws IOException {
        headers=getHeaders(user_id,user_pass);
        Response response = RestUtils.performGet(base_url+"/ilos/v1/misc/branch-mapping", headers,
                Map.of("is_additional_branch", true));
        Generic.validateResponse(response);
        response.then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/branch_mapping_schema.json"));
        String expectedJson = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("response_validation/branch_mapping_res.json"),
                StandardCharsets.UTF_8
        );
        String actualJson = response.getBody().asString();
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }
    @Test(priority = 6)
    public void verify_assignee_lead_api() throws IOException {
        headers=getHeaders(user_id,user_pass);
        Response response = RestUtils.performGet(base_url+"/ilos/v1/assignee/lead/"+PropertiesReadWrite.getValue("obj_id"), headers);
        Generic.validateResponse(response);
       // response.prettyPrint();
        response.then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/assignee_lead_schema.json"));
        String expectedJson = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("response_validation/assignee_lead_res.json"),
                StandardCharsets.UTF_8
        );
        String actualJson = response.getBody().asString();
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

}
