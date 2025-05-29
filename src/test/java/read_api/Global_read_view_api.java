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
    private final String baseUrl = PropertiesReadWrite.getValue("baseURL");
    private final String appId = PropertiesReadWrite.getValue("application_id");
    private final String userId = PropertiesReadWrite.getValue("global_readOnly_user");
    private final String userPass = PropertiesReadWrite.getValue("global_readOnly_password");

    private Map<String, Object> headers;

    private void validateApiResponse(String endpoint, Map<String, Object> queryParams, String schemaPath, String expectedResponsePath) throws IOException {
        headers = getHeaders(userId, userPass);
        Response response = RestUtils.performGet(endpoint, headers, queryParams);
        response.prettyPrint();
        Generic.validateResponse(response);
        response.then().assertThat().body(matchesJsonSchemaInClasspath(schemaPath));
        String expectedJson = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(expectedResponsePath),
                StandardCharsets.UTF_8
        );
        JSONAssert.assertEquals(expectedJson, response.getBody().asString(), true);
    }

    @Test(priority = 1)
    public void global_search_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/search",
                Map.of("application_id", appId, "page", 1, "pageSize", 5),
                "schemas/global_search_schema.json",
                "response_validation/global_search_res.json"
        );
    }

    @Test(priority = 2)
    public void verify_designation_list_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/misc/designation-list",
                null,
                "schemas/designation-list_schema.json",
                "response_validation/designation-list_res.json"
        );
    }

    @Test(priority = 3, enabled = false)
    public void verify_master_data_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/proxy/pragati/master-data",
                null,
                "schemas/master-data_schema.json",
                "response_validation/master-data_res.json"
        );
    }

    @Test(priority = 4)
    public void verify_dms_master_data_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/misc/dms-master-data",
                null,
                "schemas/dms_master_data_schema.json",
                "response_validation/dms_master_data_res.json"
        );
    }

    @Test(priority = 5)
    public void verify_branch_mapping_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/misc/branch-mapping",
                Map.of("is_additional_branch", true),
                "schemas/branch_mapping_schema.json",
                "response_validation/branch_mapping_res.json"
        );
    }

    @Test(priority = 6)
    public void verify_assignee_lead_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/assignee/lead/" + PropertiesReadWrite.getValue("obj_id"),
                null,
                "schemas/assignee_lead_schema.json",
                "response_validation/assignee_lead_res.json"
        );
    }
    @Test(priority = 7)
    public void verify_lead_config_api() throws IOException {
        headers = getHeaders(userId, userPass);
        Response response = RestUtils.performGet(
                baseUrl + "/ilos/v1/misc/lead-config",
                headers,
                Map.of("section", "references")
        );

      // response.prettyPrint();
        Generic.validateResponse(response);
        response.then().assertThat().body(matchesJsonSchemaInClasspath("schemas/lead_config_schema.json"));
        String expectedJson = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("response_validation/lead_config_res.json"),
                StandardCharsets.UTF_8
        );
        JSONAssert.assertEquals(expectedJson, response.getBody().asString(), true);
    }
    @Test(priority = 8)
    public void verify_lead_config_section_UW_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/misc/lead-config",
                Map.of("section", "underwriting"),
                "schemas/lead_config_section_UW_schema.json",
                "response_validation/lead_config_section_UW_res.json"
        );
    }
    @Test(priority = 9)
    public void verify_underwriter_lead_result_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/underwriter/lead/result/" + PropertiesReadWrite.getValue("obj_id"),
                null,
                "schemas/underwriter_lead_result_schema.json",
                "response_validation/underwriter_lead_result_res.json"
        );
    }
    @Test(priority = 10)
    public void verify_underwriter_coapp_list_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/underwriter/lead/co-applicant/list/" + PropertiesReadWrite.getValue("obj_id"),
                null,
                "schemas/underwriter_coapp_list_schema.json",
                "response_validation/underwriter_coapp_list_res.json"
        );
    }
    @Test(priority = 11)
    public void verify_technical_get_property_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/technical/get-property-list",
                Map.of("obj_id", PropertiesReadWrite.getValue("obj_id"), "role", "GLOBAL_READ_ONLY"),
                "schemas/technical_get_property_schema.json",
                "response_validation/technical_get_property_res.json"
        );
    }
    @Test(priority = 12)
    public void verify_get_technical_combination_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/technical/get-technical-combinations",
                Map.of("portfolio", "msme tl"),
                "schemas/technical_combination_schema.json",
                "response_validation/technical_combination_res.json"
        );
    }
    @Test(priority = 13)
    public void verify_deviation_data_technical_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/misc/deviation-data",
                Map.of("obj_id", PropertiesReadWrite.getValue("obj_id"),"module", "technical"),
                "schemas/deviation_data_technical_schema.json",
                "response_validation/deviation_data_technical_res.json"
        );
    }
    @Test(priority = 14)
    public void verify_deviation_condition_LE_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/misc/deviation-condition",
                Map.of("portfolio_type", "msme tl", "module", "loan_eligibility"),
                "schemas/deviation_condition_LE_schema.json",
                "response_validation/deviation_condition_LE_res.json"
        );
    }
    @Test(priority = 15)
    public void verify_legal_remarks_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/legal/remarks/" + PropertiesReadWrite.getValue("obj_id"),
                Map.of("role", "GLOBAL_READ_ONLY"   ),
                "schemas/legal_remarks_schema.json",
                "response_validation/legal_remarks_res.json"
        );
    }
    @Test(priority = 16)
    public void verify_deviation_data_legal_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/misc/deviation-data",
                Map.of("obj_id", PropertiesReadWrite.getValue("obj_id"), "module", "legal"),
                "schemas/deviation_data_legal_schema.json",
                "response_validation/deviation_data_legal_res.json"
        );
    }
    @Test(priority = 17)
    public void verify_lead_config_section_legal_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/misc/lead-config",
                Map.of("section", "legal"),
                "schemas/lead_config_section_legal_schema.json",
                "response_validation/lead_config_section_legal_res.json"
        );
    }
    @Test(priority = 18)
    public void verify_deviation_condition_legal_api() throws IOException {
        validateApiResponse(
                baseUrl + "/ilos/v1/misc/deviation-condition",
                Map.of("application_id",appId, "module", "legal","portfolio_type", "msme tl"),
                "schemas/deviation_condition_legal_schema.json",
                "response_validation/deviation_condition_legal_res.json"
        );
    }

}