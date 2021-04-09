package com.mycelium.wallet.event;

public class PageSelectedEvent {
    public int position;
    public String tag;

    public PageSelectedEvent(int position, String tag) {
        this.position = position;
        this.tag = tag;
    }
}
