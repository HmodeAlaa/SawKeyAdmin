package com.ladrraza.sawkeyadmin;

public class KeyItem {
    public final String code;
    public final String expiry;
    public final String status;
    public final String hwid;
    public final String note;

    public KeyItem(String code, String expiry, String status, String hwid, String note) {
        this.code = code;
        this.expiry = expiry;
        this.status = status;
        this.hwid = hwid;
        this.note = note;
    }
}
