package com.nineclown.lbarsns.model;

public class AlarmDTO {
    private String destinationUid;
    private String userId;
    private String uid;
    private int kind;
    private String message;
    private Long timestamp;

    public AlarmDTO() {
        this.destinationUid = null;
        this.userId = null;
        this.uid = null;
        this.kind = 0;
        this.message = null;
        this.timestamp = null;
    }
    public String getDestinationUid() {
        return destinationUid;
    }

    public void setDestinationUid(String destinationUid) {
        this.destinationUid = destinationUid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getKind() {
        return kind;
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
