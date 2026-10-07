package com.wandile.skillswap.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class StartConversationRequest {
    @NotEmpty
    private List<String> participantEmails;

    public List<String> getParticipantEmails() { return participantEmails; }
    public void setParticipantEmails(List<String> participantEmails) { this.participantEmails = participantEmails; }
}