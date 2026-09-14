package com.wandile.skillswap.dto;

public class NotificationPayload {
    private String type;
    private String message;
    private Object data;

    public NotificationPayload(String type, String message, Object data) {
        this.type = type;
        this.message = message;
        this.data = data;
    }

    public String getType() { return type; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}