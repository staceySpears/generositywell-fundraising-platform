package com.kenzie.capstone.service.client;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.GetRestApisRequest;
import software.amazon.awssdk.services.apigateway.model.GetRestApisResponse;
import software.amazon.awssdk.services.apigateway.model.RestApi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class EndpointUtility {
    private String apiEndpoint;

    public EndpointUtility() {
        this.apiEndpoint = getApiEndpint();
    }

    public static String getStackName() {
        String deploymentName = System.getenv("CAPSTONE_SERVICE_STACK_DEV");
        if (deploymentName == null) {
            deploymentName = System.getenv("SERVICE_STACK_NAME");
        }
        if (deploymentName == null) {
            deploymentName = System.getenv("STACK_NAME");
        }
        if (deploymentName == null) {
            throw new IllegalArgumentException("Could not find the deployment name in environment variables.  Make sure that you have set up your environment variables using the setupEnvironment.sh script.");
        }
        return deploymentName;
    }

    public static String getApiEndpint() {
        String region = System.getenv("AWS_REGION");
        if (region == null) {
            region = "us-east-1";
        }

        String deploymentName = getStackName();

        ApiGatewayClient apiGatewayClient = ApiGatewayClient.builder()
                .region(Region.of(region))
                .build();
        GetRestApisRequest request = GetRestApisRequest.builder()
                .limit(500)
                .build();
        GetRestApisResponse result = apiGatewayClient.getRestApis(request);

        String endpointId = null;
        for (RestApi restApi : result.items()) {
            if (restApi.name().equals(deploymentName)) {
                endpointId = restApi.id();
                break;
            }
        }
        if (endpointId == null) {
            throw new IllegalArgumentException("Could not locate the API Gateway endpoint.  Make sure that your API is deployed and that your AWS credentials are valid.");
        }

        return "https://" + endpointId + ".execute-api." + region + ".amazonaws.com/Prod/";
    }

    public String postEndpoint(String endpoint, String data) {
        String api = getApiEndpint();
        String url = api + endpoint;

        System.out.println("Url in postEndpoint utility class");
        System.out.println(url);
        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();
        try {
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            if (statusCode == 200) {
                return httpResponse.body();
            } else {
                throw new ApiGatewayException("GET request failed: " + statusCode + " status code received");
            }
        } catch (IOException | InterruptedException e) {
            return e.getMessage();
        }
    }

    public String getEndpoint(String endpoint) {
        String api = getApiEndpint();
        String url = api + endpoint;

        System.out.println(url);
        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            if (statusCode == 200) {
                return httpResponse.body();
            } else {
                throw new ApiGatewayException("GET request failed: " + statusCode + " status code received");
            }
        } catch (IOException | InterruptedException e) {
            return e.getMessage();
        }
    }

    public String getAllEndpoint(String endpoint) {
        String api = getApiEndpint();
        String url = api + endpoint;

        System.out.println(url);
        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            if (statusCode == 200) {
                return httpResponse.body();
            } else {
                throw new ApiGatewayException("GET request failed: " + statusCode + " status code received");
            }
        } catch (IOException | InterruptedException e) {
            return e.getMessage();
        }
    }
}
