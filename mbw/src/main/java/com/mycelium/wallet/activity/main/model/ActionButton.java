package com.mycelium.wallet.activity.main.model;

public class ActionButton {
    public int icon;
    public String text;
    public int textColor = 0;
    public Runnable task;

    public ActionButton(String text, Runnable task) {
        this(text, 0, task);
    }

    public ActionButton(String text, int icon, Runnable task) {
        this.icon = icon;
        this.text = text;
        this.task = task;
    }
}
