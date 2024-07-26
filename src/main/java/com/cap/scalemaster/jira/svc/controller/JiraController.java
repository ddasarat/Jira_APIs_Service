package com.cap.scalemaster.jira.svc.controller;

import com.cap.scalemaster.jira.svc.model.SprintDetails;
import com.cap.scalemaster.jira.svc.model.SprintIssueDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.cap.scalemaster.jira.svc.service.JiraService;

import java.io.IOException;
import java.util.List;

@RestController
public class JiraController {

    private final JiraService jiraService;

    public JiraController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @GetMapping("/jira-projects")
    public String getJiraProjects() {
        return jiraService.getProjects();
    }

    @GetMapping("/jira/board/{boardId}/velocity")
    public Double calculateSprintVelocity(@PathVariable String boardId) {
        try {
            return jiraService.calculateSprintVelocity(boardId);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("/jira/sprint/{sprintId}/issues")
    public List<SprintIssueDetails> getIssuesInSprint(@PathVariable("sprintId") String sprintId) {
        try {
            return jiraService.getIssueDetailsInSprint(sprintId);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("/jira/sprint/{sprintId}/details")
    public SprintDetails getSprintDetails(@PathVariable String sprintId) {
        try {
            return jiraService.getSprintDetails(sprintId);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

