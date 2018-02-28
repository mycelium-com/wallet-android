package com.mycelium.wallet.activity.main.model;


public class ActoinButton {
    public int icon;
    public String text;
    public Runnable task;

    public ActoinButton(String text, Runnable task) {
        this.text = text;
        this.task = task;
    }

    public ActoinButton(String text, int icon, Runnable task) {
        this.icon = icon;
        this.text = text;
        this.task = task;
    }
}
