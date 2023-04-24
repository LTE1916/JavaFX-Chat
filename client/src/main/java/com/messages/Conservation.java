package com.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Conservation implements Serializable {
  private int ID;
  private ArrayList<String> containUsers = new ArrayList<>();

  private Date lastTalk;
  private String name;
  private String picture;

  private int type;
  private int target;

  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getID() {
    return ID;
  }


  public void setID(int ID) {
    this.ID = ID;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setPicture(String picture) {
    this.picture = picture;
  }

  public String getPicture() {
    return picture;
  }

  public void setContainUsers(ArrayList<String> containUsers) {
    this.containUsers = containUsers;
  }

  public ArrayList<String> getContainUsers() {
    return containUsers;
  }

  public Date getLastTalk() {
    return lastTalk;
  }

  public void setLastTalk(Date lastTalk) {
    this.lastTalk = lastTalk;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public int getTarget() {
    return target;
  }

  public void setTarget(int target) {
    this.target = target;
  }
}
