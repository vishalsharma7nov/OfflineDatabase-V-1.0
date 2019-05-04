package com.allumez.offlinedatabase;

public class Name {
    private String name;
    private int status;
    private String id;

    public Name(String id,String name, int status) {
        this.id = id;
        this.name = name;
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