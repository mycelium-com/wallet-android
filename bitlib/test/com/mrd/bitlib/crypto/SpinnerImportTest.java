package com.mrd.bitlib.crypto;


import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class SpinnerImportTest {

   @Test
    public void spinnerImprt() {
      SpinnerPrivateUri spinnerImport = SpinnerPrivateUri.fromSpinnerUri("bsb:6hm5yUxrSXRYpPkeu5HrfcQ8BXbf6e7d91AQtwa6ViUz?net=0");
       Address addr = Address.fromStandardPublicKey(spinnerImport.key.getPublicKey(), NetworkParameters.productionNetwork);
       assertEquals("1Ea3kC4swu6v6rnaEe1BDDkek85286YAiL",addr.toString());
    }
}
