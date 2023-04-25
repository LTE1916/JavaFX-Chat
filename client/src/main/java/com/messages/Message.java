package com.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Message implements Serializable {

    private String name;//senderID或者是SEVER
    private String target;//targetID

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    private MessageType type;
    private int conversationType;
    private Date snedDate;

    public void setConversationType(int type){this.conversationType = type;}
    public int getConversationType(){return conversationType;}
    private int conversationID;
    public void setConversationID(int conversationID){this.conversationID = conversationID;}
    public int getConversationID(){return conversationID;}
    private int id;
    public void setID(int id){this.id = id;}
    public int getID(){return id;}
    private String msg;
    private int count;
    private ArrayList<User> list;
    private ArrayList<User> users;

    private Status status;

    public byte[] getVoiceMsg() {
        return voiceMsg;
    }

    private byte[] voiceMsg;

    private  byte[]file;

    public String getPicture() {
        return picture;
    }

    private String picture;

    public Message() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMsg() {

        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public ArrayList<User> getUserlist() {
        return list;
    }

    public void setUserlist(HashMap<String, User> userList) {
        this.list = new ArrayList<>(userList.values());
    }

    public void setOnlineCount(int count){
        this.count = count;
    }

    public int getOnlineCount(){
        return this.count;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }


    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setVoiceMsg(byte[] voiceMsg) {
        this.voiceMsg = voiceMsg;
    }

    public void setSnedDateDate(Date date) {
        snedDate = date;
  }

    public Date getSendDate(){return snedDate;}

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }
}
