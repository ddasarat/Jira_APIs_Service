package com.cap.scalemaster.jira.svc.service;

import com.cap.scalemaster.jira.svc.model.SprintDetails;
import com.cap.scalemaster.jira.svc.model.SprintInfo;
import com.cap.scalemaster.jira.svc.model.SprintIssueDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class JiraService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final Logger logger = LoggerFactory.getLogger(JiraService.class);
    @Value("${jira.url}")
    private String jiraUrl;
    @Value("${jira.username}")
    private String username;
    @Value("${jira.api.token}")
    private String apiToken;

    public JiraService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    private CloseableHttpClient createHttpClient() {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, apiToken));
        return HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
    }

    public String getProjects() {
        String uri = UriComponentsBuilder.fromHttpUrl(jiraUrl).toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, apiToken);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String endPoint = uri.concat("/rest/api/3/project");

        ResponseEntity<String> response = restTemplate.exchange(endPoint, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    private JsonNode getJsonResponse(String endpoint) throws IOException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet request = new HttpGet(jiraUrl + endpoint);
            request.addHeader("Accept", "application/json");

            // Add Basic Authentication header
            String auth = username + ":" + apiToken;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            request.addHeader("Authorization", authHeader);

            HttpResponse response = httpClient.execute(request);
            String json = EntityUtils.toString(response.getEntity());
            logger.info("Received JSON response: {}", json);
            return objectMapper.readTree(json);
        }
    }

    public JsonNode getSprints(String boardId) throws IOException {

        String getSprintsEndpoint = "/rest/agile/1.0/board/" + boardId + "/sprint";


        JsonNode jsonSprintsResponse = getJsonResponse(getSprintsEndpoint);
        logger.info("Get Spring Response URL: {}, Service Response:{}", getSprintsEndpoint, jsonSprintsResponse);
        return jsonSprintsResponse;
    }

    public SprintDetails getSprintDetails(String sprintId) throws IOException {
        JsonNode issuesNode = getJsonResponse("/rest/agile/1.0/sprint/" + sprintId + "/issue");
        SprintInfo sprintInfo = getSprintInfo(sprintId);
        SprintDetails sprintDetails = new SprintDetails();
        sprintDetails.setSprintId(sprintInfo.getSprintId());
        sprintDetails.setSprintName(sprintInfo.getSprintName());
        List<SprintIssueDetails> issueDetailsList = new ArrayList<>();
        int totalStoryPoints = 0;

        if (issuesNode.has("issues")) {
            for (JsonNode issueNode : issuesNode.get("issues")) {
                SprintIssueDetails details = new SprintIssueDetails();

                details.setIssueId(issueNode.get("id").asText());
                details.setIssueKey(issueNode.get("key").asText());
                details.setIssueSummary(issueNode.get("fields").get("summary").asText());
                details.setAssigneeName(issueNode.get("fields").get("assignee") != null ? issueNode.get("fields").get("assignee").get("displayName").asText() : "Unassigned");
                details.setStoryPoints(issueNode.get("fields").get("customfield_10016") != null ? issueNode.get("fields").get("customfield_10016").asDouble() : null);

                if (details.getStoryPoints() != null) {
                    totalStoryPoints += details.getStoryPoints();
                }

                issueDetailsList.add(details);
            }
        }

        sprintDetails.setIssues(issueDetailsList);
        sprintDetails.setVelocity(totalStoryPoints);

        return sprintDetails;
    }

    private SprintInfo getSprintInfo(String sprintId) throws IOException {
        JsonNode sprintNode = getJsonResponse("/rest/agile/1.0/sprint/" + sprintId);
        SprintInfo sprintInfo = new SprintInfo();
        sprintInfo.setSprintId(sprintNode.get("id").asText());
        sprintInfo.setSprintName(sprintNode.get("name").asText());
        return sprintInfo;
    }

   public Double calculateSprintVelocity(String boardId) throws IOException {
        double velocity = 0.0;
        JsonNode sprints = getSprints(boardId);

        if (sprints.has("values")) {
            for (JsonNode sprint : sprints.get("values")) {
                String sprintId = sprint.get("id").asText();
                JsonNode issues = getIssuesInSprint(sprintId);

                if (issues.has("issues")) {
                    for (JsonNode issue : issues.get("issues")) {
                        if (issue.has("fields") && issue.get("fields").has("customfield_10016")) {
                            velocity += issue.get("fields").get("customfield_10016").asDouble(0.0);
                        }
                    }
                }
            }
        }

        logger.info("Calculated Velocity: {}", velocity);
        return velocity;
    }

    public JsonNode getIssuesInSprint(String sprintId) throws IOException {
        return getJsonResponse("/rest/agile/1.0/sprint/" + sprintId + "/issue");
    }
    public List<SprintIssueDetails> getIssueDetailsInSprint(String sprintId) throws IOException {
        JsonNode issuesNode = getJsonResponse("/rest/agile/1.0/sprint/" + sprintId + "/issue");
        List<SprintIssueDetails> issueDetailsList = new ArrayList<>();

        if (issuesNode.has("issues")) {
            for (JsonNode issueNode : issuesNode.get("issues")) {
                SprintIssueDetails details = new SprintIssueDetails();

                details.setIssueId(issueNode.get("id").asText());
                details.setIssueKey(issueNode.get("key").asText());
                details.setIssueSummary(issueNode.get("fields").get("summary").asText());
                details.setAssigneeName(issueNode.get("fields").get("assignee") != null ? issueNode.get("fields").get("assignee").get("displayName").asText() : "Unassigned");
                details.setStoryPoints(issueNode.get("fields").get("customfield_10016") != null ? issueNode.get("fields").get("customfield_10016").asDouble() : null);
                issueDetailsList.add(details);
            }
        }

        return issueDetailsList;
    }
}

