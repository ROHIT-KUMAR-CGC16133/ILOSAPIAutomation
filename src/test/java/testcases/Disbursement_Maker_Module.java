package testcases;

import generic.Generic;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.PropertiesReadWrite;
import utils.RestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.*;
import static payloads.Header.getHeaders;

public class Disbursement_Maker_Module {
    String baseUrl = PropertiesReadWrite.getValue("baseURL");
    Map<String, Object> headers;
    Response response;
    String appId = PropertiesReadWrite.getValue("application_id");
    String lead_url = baseUrl + "/ilos/v1/assignee/lead/" + PropertiesReadWrite.getValue("obj_id");
    String maker_id = PropertiesReadWrite.getValue("maker_id");
    String makerPassword = PropertiesReadWrite.getValue("makerPassword");

    @Test(priority=1)
    public void completeUnassignedCase() {
        headers = getHeaders(maker_id, makerPassword);
        Response dashboard = RestUtils.performGet(baseUrl+"ilos/v1/disbursement/dashboard", headers, of("role", "MAKER"));
        Generic.validateResponse(response);
        Response openLead_res = RestUtils.performGet(baseUrl+"/ilos/v1/disbursement/open-leads", headers, of("role", "MAKER"));
        Generic.validateResponse(openLead_res);
        Response selfAssign_res = RestUtils.performPost(baseUrl+"/ilos/v1/disbursement/self-assign", of("application_ids", List.of("677e883af2934bed9fb25096")), of("role","MAKER"), headers);
        Generic.validateResponse(selfAssign_res);
    }
    @Test(priority=2)
    public void completeAssignedLead(){
        headers = getHeaders(maker_id, makerPassword);
        Response assignedLeadList = RestUtils.performGet(baseUrl+"/ilos/v1/disbursement/assigned-leads", headers, of("role", "MAKER"));
        Generic.validateResponse(assignedLeadList);
        Assert.assertTrue(assignedLeadList.getBody().asString().contains(appId), "application is not present in Assigned lead    listing");
        //url= https://ilosapi-uat.capriglobal.in/ilos/v1/disbursement/favoring-approvals?obj_id=66d8973b6800a6570317855a&role=MAKER
        //url =https://ilosapi-uat.capriglobal.in/ilos/v1/disbursement/favoring-details/key?favoring_type=SELLER&obj_id=66d8973b6800a6570317855a&role=MAKER
        //url = https://ilosapi-uat.capriglobal.in/ilos/v1/disbursement/favoring-details/key?favoring_type=CUSTOMER&obj_id=66d8973b6800a6570317855a&role=MAKER

        Response repayment_bank_account = RestUtils.performGet(baseUrl+"/ilos/v1/repayment/bank-accounts"+PropertiesReadWrite.getValue("obj_id"), headers);
        Generic.validateResponse(repayment_bank_account);
        Response favoring_details = RestUtils.performGet(baseUrl+"/ilos/v1/disbursement/favoring-details", headers, of("obj_id", PropertiesReadWrite.getValue("obj_id"), "role", "MAKER"));
        Generic.validateResponse(favoring_details);
        Response response = RestUtils.performGet(lead_url, headers);
        Generic.validateResponse(response);
        Response favoring_type_insurance = RestUtils.performGet(baseUrl+"/ilos/v1/disbursement/favoring-details/key", headers, of("favoring_type", "INSURANCE_VENDOR", "obj_id", PropertiesReadWrite.getValue("obj_id"), "role", "MAKER"));
        Generic.validateResponse(favoring_type_insurance);
        String stage_of_construction = favoring_details.jsonPath().getString("data.amount_details.stage_of_construction");
        int amount_to_be_disbursed = favoring_details.jsonPath().getInt("data.amount_details.amount_to_be_disbursed");
        int favorting_details_count = favoring_details.jsonPath().getInt("data.favoring_details");
        String portfolio_type = response.jsonPath().getString("dt.primary.inquiry_details.portfolio_type");
        int applicant_id = response.jsonPath().getInt("dt.applicant.primary.id");
        String applicant_name = response.jsonPath().getString("dt.applicant.primary.name");
        String type="";
        String bank_partner_code = "";
        String bank_name = "";
        String account_number = "";
        if(portfolio_type.equalsIgnoreCase("msme tl")){
            type="CGCL";
             bank_partner_code =  favoring_type_insurance.jsonPath().getString("capri_disbursal_accounts.find {  it.type == '" + type + "' }.bank_partner_code");
             bank_name =  favoring_type_insurance.jsonPath().getString("capri_disbursal_accounts.find {  it.type == '" + type + "' }.bank_name");
             account_number =  favoring_type_insurance.jsonPath().getString("capri_disbursal_accounts.find {  it.type == '" + type + "' }.account_number");
        }else if (portfolio_type.equalsIgnoreCase("home loan")|| portfolio_type.equalsIgnoreCase("home loan top up") || portfolio_type.equalsIgnoreCase("home equity")) {
            type="CGHFL";
             bank_partner_code =  favoring_type_insurance.jsonPath().getString("capri_disbursal_accounts.find {  it.type == '" + type + "' }.bank_partner_code");
             bank_name =  favoring_type_insurance.jsonPath().getString("capri_disbursal_accounts.find {  it.type == '" + type + "' }.bank_name");
             account_number =  favoring_type_insurance.jsonPath().getString("capri_disbursal_accounts.find {  it.type == '" + type + "' }.account_number");
        }

        Map<String, Object> capriDisbursalAccount = new HashMap<>();
        capriDisbursalAccount.put("bank_partner_code", bank_partner_code);
        capriDisbursalAccount.put("bank_name", bank_name);
        capriDisbursalAccount.put("account_number", account_number);
        capriDisbursalAccount.put("type", type);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", favorting_details_count+1);
        payload.put("favoring_type", "CUSTOMER");
        payload.put("favoring_details", applicant_name);
        payload.put("mode_of_disbursement", "NEFT");
        payload.put("instrument_number", "");
        payload.put("ifsc_code", "SBIN0000300");
        payload.put("bank_name", "STATE BANK OF INDIA");
        payload.put("account_number", "17687698798");
        payload.put("account_holder_name", "Dummy Customer Name");
        payload.put("account_status", true);
        payload.put("passbook_or_cheque", null);
        payload.put("amount", String.valueOf(amount_to_be_disbursed / 2));
        payload.put("maker_date", null);
        payload.put("checker_id", null);
        payload.put("checker_date", null);
        payload.put("remarks", "ok");
        payload.put("favoringDetailsOptions", List.of( response.jsonPath().getString("dt.primary.inquiry_details.name"), "INDRABAI RAJESH SATNORIYA"));
        payload.put("isIfscVerified", true);
        payload.put("branch_name", "MUMBAI MAIN");
        payload.put("capri_disbursal_account", capriDisbursalAccount);
        payload.put("application_id", appId);
        payload.put("maker_action", "RECOMMENDED");

        Response favoring_submit = RestUtils.performPost(baseUrl+"/ilos/v1/disbursement/favoring-details", payload, headers, of("role", "MAKER"));
        Generic.validateResponse(favoring_submit);
        Map<String, Object> bankAccount = new HashMap<>();
        bankAccount.put("bank_name", repayment_bank_account.jsonPath().getString("data[0].bank_name"));
        bankAccount.put("ifsc_code", repayment_bank_account.jsonPath().getString("data[0].ifsc_code"));
        bankAccount.put("branch_name", repayment_bank_account.jsonPath().getString("data[0].branch_name"));
        bankAccount.put("account_number", repayment_bank_account.jsonPath().getString("data[0].account_number"));
        bankAccount.put("account_holder_name", repayment_bank_account.jsonPath().getString("data[0].name_of_account_holder"));
        bankAccount.put("account_type", repayment_bank_account.jsonPath().getString("data[0].account_type"));
        bankAccount.put("account_since", "");
        bankAccount.put("default_repayment", true);
        bankAccount.put("uid", repayment_bank_account.jsonPath().getString("data[0].uid"));

        Map<String, Object> payload_repayment = new HashMap<>();
        payload_repayment.put("applicant_id", applicant_id);
        payload_repayment.put("applicant_name",applicant_name );
        payload_repayment.put("bank_account", bankAccount);
        Response repayment_submit = RestUtils.sendPatchRequest(baseUrl+"/ilos/v1/disbursement/repayment/bank-accounts"+ PropertiesReadWrite.getValue("obj_id"), payload_repayment, headers);
        Generic.validateResponse(repayment_submit);

        Response generate_doc = RestUtils.performPost(baseUrl+"/ilos/v1/disbursement/generate-docs",new HashMap<String,Object>(),headers, Map.of("obj_id", PropertiesReadWrite.getValue("obj_id")));
        Generic.validateResponse(generate_doc);

        Response get_doc = RestUtils.performGet(baseUrl+"ilos/v1/disbursement/get-generated-docs", headers, of("obj_id", PropertiesReadWrite.getValue("obj_id")));
        Generic.validateResponse(get_doc);

        String doc_link= Generic.generate_upload_url_and_upload_file("bankstatment.pdf", appId, "application/pdf", "pdf", "OTHERS=ADDITIONAL-DOCUMENT", headers);

        Map<String, Object> payload_doc_handler = new HashMap<>();
        payload_doc_handler.put("applicant_id", applicant_id);
        payload_doc_handler.put("applicant_type", "primary");
        payload_doc_handler.put("doc_sub_type", "ADDITIONAL-DOCUMENT");
        payload_doc_handler.put("doc_type", "OTHERS");
        payload_doc_handler.put("doc_type_identifier", "disbursement_esign");
        payload_doc_handler.put("docindex", "undefined");
        payload_doc_handler.put("section", "bankstatment.pdf");
        payload_doc_handler.put("sub_section", "esign");
        payload_doc_handler.put("unique_id", "null");
        payload_doc_handler.put("url", doc_link);
        Response doc_handler = RestUtils.performPost(baseUrl+"/ilos/v2/document-handler/"+PropertiesReadWrite.getValue("obj_id"), payload_doc_handler, headers);
        Generic.validateResponse(doc_handler);

        Response manual_esign = RestUtils.performPost(baseUrl+"/ilos/v1/disbursement/manual-esign", of("application_id", appId,"doc_label",null,"doc_link",doc_link),headers);
        Generic.validateResponse(manual_esign);

        Response get_sanction_condition = RestUtils.performGet(baseUrl+"/ilos/v1/disbursement/sanction-condition", headers, of("obj_id", PropertiesReadWrite.getValue("obj_id")));
        Generic.validateResponse(get_sanction_condition);
        int data_count= get_sanction_condition.jsonPath().getList("data").size();
        Response disbursement_master = RestUtils.performGet(baseUrl+"/ilos/v1/disbursement/master", headers);
        Generic.validateResponse(disbursement_master);
        for(int i=0;i<data_count;i++){
            if(get_sanction_condition.jsonPath().getString("data["+i+"].received_status").equalsIgnoreCase("NO")){
                String sanction_condition_id = get_sanction_condition.jsonPath().getString("data["+i+"]._id");
                Map<String, Object> payload_sanction_condition = new HashMap<>();
                payload_sanction_condition.put("id", get_sanction_condition.jsonPath().getString("data["+i+"].id"));
                payload_sanction_condition.put("received_status", "NO");
                payload_sanction_condition.put("collection_stage", get_sanction_condition.jsonPath().getString("data["+i+"].collection_stage"));
                payload_sanction_condition.put("receipt_date", get_sanction_condition.jsonPath().getString("data["+i+"].receipt_date"));
                payload_sanction_condition.put("doc_upload", get_sanction_condition.jsonPath().getString("data["+i+"].doc_upload"));
                payload_sanction_condition.put("application_id", PropertiesReadWrite.getValue("application_id"));
                payload_sanction_condition.put("doc_type", get_sanction_condition.jsonPath().getString("data["+i+"].doc_type"));
                payload_sanction_condition.put("doc_nature", disbursement_master.jsonPath().getString("data.document_type_master["+i+"]"));
                payload_sanction_condition.put("sub_document_tag", get_sanction_condition.jsonPath().getString("data["+i+"].sub_document_tag"));
                payload_sanction_condition.put("lod", get_sanction_condition.jsonPath().getString("data["+i+"].lod"));
                payload_sanction_condition.put("target_date", "2027-05-20");
                Response sanction_condition = RestUtils.sendPatchRequest(baseUrl+"/ilos/v1/disbursement/sanction-condition", payload_sanction_condition, headers);
                Generic.validateResponse(sanction_condition);
            }
        }

        String ndc_checklist_payload = "{\"obj_id\":\""+PropertiesReadWrite.getValue("obj_id")+"\",\"checklist_data\":{\"application_form-photograph\":\"Yes\",\"application_form-application_form\":\"Yes\",\"application_form-insurance_form\":\"Yes\"," +
                "\"application_form-product_details\":\"Yes\",\"application_form-connector_dsa\":\"Yes\",\"application_form-non_bt_case\":\"Yes\",\"kyc-kyc_docs\":\"Yes\",\"osv-osv_check\":\"Yes\",\"verification_reports-external_reports\":\"" +
                "Yes\",\"verification_reports-roc_search_reports\":\"Yes\",\"verification_reports-property_address_match\":\"Yes\",\"verification_reports-technical_report\":\"Yes\",\"verification_reports-legal_report\":\"Yes\",\"verification_reports-title_search_report\":\"" +
                "Yes\",\"system_checks-credit_sanction\":\"Yes\",\"system_checks-commercial_approval\":\"Yes\",\"sanction_letter-sanction_letter\":\"Yes\",\"disbursal_request_form-disbursal_request_form\":\"Yes\",\"loan_agreement_kit-cheque_favoring\":\"Yes\",\"loan_agreement_kit-signed_printed_schedules\":\"Yes\"," +
                "\"loan_agreement_kit-agreement_stamping\":\"Yes\",\"loan_agreement_kit-execution_date_in_range\":\"Yes\",\"loan_agreement_kit-sanction_letter_details_match\":\"Yes\",\"loan_agreement_kit-signed_continuity_letter\":\"Yes\",\"loan_agreement_kit-moe_stamped\":\"Yes\"," +
                "\"loan_agreement_kit-signate_match\":\"Yes\",\"loan_agreement_kit-valid_dated_executed_document\":\"Yes\",\"loan_agreement_kit-correct_due_date\":\"Yes\",\"loan_agreement_kit-pdc_nach_check\":\"Yes\",\"loan_agreement_kit-emi_amount_match\":\"Yes\",\"loan_agreement_kit-payers_name\":\"Yes\"," +
                "\"loan_agreement_kit-pdc_approval\":\"Yes\",\"loan_agreement_kit-mandate_signature\":\"Yes\",\"loan_agreement_kit-pdc_rubber_stamped\":\"Yes\",\"loan_agreement_kit-valid_nach\":\"Yes\",\"loan_agreement_kit-lod_modt_collection\":\"Yes\",\"loan_agreement_kit-bt_collection\":\"Yes\",\"repayment_document-pdc_nach\":\"Yes\"," +
                "\"otc_pdd-document_updated_legal_report\":\"Yes\",\"documents-otc_approval\":\"Yes\",\"documents-cersai_search_report\":\"Yes\",\"documents-file_docket_screened_stamped\":\"Yes\",\"documents-sampled_report_check\":\"Yes\",\"rcu_check-hunter_check\":\"Yes\",\"fee_charges-charge_verification\":\"Yes\"," +
                "\"sanction_condition_compliance-compiled_sanction_condition\":\"Yes\",\"repayment_schedule-repayment_schedule\":\"Yes\",\"other_documents-company_director\":\"Yes\",\"other_documents-board_resolution\":\"Yes\",\"other_documents-partnership_authority_letter\":\"Yes\",\"other_documents-section_180_compliance\":\"Yes\"," +
                "\"other_documents-vernacular_undertaking\":\"Yes\",\"other_documents-end_user_letter\":\"Yes\",\"other_documents-loan_fore_closure_letter\":\"Yes\",\"other_documents-own_contribution_receipt\":\"Yes\"}}";


        Response ndc_checklist = RestUtils.performPut(baseUrl+"ilos/v1/disbursement/checklist", ndc_checklist_payload,Map.of("role","MAKER"), headers);
        Generic.validateResponse(ndc_checklist);

    }




}
