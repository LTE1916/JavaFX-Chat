package com.messages;

import java.io.Serializable;
/*
在线列表中的每个用户对应一个User对象
包含用户名称、头像、在线状态
 */
public class User implements Serializable {

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String name;

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    String picture;
    Status status;
}
