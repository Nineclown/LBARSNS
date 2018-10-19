package com.nineclown.lbarsns.model;

import java.util.HashMap;

public class FollowDTO {
    private int followerCount;
    private HashMap<String, Boolean> followers;

    private int followingCount;
    private HashMap<String, Boolean> followings;

    public FollowDTO() {
        this.followerCount = 0;
        this.followers = new HashMap<>();

        this.followingCount = 0;
        this.followings = new HashMap<>();
    }


    public int getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    public HashMap<String, Boolean> getFollowers() {
        return followers;
    }

    public void setFollowers(String uid, boolean bool) {
        this.followers.put(uid, bool);
    }

    public HashMap<String, Boolean> getFollowings() {
        return followings;
    }

    public void setFollowings(String uid, boolean bool) {
        this.followings.put(uid, bool);
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }
}
