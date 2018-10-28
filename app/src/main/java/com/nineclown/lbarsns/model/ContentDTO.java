package com.nineclown.lbarsns.model;

import java.util.HashMap;

public class ContentDTO {

    private String explain;
    private String imageUrl;
    private String uid;
    private String userId;
    private Double latitude;
    private Double longitude;
    private Long timestamp;
    private int favoriteCount;
    private HashMap<String, Boolean> favorites;

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public static class Comment {
        private String uid;
        private String userId;
        private String comment;
        private Long timestamp;

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

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }


    public ContentDTO() {
        this.explain = null;
        this.imageUrl = null;
        this.uid = null;
        this.userId = null;
        this.latitude = null;
        this.longitude = null;
        this.timestamp = null;
        this.favoriteCount = 0;
        this.favorites = new HashMap<>();
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(int favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public HashMap<String, Boolean> getFavorites() {
        return favorites;
    }

    public void setFavorites(String uid, boolean bool) {
        this.favorites.put(uid, bool);
    }


}
