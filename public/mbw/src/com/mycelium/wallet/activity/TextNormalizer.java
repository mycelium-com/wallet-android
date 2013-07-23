package com.mycelium.wallet.activity;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

public class TextNormalizer implements TextWatcher {

   private final Function<String, String> normalizer;
   private final EditText editText;

   public TextNormalizer(Function<String, String> normalizer, EditText editText) {
      this.normalizer = normalizer;
      this.editText = editText;
   }

   @Override
   public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
   }

   @Override
   public void onTextChanged(final CharSequence s, int i, int i2, int i3) {
      String input = Preconditions.checkNotNull(normalizer.apply(s.toString()));
      if (!input.equals(s.toString())) {
         editText.setText(input);
         editText.setSelection(input.length());
      }
   }

   @Override
   public void afterTextChanged(Editable editable) {
   }

}
