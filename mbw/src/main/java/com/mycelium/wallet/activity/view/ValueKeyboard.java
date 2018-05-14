package com.mycelium.wallet.activity.view;


import android.content.Context;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;

import java.math.BigDecimal;

public class ValueKeyboard extends GridLayout {
    public final static int DEL = -1;
    public final static int DOT = -2;
    public final static int MAX_DIGITS_BEFORE_DOT = 9;

    InputListener inputListener = null;
    TextView inputTextView = null;

    int maxDecimals = 0;

    public ValueKeyboard(Context context) {
        super(context);
    }

    public ValueKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ValueKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TextView getInputTextView() {
        return inputTextView;
    }

    public void setMaxDecimals(int maxDecimals) {
        this.maxDecimals = maxDecimals;
        value.setEntry(value.getEntryAsBigDecimal(), maxDecimals);
        updateDotBtn();
    }

    BigDecimal spendableValue = BigDecimal.ZERO;

    public void setSpendableValue(BigDecimal spendableValue) {
        this.spendableValue = spendableValue;
        updateMaxBtn();
    }

    BigDecimal maxValue;

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    public void setInputTextView(TextView inputTextView) {
        this.inputTextView = inputTextView;
    }

    NumberEntry value = new NumberEntry(maxDecimals, "", new EntryChange() {
        @Override
        public void entryChange(String entry, boolean wasSet) {
            if (inputTextView != null) {
                inputTextView.setText(entry);
            }
        }
    });

    public void setEntry(String val) {
        value = new NumberEntry(maxDecimals, val, new EntryChange() {
            @Override
            public void entryChange(String entry, boolean wasSet) {
                if (inputTextView != null) {
                    inputTextView.setText(entry);
                }
            }
        });
    }

    public void setInputListener(InputListener inputListener) {
        this.inputListener = inputListener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view.getId() == R.id.btn_max) {
                        value.setEntry(spendableValue, maxDecimals);
                    } else if (view.getId() == R.id.btn_backspace) {
                        value.clicked(DEL);
                    } else if (view.getId() == R.id.btn_dot) {
                        value.clicked(DOT);
                    } else if (view.getId() == R.id.btn_done) {
                        done();
                    } else if (view.getId() == R.id.btn_copy) {
                        String clipboardString = Utils.getClipboardString(getContext());
                        if (!clipboardString.isEmpty()) {
                            try {
                                value.setEntry(new BigDecimal(clipboardString), maxDecimals);
                            } catch (NumberFormatException ignore) {
                            }
                        }
                    } else if (view instanceof TextView) {
                        value.clicked(Integer.parseInt(((TextView) view).getText().toString()));
                    }
                }
            });
        }

        updateDotBtn();
        updateMaxBtn();

        findViewById(R.id.btn_backspace).setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                value.clear();
                return true;
            }
        });
    }

    public void setMaxText(String text, float size) {
        TextView textView = findViewById(R.id.btn_max);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setPasteVisibility(int visibility) {
        findViewById(R.id.btn_copy).setVisibility(visibility);
    }

    public void done() {
        setVisibility(View.GONE);
        if (inputListener != null) {
            inputListener.done();
        }
    }

    private void updateDotBtn() {
        findViewById(R.id.btn_dot).setEnabled(maxDecimals > 0);
    }

    private void updateMaxBtn() {
        findViewById(R.id.btn_max).setVisibility(spendableValue.equals(BigDecimal.ZERO) ? View.INVISIBLE : View.VISIBLE);
    }

    public interface InputListener {
        void input(CharSequence sequence);

        void max();

        void backspace();

        void done();
    }

    public static class SimpleInputListener implements InputListener {

        @Override
        public void input(CharSequence sequence) {

        }

        @Override
        public void max() {

        }

        @Override
        public void backspace() {

        }

        @Override
        public void done() {

        }
    }

    interface EntryChange {
        void entryChange(String entry, boolean wasSet);
    }

    class NumberEntry {
        int _maxDecimals;
        String entry;
        EntryChange entryChange;

        public NumberEntry(int _maxDecimals, String entry, EntryChange entryChange) {
            this._maxDecimals = _maxDecimals;
            this.entry = entry;
            this.entryChange = entryChange;
            if (!entry.isEmpty()) {
                try {
                    entry = new BigDecimal(entry).toPlainString();
                } catch (Exception e) {
                    entry = "";
                }
            }

//            if (_maxDecimals > 0) {
//                setClickListener(_llNumberEntry.findViewById(R.id.btDot) as Button, DOT)
//            } else {
//                (_llNumberEntry.findViewById(R.id.btDot) as Button).text = ""
//            }
//            setClickListener(_llNumberEntry.findViewById(R.id.btZero) as Button, 0)
//            setClickListener(_llNumberEntry.findViewById(R.id.btDel) as Button, DEL)

//            _llNumberEntry.findViewById(R.id.btDel).setOnLongClickListener {
//                entry = ""
//                _listener.onEntryChanged(entry, false)
//                true
//            }
        }

        void clear() {
            entry = "";
            entryChange.entryChange(entry, false);
        }

        void clicked(int digit) {
            if (entry.equals("0")) {
                entry = "";
            }
            if (digit == DEL) {
                // Delete Digit
                if (!entry.isEmpty()) {
                    entry = entry.substring(0, entry.length() - 1);
                }
            } else if (digit == DOT) {
                // Do we already have a dot?
                if (hasDot()) {
                    return;
                }
                if (_maxDecimals == 0) {
                    return;
                }
                if (entry.isEmpty()) {
                    entry = "0.";
                } else {
                    entry += '.';
                }
            } else {
                // Append Digit
                if (digit == 0 && "0".equals(entry)) {
                    // Only one leading zero
                    return;
                }
                if (hasDot()) {
                    if (decimalsAfterDot() >= _maxDecimals) {
                        // too many decimals
                        return;
                    }
                } else {
                    if (decimalsBeforeDot() >= MAX_DIGITS_BEFORE_DOT) {
                        return;
                    }
                }
                if (maxValue == null || new BigDecimal(entry + digit).compareTo(maxValue) <= 0) {
                    entry = entry + digit;
                }
            }
            entryChange.entryChange(entry, false);
        }

        private boolean hasDot() {
            return entry.indexOf('.') != -1;
        }

        private int decimalsAfterDot() {
            int dotIndex = entry.indexOf('.');
            if (dotIndex == -1) {
                return 0;
            }
            return entry.length() - dotIndex - 1;
        }

        private int decimalsBeforeDot() {
            int dotIndex = entry.indexOf('.');
            if (dotIndex == -1) {
                return entry.length();
            }
            return dotIndex;
        }

        void setEntry(BigDecimal number, int maxDecimals) {
            _maxDecimals = maxDecimals;
            if (number == null || number.compareTo(BigDecimal.ZERO) == 0) {
                entry = "";
            } else {
                entry = number.setScale(_maxDecimals, BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toPlainString();
            }
            entryChange.entryChange(entry, true);
        }


        public BigDecimal getEntryAsBigDecimal() {
            if (entry.isEmpty()) {
                return BigDecimal.ZERO;
            }
            if ("0.".equals(entry)) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(entry);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
    }
}
