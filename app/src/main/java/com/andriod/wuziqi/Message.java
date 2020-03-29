package com.andriod.wuziqi;

public class Message {
    private String text;
    private String owner;

    public Message (String text, String owner) {
        this.text = text;
        this.owner = owner;
    }

    public String getText() {
        return this.text;
    }
    public String getOwner() {
        return this.owner;
    }
}
