package utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import reporting.ExtentReportListener;
import reporting.ExtentReportManager;

import java.util.Map;
import java.util.Objects;

public class RestUtils {
    private static RequestSpecification getRequestSpecification(String endPoint, Object requestPayload, Map<String,Object>headers) {
        return RestAssured.given()
                .baseUri(endPoint)
                .headers(headers)
                .contentType(ContentType.JSON)
                .body(requestPayload);
    }
    private static RequestSpecification getRequestSpecification(String endPoint, Object requestPayload, Map<String,Object>queryParams, Map<String,Object>headers) {
        return RestAssured.given()
                .baseUri(endPoint)
                .headers(headers)
                .queryParams(queryParams)
                .contentType(ContentType.JSON)
                .body(requestPayload);
    }
    private static RequestSpecification getRequestSpecification(String endPoint, Map<String,Object>headers) {
        return RestAssured.given()
                .baseUri(endPoint)
                .headers(headers)
                .contentType(ContentType.JSON);
    }
    private static RequestSpecification getRequestSpecification_with_queryparam(String endPoint, Map<String,Object>queryParams, Map<String,Object>headers) {
        return RestAssured.given()
                .baseUri(endPoint)
                .headers(headers)
                .queryParams(queryParams)
                .contentType(ContentType.JSON);
    }

    public static Response performPost1(String endPoint, String payload, Map<String, Object> headers) {
        Response response = RestAssured.given().log().body()
                .headers(headers)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(endPoint);
        return response;
    }

    public static Response performPatch1(String endPoint, String payload, Map<String, String> headers) {
        Response response = RestAssured.given().log().body()
                .headers(headers)
                .contentType(ContentType.JSON)
                .body(payload)
                .patch(endPoint);
        return response;
    }


    public static Response performPost(String endPoint, String payload, Map<String, Object> headers) {
        RequestSpecification requestSpec = getRequestSpecification(endPoint, payload, headers);
        Response response = requestSpec.post();
        logRequestResponse(requestSpec, response);
        return response;
    }

    public static Response performPost(String endPoint, Map<String, Object> payload, Map<String, Object> queryParams, Map<String, Object> headers) {
        RequestSpecification requestSpec = getRequestSpecification(endPoint, payload, queryParams, headers);
        Response response = requestSpec.post();
        logRequestResponse(requestSpec, response);
        return response;
    }

    public static Response performPost(String endPoint, Map<String, Object> payload, Map<String, Object> headers) {
        RequestSpecification requestSpec = getRequestSpecification(endPoint, payload, headers);
        Response response = requestSpec.post();
        logRequestResponse(requestSpec, response);
      //  response.prettyPrint();

        return response;
    }

    public static Response performGet(String endPoint, Map<String, Object> headers) {
        RequestSpecification requestSpec = getRequestSpecification(endPoint, headers);
        Response response = requestSpec.get();
        logRequestResponse(requestSpec, response);

        return response;
    }

    public static Response performGet(String endPoint, Map<String, Object> headers, Map<String, Object> queryParams) {
        RequestSpecification requestSpec = getRequestSpecification_with_queryparam(endPoint,queryParams, headers);
        Response response = requestSpec.get();
        logRequestResponse(requestSpec, response);
        return response;
    }

    public static Response sendPatchRequest(String url, Map<String, Object> headers) {
        RequestSpecification requestSpec = getRequestSpecification(url, headers);
        Response response = requestSpec.patch();
        logRequestResponse(requestSpec, response);
        return response;
    }
    public static Response sendPatchRequest(String url,String payload, Map<String, Object> headers) {
        RequestSpecification requestSpec = getRequestSpecification(url, payload, headers);
        Response response = requestSpec.patch();
        logRequestResponse(requestSpec, response);
        return response;
    }
    public static Response sendPatchRequest(String url, Map<String, Object> payload, Map<String, Object> headers) {
        RequestSpecification requestSpec = getRequestSpecification(url, payload, headers);
        Response response = requestSpec.patch();
        logRequestResponse(requestSpec, response);
        return response;
    }
    public static Response performPut(String endPoint, Object payload, Map<String, Object> queryParams, Map<String, Object> headers) {
        RequestSpecification requestSpec = getRequestSpecification_with_queryparam(endPoint, queryParams, headers)
                .body(payload);
        Response response = requestSpec.put();
        logRequestResponse(requestSpec, response);
        return response;
    }


    private static void logRequestResponse(RequestSpecification requestSpec, Response response) {
        QueryableRequestSpecification queryableRequestSpecification = SpecificationQuerier.query(requestSpec);
      //  System.out.println("Request URL is " + queryableRequestSpecification.getBaseUri());
      //  System.out.println("query param"+queryableRequestSpecification.getQueryParams());
        if(ExtentReportListener.getTest() != null) {
            ExtentReportManager.logInfoDetails("Endpoint: " + queryableRequestSpecification.getBaseUri());
            ExtentReportManager.logInfoDetails("Method: " + queryableRequestSpecification.getMethod());
            if (queryableRequestSpecification.getBody() != null) {
                ExtentReportManager.logInfoDetails("Request body is ");
                ExtentReportManager.logJson(queryableRequestSpecification.getBody().toString());
            }
            ExtentReportManager.logInfoDetails("Response Status: " + response.getStatusCode());
            ExtentReportManager.logInfoDetails("Response Time: " + response.getTime() + " ms");
            if(response.getStatusCode() != 200) {
                ExtentReportManager.logInfoDetails("Response body is ");
                ExtentReportManager.logInfoDetails(response.getBody().prettyPrint());
            }
//        ExtentReportManager.logInfoDetails("Response body is ");
//        ExtentReportManager.logInfoDetails(response.getBody().prettyPrint());

        }
    }



}
