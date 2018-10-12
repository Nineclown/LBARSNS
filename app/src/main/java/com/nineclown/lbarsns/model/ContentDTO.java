package com.nineclown.lbarsns.model;

import java.util.HashMap;

public class ContentDTO {

    private String explain;
    private String imageUrl;
    private String uid;
    private String userId;
    private Long timestamp;
    private int favoriteCount;
    private HashMap<String, Boolean> favorites;
   // private Comment comment;


    public ContentDTO() {
        explain = null;
        imageUrl = null;
        uid = null;
        userId = null;
        timestamp = null;
        favoriteCount = 0;
        favorites = new HashMap<>();
        //comment = new Comment();
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

    public void setFavorites(HashMap<String, Boolean> favorites) {
        this.favorites = favorites;
    }

   /* public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }
*/

    /*private class Comment {
        private String uid;
        private String userId;
        private String comment;
        private Long timestamp;

        public Comment() {

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
    }*/
}
