package com.moneymanager.model;

import java.time.OffsetDateTime;

public class User {

    private long userId;
    private String username;
    private String passwordHash;
    private String securityQuestion;
    private String securityAnswerHash;
    private OffsetDateTime createdAt;

    public User() {}

    public User(long userId, String username, String passwordHash,
                String securityQuestion, String securityAnswerHash,
                OffsetDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.securityQuestion = securityQuestion;
        this.securityAnswerHash = securityAnswerHash;
        this.createdAt = createdAt;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }

    public String getSecurityAnswerHash() { return securityAnswerHash; }
    public void setSecurityAnswerHash(String securityAnswerHash) { this.securityAnswerHash = securityAnswerHash; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
