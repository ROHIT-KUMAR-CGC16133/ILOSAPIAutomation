package utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;
import java.util.Objects;

public class RestUtils {

    public static Response performPost1(String endPoint, String payload, Map<String, Object> headers) {
        Response response = RestAssured.given().log().body()
                .headers(headers)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(endPoint);
        return response;
    }


    public static Response performPost(String endPoint, String payload, Map<String, Object> headers) {
                Response response = RestAssured.given().log().body()
                .baseUri(endPoint)
                .headers(headers)
                .contentType(ContentType.JSON)
                .body(payload)
                .post();
        return response;
    }
    public static Response performPost(String endPoint, Map<String, Object> payload, Map<String, String> headers) {
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





}
