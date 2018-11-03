package com.nineclown.lbarsns.model;

public class TravelDTO {

    private String uid;
    private String userId;
    private String travelId;
    private Long timestamp;

    public static class LatLon {
        private Double latitude;
        private Double longitude;
        private Long timestamp;

        public Double getLatitude() { return latitude; }

        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }

        public void setLongitude(Double longitude) { this.longitude = longitude; }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

    }

    public TravelDTO() {
        this.uid = null;
        this.userId = null;
        this.travelId = null;
        this.timestamp = null;

    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTravelId() { return travelId; }

    public void setTravelId(String name) { this.travelId = name; }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

}
