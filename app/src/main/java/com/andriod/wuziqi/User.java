package com.andriod.wuziqi;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class User {
    private String username;
    private MainActivity.Status status;
    private String opponentName;
    private int userID;
    private boolean playing;
    private int myPrevMove;
    private int opponentPrevMove;

    public User() {
        this.username = null;
        this.status = MainActivity.Status.idle;
        this.opponentName = "no match yet";
        this.userID = 0;
        this.playing = false;
        this.myPrevMove = -1;
        this.opponentPrevMove = -1;
    }

    public void setStatus(MainActivity.Status s) {
        this.status = s;
    }
    public MainActivity.Status getStatus() {
        return this.status;
    }
    public String getUsername() {
        return this.username;
    }
    public void setUsername(String name) {
        this.username = name;
    }
    public void setOpponentName(String name) {
        this.opponentName = name;
    }
    public String getOpponentName() {
        return this.opponentName;
    }
    public int getUserID() {
        return this.userID;
    }
    public void setUserID(int id) {
        this.userID = id;
    }
    public void setPlaying(boolean playing) {
        this.playing = playing;
    }
    public boolean getPlaying() {
        return this.playing;
    }
    public void setMyPrevMove(int pos) {
        this.myPrevMove = pos;
    }
    public int getMyPrevMove() {
        return this.myPrevMove;
    }
    public void setOpponentPrevMove(int pos) {
        this.opponentPrevMove = pos;
    }
    public int getOpponentPrevMove() {
        return this.opponentPrevMove;
    }
}
