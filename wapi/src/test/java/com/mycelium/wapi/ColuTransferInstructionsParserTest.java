package com.mycelium.wapi;

import com.subgraph.orchid.encoders.Hex;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ColuTransferInstructionsParserTest {
    @Test
    public void getAmountTotalBytesSFFC() throws Exception {
        for (byte b: ColuTransferInstructionsParser.SFFC_FLAG_BYTES_MAP.keySet()) {
            assertEquals((int)ColuTransferInstructionsParser.SFFC_FLAG_BYTES_MAP.get(b), ColuTransferInstructionsParser.getAmountTotalBytesSFFC(b));
        }
    }

    @Test
    public void retrieveOutputIndexesFromScript() throws Exception {
        byte[] script = Hex.decode("00 00 43 43" + // colored coin protocol
                "02 15 " +      // TODO: document
                "40 01 20 73" + // output: 1, amount: 7000
                "25 0a" +       // output: 5, amount: 10
                "00 03");       // output: 0, amount: 3
        List<Integer> outputIndexes = ColuTransferInstructionsParser.retrieveOutputIndexesFromScript(script);
        assertEquals(3, outputIndexes.size());
        assertEquals(1, (int)outputIndexes.get(0));
        assertEquals(5, (int)outputIndexes.get(1));
        assertEquals(0, (int)outputIndexes.get(2));
    }

    @Test
    public void retrieveOutputIndexesFromTooSmallScript() throws Exception {
        byte[] script = Hex.decode("00 00 43 43 02");
        List<Integer> outputIndexes = ColuTransferInstructionsParser.retrieveOutputIndexesFromScript(script);
        assertEquals(0, outputIndexes.size());
    }
}