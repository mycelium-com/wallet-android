package com.mycelium.wallet.activity.main.model;

import java.util.Objects;

public class ActionButton {
    public int id;
    public int icon;
    public String text;
    public int textColor = 0;

    public ActionButton(int actionId, String text) {
        this(actionId, text, 0);
    }

    public ActionButton(int actionId, String text, int icon) {
        this.id = actionId;
        this.icon = icon;
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionButton that = (ActionButton) o;
        return id == that.id &&
                icon == that.icon &&
                textColor == that.textColor &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, icon, text, textColor);
    }
}
