package ru.hse.cs.java2020.task03;

public class TrackerQueue {
    public TrackerQueue(String newKey, long newId) {
        this.key = newKey;
        this.id = newId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String newKey) {
        this.key = newKey;
    }

    public long getId() {
        return id;
    }

    public void setId(long newId) {
        this.id = newId;
    }

    private String key;
    private long id;
}
