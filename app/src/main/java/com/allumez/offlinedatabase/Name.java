package com.allumez.offlinedatabase;

import android.util.Log;

public class Name {
    private String name;
    private int status;
    private String id;
    private String phone;

    public String getPhone() {
        return phone;
    }

    public Name(String id, String name, String phone, int status) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.status = status;
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getStatus() {
        return status;
    }
}