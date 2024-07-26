package com.cap.scalemaster.jira.svc.model;



import java.util.List;

public class SprintDetails {
    private String sprintId;
    private String sprintName;
    private int velocity;
    private List<SprintIssueDetails> issues;

    // Getters and Setters

    public String getSprintId() {
        return sprintId;
    }

    public void setSprintId(String sprintId) {
        this.sprintId = sprintId;
    }

    public String getSprintName() {
        return sprintName;
    }

    public void setSprintName(String sprintName) {
        this.sprintName = sprintName;
    }

    public int getVelocity() {
        return velocity;
    }

    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    public List<SprintIssueDetails> getIssues() {
        return issues;
    }

    public void setIssues(List<SprintIssueDetails> issues) {
        this.issues = issues;
    }
}
