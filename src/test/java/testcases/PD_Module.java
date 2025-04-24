package testcases;

import generic.Generic;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static payloads.Header.getHeaders;

public class PD_Module {

    String baseUrl = PropertiesReadWrite.getValue("baseURL");
    Map<String, Object> headers;
    Response response;
    String appId = PropertiesReadWrite.getValue("application_id");
    String lead_url = baseUrl + "/ilos/v1/assignee/lead/" + PropertiesReadWrite.getValue("obj_id");
    String CPuser = PropertiesReadWrite.getValue("CPUser");
    String CPpassword = PropertiesReadWrite.getValue("CPPassword");
   // String UWUser = PropertiesReadWrite.getValue("UWUser");
   // String UWPassword = PropertiesReadWrite.getValue("UWPassword");

    @Test(priority = 1,enabled = false)
    public void getPD_HomeBranch_Lead() {
        headers = getHeaders(CPuser, CPpassword);
        String endPoint = baseUrl + "/pd/application/list/home/0/10";
        Response get_Homebranch_lead = RestUtils.performGet(endPoint, headers);
        Generic.validateResponse(get_Homebranch_lead);
        Assert.assertTrue(get_Homebranch_lead.getBody().asString().contains(appId), "application is not present in the home branch listing");

    }
    @Test(priority = 2,enabled = true)
    public void assignPD_ToSELF() {
        String url = baseUrl + "/pd/application/assign";
        headers = getHeaders(CPuser, CPpassword);
        Map<String, Object> payload = Map.of( "appId", appId);
        Response response = RestUtils.performPost(url, payload, headers);
        Generic.validateResponse(response);
        Response assign_to_me_application = RestUtils.performGet(baseUrl+"/pd/application/list/myApplication/PROCESSOR/0/10?", headers);
        Generic.validateResponse(assign_to_me_application);
        Assert.assertTrue(assign_to_me_application.getBody().asString().contains(appId), "application is not present in the assign to me listing");
    }
    @Test(priority = 3)
    public void assignPD_ToProcessor() {
        headers = getHeaders(CPuser, CPpassword);
        Map<String, Object> queryparam_lead = Map.of( "application_id", appId);
        Response lead_details = RestUtils.performGet(baseUrl+"/ilos/v1/lead/lead-detail", headers,queryparam_lead);
        Generic.validateResponse(lead_details);
        Map<String, Object> queryparam = Map.of( "user", "PROCESSOR");
        Response pdmeta_list = RestUtils.performGet(baseUrl+"/pd/pdmeta/list/"+appId,headers, queryparam);
        Generic.validateResponse(pdmeta_list);
        Map<String, Object> queryParams1 = Map.of("section", "pd_initiation");
        Response pd_initiation = RestUtils.performGet(baseUrl+"/ilos/v1/misc/lead-config", headers,queryParams1);
        Generic.validateResponse(pd_initiation);
        Response assignee_lead = RestUtils.performGet(lead_url,headers);
        Generic.validateResponse(assignee_lead);
        String branch_code = lead_details.jsonPath().getString("dt.primary.inquiry_details.branch_code");
        System.out.println("branch_code: "+branch_code);
        String portfolio_type = lead_details.jsonPath().getString("dt.primary.inquiry_details.portfolio_type");
        System.out.println("portfolio_type: "+portfolio_type);
        String empId="";
        String empName="";
        String company="";
        if(portfolio_type.equalsIgnoreCase("msme tl")){
            company = "cgcl";
            empId="BU0025";
            empName="Ahmprgt016";
            PropertiesReadWrite.setValue("UWUser","branchuser25@capriglobal.in");
        }else if(portfolio_type.equalsIgnoreCase("home equity") || portfolio_type.equalsIgnoreCase("home loan")){
            company = "cghfl";
            empId="BU0010";
            empName="Jaipragatiuser20";
            PropertiesReadWrite.setValue("UWUser","branchuser10@capriglobal.in");
        }
        System.out.println("company: "+company+" empId: "+empId+" empName: "+empName);
        Response user_available = RestUtils.performGet(baseUrl+"/ilosuser/v1/user/available?additional_branch_code="+branch_code+"&role=UNDERWRITER&login_status=0&email=&sbu="+company, headers);
        Generic.validateResponse(user_available);
        List<String> applicantIds = pdmeta_list.jsonPath().getList("result.applicantId");
        Set<String> uniqueApplicantIds = new HashSet<>(applicantIds);
        boolean isReferredBranchView = pdmeta_list.jsonPath().getBoolean("result[0].isReferred");
        for(int i=0;i<uniqueApplicantIds.size();i++){
            String applicantId = uniqueApplicantIds.toArray()[i].toString();
            System.out.println("applicantId: "+applicantId);
            List<Map<String, Object>> resultList = pdmeta_list.jsonPath().getList("result");
            List<Map<String, Object>> filtered = resultList.stream()
                    .filter(item -> applicantId.equals(item.get("applicantId")))
                    .collect(Collectors.toList());
         //   List<Map<String, Object>> pdRequestList = new ArrayList<>();
            JSONArray pdRequestList = new JSONArray();
            for (Map<String, Object> item : filtered) {
                Map<String, Object> pdEntry = new HashMap<>();
                pdEntry.put("pdId", item.get("id"));
                pdEntry.put("empId", empId);
                pdEntry.put("empName", empName);
                pdRequestList.put(pdEntry);
            }
            JSONObject requestBody = new JSONObject();
            requestBody.put("applicationId", appId);
            requestBody.put("applicantId", applicantId);
            requestBody.put("isReferredBranchView", isReferredBranchView);
            requestBody.put("pdRequestList", pdRequestList);
            Response saveRequest = RestUtils.performPost(baseUrl+"/pd/pdmeta/saveRequest", requestBody.toString(), headers);
            Generic.validateResponse(saveRequest);
        }
        String applicantId = pdmeta_list.jsonPath().getString("result[0].applicantId");
        List<Map<String, Object>> resultList = pdmeta_list.jsonPath().getList("result");
        List<Map<String, Object>> filtered_ApplicantType = resultList.stream()
                .filter(item -> "Primary Applicant".equals(item.get("applicantType")))
                .collect(Collectors.toList());
        JSONArray pdRequestList = new JSONArray();
        for (Map<String, Object> item : filtered_ApplicantType) {
            Map<String, Object> pdEntry = new HashMap<>();
            pdEntry.put("pdId", item.get("id"));
            pdEntry.put("empId", empId);
            pdEntry.put("empName", empName);
            pdRequestList.put(pdEntry);
        }
        JSONObject requestBody = new JSONObject();
        requestBody.put("applicationId", appId);
        requestBody.put("applicantId", applicantId);
        requestBody.put("isReferredBranchView", isReferredBranchView);
        requestBody.put("pdRequestList", pdRequestList);
        Response submitRequest = RestUtils.performPost(baseUrl+"/pd/pdmeta/submitRequest", requestBody.toString(), headers);
        Generic.validateResponse(submitRequest);
    }

    @Test(priority = 4)
    public void complete_PD_From_Mobile(){
        headers = getHeaders(CPuser, CPpassword);
        String pdmetalist_url = baseUrl+"/pd/pdmeta/list/"+appId;
        Map<String, Object> payload = Map.of( "appId", appId);
        Response pdmetalist_response = RestUtils.performGet(pdmetalist_url, headers);
        Generic.validateResponse(pdmetalist_response);
        List<String> pdIdList = pdmetalist_response.jsonPath().getList("result.id");
        for(int i=0;i<pdIdList.size();i++){
            String pdId = pdIdList.toArray()[i].toString();
            System.out.println("pdId: "+pdId);
            JSONObject requestBody = new JSONObject()
                    .put("pdId", pdId)
                    .put("pdResponse", new JSONObject()
                            .put("officialPhotosRemark", "auto")
                            .put("photosRemark", "auto1")
                            .put("additionalPhotosRemark", "auto2")
                            .put("photos", getPhotosArray())
                            .put("officialPhotos", getPhotosArray(2))
                            .put("additionalPhotos", getPhotosArray(2))
                    );
            Response complete_pd_response = RestUtils.performPost(baseUrl+"/pd/responses/verification", requestBody.toString(), headers);
            Generic.validateResponse(complete_pd_response);
            Map<String, Object> payload1 = new HashMap<>();
            payload1.put("pdId", pdId);
            payload1.put("status", "POSITIVE");
            payload1.put("remark", "pd status positive");
            payload1.put("recommendedAmount", "BELOW_50");
            Response submit_pd_with_status_response = RestUtils.performPost(baseUrl+"/pd/pdmeta/status", payload1, headers);
            Generic.validateResponse(submit_pd_with_status_response);

        }
    }
    @Test(priority = 5)
    public void submit_PD_from_Performer(){
        headers = getHeaders(PropertiesReadWrite.getValue("UWUser"), PropertiesReadWrite.getValue("UWPassword"));
        Response performerList_res = RestUtils.performGet(baseUrl+"/pd/application/list/myApplication/PERFORMER/0/10?", headers);
        Assert.assertTrue(performerList_res.getBody().asString().contains(appId), "appId is not present in the performer list");
        Map<String, Object> queryparam_lead = Map.of( "application_id", appId);
        Response lead_details = RestUtils.performGet(baseUrl+"/ilos/v1/lead/lead-detail", headers,queryparam_lead);
        Generic.validateResponse(lead_details);
        String pdmetalist_url = baseUrl+"/pd/pdmeta/list/"+appId;
        Map<String, Object> queryparam = Map.of( "user", "PERFORMER");
        Response pdmeta_list = RestUtils.performGet(baseUrl+"/pd/pdmeta/list/"+appId,headers,queryparam);
        Generic.validateResponse(pdmeta_list);
        String portfolio_type = lead_details.jsonPath().getString("dt.primary.inquiry_details.portfolio_type");
        String end_use_of_loan = lead_details.jsonPath().getString("dt.end_use_of_laon_as_per_mitc");
        String transaction_type = lead_details.jsonPath().getString("dt.primary.inquiry_details.transaction_type");
        int requested_loan_amount = lead_details.jsonPath().getInt("dt.primary.inquiry_details.loan_requirement");
        String expected_roi = lead_details.jsonPath().getString("dt.primary.inquiry_details.expected_roi");
        String loan_tenor_in_months = lead_details.jsonPath().getString("dt.primary.inquiry_details.loan_tenor_in_months");
        String loan_branch = lead_details.jsonPath().getString("dt.primary.inquiry_details.loan_branch");
        String primary_applicant_name = lead_details.jsonPath().getString("dt.applicant.primary.name");
        String loan_purpose = lead_details.jsonPath().getString("dt.primary.inquiry_details.loan_purpose");
        String assignedEmpName = pdmeta_list.jsonPath().getString("result.find { it.pdType == 'BUSINESS' }.assignedEmpName");
        String assignedEmpId = pdmeta_list.jsonPath().getString("result.find { it.pdType == 'BUSINESS' }.assignedEmpId");

        int resultSize = pdmeta_list.jsonPath().getList("result").size();
        String questionnaire_submit_url = baseUrl + "/pd/pdmeta/submit";
        String questionnaire_url = baseUrl + "/pd/responses/questionnaire";
        for(int i=0;i<resultSize;i++) {
            String pdType = pdmeta_list.jsonPath().getString("result[" + i + "].pdType");
            if(pdType.equalsIgnoreCase("BUSINESS")){
                String pdId = pdmeta_list.jsonPath().getString("result[" + i + "].id");
                String PDDate = LocalDate.parse(pdmeta_list.jsonPath().getString("result[" + i + "].eventSnapshotList[0].timestamp").split("T")[0])
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                String timePart = pdmeta_list.jsonPath().getString("result[" + i + "].eventSnapshotList[0].timestamp").split("T")[1].split("\\.")[0];
                String PDTime = LocalTime.parse(timePart).format(DateTimeFormatter.ofPattern("HH:mm"));
                System.out.println("pdId: BUSINESS " + pdId);
                Map<String, Object> businessPDPayload = BusinessPD_payload(pdId, portfolio_type, end_use_of_loan, transaction_type, loan_branch,
                        requested_loan_amount, expected_roi, assignedEmpName, assignedEmpId, loan_tenor_in_months, PDDate, PDTime, primary_applicant_name, loan_purpose);
                Response questionnaire_response = RestUtils.performPost(questionnaire_url, businessPDPayload, headers);
                Generic.validateResponse(questionnaire_response);
                Response questionnaire_submit_response = RestUtils.performPost(questionnaire_submit_url, questionnaire_submit_payload(pdId), headers);
                Generic.validateResponse(questionnaire_submit_response);

            }else if (pdType.equalsIgnoreCase("COLLATERAL")) {
                String pdId = pdmeta_list.jsonPath().getString("result[" + i + "].id");
                System.out.println("pdId: COLLATERAL " + pdId);
                Map<String, Object> collateralPdPayload = collateralPd_Payload(pdId, portfolio_type, end_use_of_loan, transaction_type, loan_branch,
                        requested_loan_amount, expected_roi, loan_tenor_in_months);
                Response questionnaire_response = RestUtils.performPost(questionnaire_url, collateralPdPayload, headers);
                Generic.validateResponse(questionnaire_response);
                Response questionnaire_submit_response = RestUtils.performPost(questionnaire_submit_url, questionnaire_submit_payload(pdId), headers);
                Generic.validateResponse(questionnaire_submit_response);

            }else if( pdType.equalsIgnoreCase("CURRENT_RESIDENCE")) {
                String pdId = pdmeta_list.jsonPath().getString("result[" + i + "].id");
                String PDDate = LocalDate.parse(pdmeta_list.jsonPath().getString("result[" + i + "].eventSnapshotList[0].timestamp").split("T")[0])
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                String timePart = pdmeta_list.jsonPath().getString("result[" + i + "].eventSnapshotList[0].timestamp").split("T")[1].split("\\.")[0];
                String PDTime = LocalTime.parse(timePart).format(DateTimeFormatter.ofPattern("HH:mm"));
                System.out.println("pdId: CURRENT_RESIDENCE " + pdId);
                Map<String, Object> current_residence_payload = current_residencePD_payload(pdId, portfolio_type, end_use_of_loan, transaction_type, loan_branch,
                        requested_loan_amount, expected_roi, loan_tenor_in_months, assignedEmpName, assignedEmpId, PDDate, PDTime, primary_applicant_name);
                Response questionnaire_response = RestUtils.performPost(questionnaire_url, current_residence_payload, headers);
                Generic.validateResponse(questionnaire_response);
                Response questionnaire_submit_response = RestUtils.performPost(questionnaire_submit_url, questionnaire_submit_payload(pdId), headers);
                Generic.validateResponse(questionnaire_submit_response);
                
            }
        }

    }

    @Test(priority = 6)
    public void submit_PD_from_owner(){
        headers = getHeaders(PropertiesReadWrite.getValue("UWUser"), PropertiesReadWrite.getValue("UWPassword"));
        Response performerList_res = RestUtils.performGet(baseUrl+"/pd/application/list/myApplication/OWNER/0/10?", headers);
        Assert.assertTrue(performerList_res.getBody().asString().contains(appId), "appId is not present in the owner list");
        Response lead_details = RestUtils.performGet(baseUrl+"/ilos/v1/lead/lead-detail", headers,Map.of( "application_id", appId));
        Generic.validateResponse(lead_details);
        String primary_applicant_name = lead_details.jsonPath().getString("dt.applicant.primary.name");
        Response lead_config_res = RestUtils.performGet(baseUrl+"/ilos/v1/misc/lead-config", headers,Map.of("section", "pd_owner"));
        Generic.validateResponse(lead_config_res);
        Response pdmeta_list_res = RestUtils.performGet(baseUrl+"/pd/pdmeta/list/"+appId,headers,Map.of( "user", "OWNER"));
        Generic.validateResponse(pdmeta_list_res);
        Response summary_acknowledge=RestUtils.performPost(baseUrl+"/ilos/v1/ai/summary/acknowledge",Map.of("application_id",appId),headers);
        Generic.validateResponse(summary_acknowledge);
        Map<String, Object> payload = Map.of("appId",appId,"collateral_type","","remark","","status","POSITIVE");
        Response pd_submit_owner;
        pd_submit_owner= RestUtils.performPost(baseUrl+"/pd/application/status",payload,headers);
        if(pd_submit_owner.getStatusCode()==400 && pd_submit_owner.getBody().asString().contains("Shareholding details are missing for applicant")) {
            System.out.println("Shareholding details are missing for applicant");
            Response lead_json_res = RestUtils.performGet(lead_url, headers);
            Generic.validateResponse(lead_json_res);
            String applicant_EntityType = lead_json_res.jsonPath().getString("dt.applicant.primary.entity_type");
            int applicantId = lead_json_res.jsonPath().getInt("dt.applicant.primary.id");
            String shareholding_pattern = lead_json_res.jsonPath().getString("dt.applicant.primary.shareholding_pattern");
            if(applicant_EntityType.equalsIgnoreCase("Organization") && shareholding_pattern==null){
                    System.out.println("submitting shareholding pattern for primary applicant");
                    String user_id = lead_json_res.jsonPath().getString("dt.applicant.primary.id");;
                    String applicant_type = "Applicant";
                    String constitution_type = lead_json_res.jsonPath().getString("dt.applicant.primary.organization_info.constitution");
                    Map<String, Object> payload_shareholding = getPayload_shareholding(user_id, applicant_type, constitution_type, primary_applicant_name, applicantId);
                    Response shareholding_response = RestUtils.performPost(baseUrl+"/ilos/v1/shareholding-pattern/"+PropertiesReadWrite.getValue("obj_id"), payload_shareholding, headers);
                    Generic.validateResponse(shareholding_response);
            }
            int coapp_count = lead_json_res.jsonPath().getList("dt.applicant.co_applicant").size();
            for(int i=0;i<coapp_count;i++) {
                if ((lead_json_res.jsonPath().getString("dt.applicant.co_applicant[" + i + "].shareholding_pattern") == null) &&
                        (lead_json_res.jsonPath().getString("dt.applicant.co_applicant[" + i + "].entity_type").equalsIgnoreCase("Organization"))) {
                    System.out.println("submitting shareholding pattern for coapplicant "+i);
                    String coapplicant_id = lead_json_res.jsonPath().getString("dt.applicant.co_applicant[" + i + "].id");
                    String applicant_type = "Co-Applicant";
                    String constitution_type = lead_json_res.jsonPath().getString("dt.applicant.co_applicant[" + i + "].organization_info.constitution");
                    Map<String, Object> payload_shareholding = getPayload_shareholding(coapplicant_id, applicant_type, constitution_type, primary_applicant_name, applicantId);
                    Response shareholding_response = RestUtils.performPost(baseUrl + "/ilos/v1/shareholding-pattern/" + PropertiesReadWrite.getValue("obj_id"), payload_shareholding, headers);
                    Generic.validateResponse(shareholding_response);

                }
            }
            int guarantor_count = lead_json_res.jsonPath().getList("dt.applicant.guarantors").size();
            for(int i=0;i<guarantor_count;i++) {
                if ((lead_json_res.jsonPath().getString("dt.applicant.guarantors[" + i + "].shareholding_pattern") == null) &&
                        (lead_json_res.jsonPath().getString("dt.applicant.guarantors[" + i + "].entity_type").equalsIgnoreCase("Organization"))) {
                    System.out.println("submitting shareholding pattern for guarantor "+i);
                    String guarantor_id = lead_json_res.jsonPath().getString("dt.applicant.guarantors[" + i + "].id");
                    String applicant_type = "Guarantor";
                    String constitution_type = lead_json_res.jsonPath().getString("dt.applicant.guarantors[" + i + "].organization_info.constitution");
                    Map<String, Object> payload_shareholding = getPayload_shareholding(guarantor_id, applicant_type, constitution_type, primary_applicant_name, applicantId);
                    Response shareholding_response = RestUtils.performPost(baseUrl + "/ilos/v1/shareholding-pattern/" + PropertiesReadWrite.getValue("obj_id"), payload_shareholding, headers);
                    Generic.validateResponse(shareholding_response);

                }
            }
            pd_submit_owner = RestUtils.performPost(baseUrl+"/pd/application/status",payload,headers);
        }
        Generic.validateResponse(pd_submit_owner);
        Response mark_section_complete = RestUtils.sendPatchRequest(baseUrl+"/ilos/v1/assignee/lead/mark-section-complete/"+PropertiesReadWrite.getValue("obj_id"),Map.of("section","pd"),headers);
        Generic.validateResponse(mark_section_complete);
    }

    private static Map<String, Object> getPayload_shareholding(String user_id,String applicant_type, String constitution_type,String primary_applicant_name,int applicant_id) {
        Map<String, Object> payload_shareholding = new HashMap<>();

        payload_shareholding.put("applicant_id", user_id);
        payload_shareholding.put("applicant_type", applicant_type);
        payload_shareholding.put("constitution_type", constitution_type);

        List<Map<String, Object>> shareholdingPattern = new ArrayList<>();
        Map<String, Object> shareholding = new HashMap<>();
        String designation_type = getDesignation_type(constitution_type);
        shareholding.put("id", 1);
        shareholding.put("bod", true);
        shareholding.put("borrower_type", "Applicant");
        shareholding.put("name", primary_applicant_name);
        shareholding.put("designation_type", designation_type);
        shareholding.put("ovd_type", "Voter Id");
        shareholding.put("percentage", "100");
        shareholding.put("user_id", applicant_id);

        shareholdingPattern.add(shareholding);

        payload_shareholding.put("shareholding_pattern", shareholdingPattern);
        return payload_shareholding;
    }
private static String getDesignation_type(String designation) {
    return switch (designation.toLowerCase()) {
        case "huf" -> "Director";
        case "partnership" -> "Partner";
        case "private limited company" -> "Director";
        case "public limited company" -> "Director";
        case "sole proprietorship" -> "Proprietor";
        case "llp" -> "Partner";
        case "trust" -> "Chairman";
        case "societies" -> "Chairman";
        case "association" -> "Chairman";
        default -> throw new IllegalStateException("Unexpected value: " + designation);
    };
    }

    private JSONArray getPhotosArray() {
        return getPhotosArray(4);
    }
    private JSONArray getPhotosArray(int count) {
        JSONArray photos = new JSONArray();
        for (int i = 0; i < count; i++) {
            JSONObject photo = new JSONObject()
                    .put("address", "CWC8+RC Mountain View, CA, USA")
                    .put("latitude", "37.4220936")
                    .put("longitude", "-122.083922")
                    .put("remark", "Photo outside of the business")
                    .put("uuid", "mlap-dev-frontend/upload_docs/pd_images/1732189364463_93becce7-c763-4f37-b3d9-cf0f0a07ecdb.jpg")
                    .put("uploadedAt", "2024-11-21T11:42:12.922Z");
            photos.put(photo);
        }
        return photos;
    }

    private Map<String,Object> BusinessPD_payload(String pdId,String portfolio_type, String end_use_of_loan, String transaction_type,String loan_branch,
                                                  int requested_loan_amount,String expected_roi,String assignedEmpName,String assignedEmpId,
                                                   String loan_tenor_in_months,String PDDate,String PDTime,String primary_applicant_name,
                                                   String loan_purpose) {
        Map<String, Object> body = new HashMap<>();
        body.put("pdId", pdId);
        body.put("isFinalSubmit", true);

        Map<String, Object> questionnaire = new HashMap<>();
        questionnaire.put("Application Details", Map.of(
                "portfolio", portfolio_type,
                "end_use_of_loan", end_use_of_loan,
                "transaction_type", transaction_type,
                "application_id", appId,
                "branch_name", loan_branch,
                "requested_loan_amount", requested_loan_amount,
                "requested_roi", expected_roi,
                "request_tenure", loan_tenor_in_months
        ));

        questionnaire.put("PD Details", Map.of(
                "pd_done_by_name",assignedEmpName ,
                "pd_done_by_id", assignedEmpId,
                "is_other_capri_employee", "No",
                "name", "",
                "pd_date", PDDate,
                "pd_time", PDTime,
                "pd_location", "gurgaon",
                "person_met_during_pd", List.of(primary_applicant_name),
                "other_employee", List.of(Map.of("name", ""))
        ));
        Map<String, Object> businessDetails = new HashMap<>();
        businessDetails.put("Level_of_Business_activity_observed", "High");
        businessDetails.put("Monthly_Annually_Turnover_Monthly_Salary_gross", "5");
        businessDetails.put("business_address_same_as", "Aadhar");
        businessDetails.put("business_activity_observed_remarks", "");
        businessDetails.put("industry_type", "transport operaters");
        businessDetails.put("margin_in_business", 20);
        businessDetails.put("nature_of_business", "service");
        businessDetails.put("net_monthly_annually_profit", "1");
        businessDetails.put("number_of_employees", "11");
        businessDetails.put("sub_industry_type", "commission agent");
        businessDetails.put("total_monthly_salary_on_employees", "112");
        businessDetails.put("vintage_of_Existing_business", "1");
        businessDetails.put("type_of_business_premises", "residence cum office");

        questionnaire.put("Business PD - Bussiness Details", businessDetails);
        questionnaire.put("Business Profile Comment", Map.of(
                "comment_on_business_profile", "ok"
        ));
        questionnaire.put("Business PD - Documents Details", Map.of(
                "documents_observed_during_pd", List.of("PAN")
        ));
        questionnaire.put("Business PD - Live Loan Details / Obligation", new HashMap<>());
        questionnaire.put("Trade References", Map.of(
                "other_trade_reference", List.of(
                        Map.of(
                                "tradeReferenceName", "test",
                                "firmName", "capri",
                                "contactNo", "9876543211",
                                "natureOfTransaction", "buyer",
                                "approxBussPerMonth", "120",
                                "noOfYearsDealt", "1",
                                "tradeReferenceFeedback", "Positive"
                        )
                )
        ));
        questionnaire.put("Loan OCR & End Use", Map.of(
                "end_use_of_loan_ocr",loan_purpose ,
                "cost_of_purchase", "10,000",
                "lcr","2,000",
                "lcr_plot", "3000",
                "source_of_OCR", "test1",
                "total_ocr", "5,000",
                "property_type", "commercial",
                "occupancy", "vacant",
                "total_deal_amount", "444",
                "lcr_remarks", "ok"
        ));
        questionnaire.put("CIBIL Inquiry Details", Map.of(
                "other_cibil", List.of(
                        Map.of(
                                "date_applied", "2025-04-11",
                                "financer_name", "capri",
                                "type_of_loan", "Home",
                                "loan_amount", "10,00,000",
                                "cibilRemark", "ok"
                        )
                )
        ));
        questionnaire.put("Strengths and Other Remarks", Map.of(
                "strengths_comment", "test",
                "strengths_remark", "ok",
                "total_assets", "10,000",
                "total_liabilities", "1,000",
                "net_worth", "9,000"
        ));
        questionnaire.put("Personal Reference Check", Map.of(
                "other_reference", List.of(
                        Map.of(
                                "reference_name", "Test",
                                "relationship_with_applicant", "Spouse",
                                "mobile_no", "8585151565",
                                "personalAddress", "",
                                "feedback", "Positive"
                        ),
                        Map.of(
                                "reference_name", "test",
                                "relationship_with_applicant", "Neighbour",
                                "mobile_no", "8585151595",
                                "personalAddress", "",
                                "feedback", "Positive"
                        )
                )
        ));
        List<Map<String, Object>> riskCategories = new ArrayList<>();
        riskCategories.add(Map.of("id", 1, "question", "NRI", "newCase", true, "pa_928264", "YES", "pa_928264_disabled", false));
        riskCategories.add(Map.of("id", 2, "question", "High net-worth individuals above Rs. 50 Cr", "newCase", true, "pa_928264", "YES", "pa_928264_disabled", false));
        riskCategories.add(Map.of("id", 4, "question", "politically exposed persons (PEPs)...", "newCase", true, "pa_928264", "YES", "pa_928264_disabled", false));
        riskCategories.add(Map.of("id", 7, "question", "Non face to face to customers for Loan amount", "newCase", true, "pa_928264", "YES", "pa_928264_disabled", false));
        riskCategories.add(Map.of("id", 10, "question", "Negative Profile", "newCase", true, "pa_928264", "YES", "pa_928264_disabled", false));
        riskCategories.add(Map.of("id", 11, "question", "List Of Negative Profile", "newCase", true, "pa_928264", "Builders and Developers (real estate)", "pa_928264_disabled", false));
        riskCategories.add(Map.of("id", 12, "question", "KYC RISK CATEGORY", "newCase", true, "pa_928264", "High", "pa_928264_disabled", true));

        questionnaire.put("Risk Category", Map.of("risk_category", riskCategories));

        questionnaire.put("Interviewer's Comment", Map.of(
                "interview_comment", "good",
                "status", "positive"
        ));

        body.put("questionnaire", questionnaire);
        return body;
    }

    private Map<String, Object> collateralPd_Payload(String pdId, String portfolio_type,
                                                     String end_use_of_loan, String transaction_type,String loan_branch,
                                                     int requested_loan_amount,String expected_roi,
                                                     String loan_tenor_in_months) {
        Map<String, Object> payload1 = new HashMap<>();
        payload1.put("pdId", pdId);
        payload1.put("isFinalSubmit", true);

        Map<String, Object> questionnaire1 = new HashMap<>();

        Map<String, Object> applicationDetails = new HashMap<>();
        applicationDetails.put("portfolio", portfolio_type);
        applicationDetails.put("end_use_of_loan", end_use_of_loan);
        applicationDetails.put("transaction_type", transaction_type);
        applicationDetails.put("application_id", appId);
        applicationDetails.put("branch_name",loan_branch );
        applicationDetails.put("requested_loan_amount", requested_loan_amount);
        applicationDetails.put("requested_roi", expected_roi);
        applicationDetails.put("request_tenure", loan_tenor_in_months);
        questionnaire1.put("Application Details", applicationDetails);

        Map<String, Object> propertyDetails1 = new HashMap<>();
        propertyDetails1.put("distance_of_nearest_branch", "11");
        propertyDetails1.put("distance_of_work_location", "11");
        questionnaire1.put("Property Details 1", propertyDetails1);

        Map<String, Object> propertyDetails2 = new HashMap<>();
        propertyDetails2.put("vintage_of_property", "11");
        propertyDetails2.put("relationship_with_applicant", "father");
        propertyDetails2.put("mobile_no", "9876543210");
        questionnaire1.put("Property Details 2", propertyDetails2);

        Map<String, Object> propertyDetails3 = new HashMap<>();
        propertyDetails3.put("is_there_any_board_on_property_saying_mortgaged", "No");
        propertyDetails3.put("name_of_bank", "");
        propertyDetails3.put("is_property_located_negative_area", "No");
        propertyDetails3.put("is_property_located_dominated_area", "No");
        propertyDetails3.put("is_past_collection_issue", "No");
        propertyDetails3.put("is_any_bank_fund", "No");
        propertyDetails3.put("meter_type", "Separate");
        propertyDetails3.put("neighboour_check", "test");
        questionnaire1.put("Property Details 3", propertyDetails3);

        Map<String, Object> interviewerComment = new HashMap<>();
        interviewerComment.put("interview_comment", "ok");
        interviewerComment.put("status", "positive");
        questionnaire1.put("Interviewer's Comment", interviewerComment);

        payload1.put("questionnaire", questionnaire1);
        return payload1;
    }

    private Map<String, Object> current_residencePD_payload(String pdId, String portfolio_type,
                                                              String end_use_of_loan, String transaction_type,String loan_branch,
                                                              int requested_loan_amount,String expected_roi,
                                                              String loan_tenor_in_months,String assignedEmpName,String assignedEmpId,
                                                              String PDDate,String PDTime,String primary_applicant_name) {
        Map<String, Object> question_payload = new HashMap<>();
        question_payload.put("pdId", pdId);

        Map<String, Object> questionnaire3 = new HashMap<>();

// Application Details
        Map<String, Object> applicationDetails = new HashMap<>();
        applicationDetails.put("portfolio", portfolio_type);
        applicationDetails.put("end_use_of_loan", end_use_of_loan);
        applicationDetails.put("transaction_type", transaction_type);
        applicationDetails.put("application_id", appId);
        applicationDetails.put("branch_name", loan_branch);
        applicationDetails.put("requested_loan_amount", requested_loan_amount);
        applicationDetails.put("requested_roi", expected_roi);
        applicationDetails.put("request_tenure", loan_tenor_in_months);
        questionnaire3.put("Application Details", applicationDetails);

// PD Details
        Map<String, Object> pdDetails = new HashMap<>();
        pdDetails.put("pd_done_by_name",assignedEmpName );
        pdDetails.put("pd_done_by_id", assignedEmpId);
        pdDetails.put("is_other_capri_employee", "No");
        pdDetails.put("name", "");
        pdDetails.put("pd_date", PDDate);
        pdDetails.put("pd_time", PDTime);
        pdDetails.put("pd_location", "gurgaon");
        pdDetails.put("person_met_during_pd", List.of(primary_applicant_name));
        pdDetails.put("other_employee", List.of(Map.of("name", "")));
        questionnaire3.put("PD Details", pdDetails);

// Applicant Details
        Map<String, Object> applicantDetails = new HashMap<>();
        applicantDetails.put("applicant_name",primary_applicant_name );
        applicantDetails.put("high_qualification", "bcom");
        applicantDetails.put("dependent", "2");
        applicantDetails.put("current_address", "110096");
        applicantDetails.put("current_address_type", "rented");
        applicantDetails.put("parmanent_addres", "110074");
        applicantDetails.put("permanent_address_type", "rented");
        applicantDetails.put("current_address_vintage", "delhi");
        applicantDetails.put("is_current_address_same_as_applicant_address", "No");
        applicantDetails.put("residence_address_as_per_pd", "1");
        questionnaire3.put("Applicant Details", applicantDetails);

// Residence TPC Details
        Map<String, Object> residenceTPC = new HashMap<>();
        residenceTPC.put("applicant_name", primary_applicant_name);
        residenceTPC.put("current_address_landmark", "ggn");
        residenceTPC.put("occupancy_status", "self_owned");
        residenceTPC.put("locality", "Slum");
        residenceTPC.put("person_met", "test");
        residenceTPC.put("years_residence", "1");
        residenceTPC.put("any_earning_member", "yes");
        residenceTPC.put("no_family_members", "8");
        residenceTPC.put("several_medical_condition_in_family", "Yes");
        residenceTPC.put("neighbour_name", "test");
        residenceTPC.put("neighbour_recognize", "yes");
        residenceTPC.put("neighbour_contact_no", "");
        residenceTPC.put("neighbour_reference", "positive");
        residenceTPC.put("neighbour_comments", "ok");
        questionnaire3.put("residence_tpc_details", residenceTPC);

// Interviewers Comment
        Map<String, Object> interviewComment = new HashMap<>();
        interviewComment.put("interview_comment", "ok");
        interviewComment.put("status", "positive");
        questionnaire3.put("Interviewers Comment", interviewComment);

// Final assembly
        question_payload.put("questionnaire", questionnaire3);
        question_payload.put("isFinalSubmit", true);
        return question_payload;
    }

    private Map<String,Object> questionnaire_submit_payload(String pdId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("pdId", pdId);
        payload.put("status", "POSITIVE");
        payload.put("remark", "questionnaire status positive");
        return payload;
    }
}
