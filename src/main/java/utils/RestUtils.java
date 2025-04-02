package utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
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



    public static Response performPost1(String endPoint, String payload, Map<String, Object> headers) {
        Response response = RestAssured.given().log().body()
                .headers(headers)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(endPoint);
        return response;
    }


    public static Response performPost(String endPoint, String payload, Map<String, Object> headers) {
        RequestSpecification requestSpecification = getRequestSpecification(endPoint, payload, headers);
                Response response = requestSpecification.post();
        printRequestLogInReport(requestSpecification);
        printResponseLogInReport(response);
        return response;
    }
    public static Response performPost(String endPoint, Map<String, Object> payload, Map<String, Object> headers) {
        Response response = RestAssured.given()
                .baseUri(endPoint)
                .headers(headers)
                .contentType(ContentType.JSON)
                .body(payload)
                .post();
      //  response.prettyPrint();

        return response;
    }

    public static Response performGet(String endPoint, Map<String, Object> headers) {
        Response response = RestAssured.given()
                .baseUri(endPoint)
                .headers(headers)
                .contentType(ContentType.JSON)
                .get();
        return response;
    }

    public static Response performGet(String endPoint, Map<String, Object> headers, Map<String, Object> queryParams) {
        Response response = RestAssured.given()
                .baseUri(endPoint)
                .headers(headers)
                .queryParams(queryParams)
                .contentType(ContentType.JSON)
                .get();
        return response;
    }

    public static Response sendPatchRequest(String url, Map<String, Object> headers) {
        return RestAssured.given()
                .headers(headers) // Set headers
                .contentType(ContentType.JSON) // Set Content-Type
                .when()
                .patch(url) // PATCH request
                .then()
                .extract()
                .response();
    }
    public static Response sendPatchRequest(String url,String payload, Map<String, Object> headers) {
        return RestAssured.given()
                .headers(headers) // Set headers
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .patch(url) // PATCH request
                .then()
                .extract()
                .response();
    }
    public static Response sendPatchRequest(String url, Map<String, Object> payload, Map<String, Object> headers) {
        return RestAssured.given().log().body()
                .headers(headers) // Set headers
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .patch(url) // PATCH request
                .then()
                .extract()
                .response();
    }



    private static void printRequestLogInReport(RequestSpecification requestSpecification) {
        QueryableRequestSpecification queryableRequestSpecification = SpecificationQuerier.query(requestSpecification);
        ExtentReportManager.logInfoDetails("Endpoint is " + queryableRequestSpecification.getBaseUri());
        ExtentReportManager.logInfoDetails("Method is " + queryableRequestSpecification.getMethod());
        ExtentReportManager.logInfoDetails("Headers are ");
        ExtentReportManager.logHeaders(queryableRequestSpecification.getHeaders().asList());
        ExtentReportManager.logInfoDetails("Request body is ");
        ExtentReportManager.logJson(queryableRequestSpecification.getBody());
    }

    private static void printResponseLogInReport(Response response) {
        ExtentReportManager.logInfoDetails("Response status is " + response.getStatusCode());
        ExtentReportManager.logInfoDetails("Response Headers are ");
        ExtentReportManager.logHeaders(response.getHeaders().asList());
        ExtentReportManager.logInfoDetails("Response body is ");
        ExtentReportManager.logJson(response.getBody().prettyPrint());
    }



}
