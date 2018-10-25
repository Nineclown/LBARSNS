package com.nineclown.lbarsns.model;

public class FcmDTO {
    private Notification notification;
    private String to;

    public FcmDTO() {
        this.to = null;
        this.notification = new Notification();
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(String title, String body) {
        this.notification.setTitle(title);
        this.notification.setBody(body);
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

}

class Notification {
    private String title;
    private String body;

    public Notification() {
        this.body = null;
        this.title = null;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
