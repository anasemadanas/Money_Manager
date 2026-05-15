package com.moneymanager.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Contribution {

    private long contributionId;
    private long goalId;
    private BigDecimal amount;
    private String note;
    private OffsetDateTime contributedAt;

    public Contribution() {}

    public long getContributionId() { return contributionId; }
    public void setContributionId(long contributionId) { this.contributionId = contributionId; }

    public long getGoalId() { return goalId; }
    public void setGoalId(long goalId) { this.goalId = goalId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public OffsetDateTime getContributedAt() { return contributedAt; }
    public void setContributedAt(OffsetDateTime contributedAt) { this.contributedAt = contributedAt; }
}
