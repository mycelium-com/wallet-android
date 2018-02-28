package com.mycelium.wallet.activity.main.adapter;


import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.main.model.ActoinButton;

import java.util.ArrayList;
import java.util.List;

public class ButtonPagerAdapter extends PagerAdapter {
    private List<ActoinButton> buttons = new ArrayList<>();

    public ButtonPagerAdapter(List<ActoinButton> buttons) {
        this.buttons = buttons;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        View view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.item_action_button, container, false);
        Button button = view.findViewById(R.id.btn_action);
        button.setText(buttons.get(position).text);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttons.get(position).task != null) {
                    buttons.get(position).task.run();
                }
            }
        });
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return buttons.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

}
