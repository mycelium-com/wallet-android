package com.mycelium.wapi;

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
        /*
            { skip: false, range: true, percent: false, output: 1, amount: 7000 } - 0x40 0x01 0x20 0x73
            { skip: false, range: false, percent: true, output: 5, amount: 10}    - 0x25 0x0a
            { skip: false, range: false, percent: false, output: 0, amount: 3}    - 0x00 0x03

            { skip: false, range: false, percent: false, output: 0, amount: 3}    - 0x00 0x03
            { skip: false, range: false, percent: false, output: 0, amount: 3}    - 0x00 0x03
            { skip: false, range: false, percent: false, output: 0, amount: 3}    - 0x00 0x03
            { skip: false, range: false, percent: false, output: 0, amount: 3}    - 0x00 0x03
            { skip: false, range: false, percent: false, output: 0, amount: 3}    - 0x00 0x03
            { skip: false, range: false, percent: false, output: 0, amount: 3}    - 0x00 0x03
            { skip: false, range: false, percent: false, output: 0, amount: 3}    - 0x00 0x03
         */

        byte[] script = {0x00, 0x00, 0x43, 0x43, 0x02, 0x15, 0x40, 0x01, 0x20, 0x73, 0x25, 0x0a, 0x00, 0x03};
        List<Integer> outputIndexes = ColuTransferInstructionsParser.retrieveOutputIndexesFromScript(script);
        assertEquals(3, outputIndexes.size());
        assertEquals(1, (int)outputIndexes.get(0));
        assertEquals(5, (int)outputIndexes.get(1));
        assertEquals(0, (int)outputIndexes.get(2));
    }

    @Test
    public void retrieveOutputIndexesFromTooSmallScript() throws Exception {

        byte[] script = {0x00, 0x00, 0x42, 0x40, 0x02};
        List<Integer> outputIndexes = ColuTransferInstructionsParser.retrieveOutputIndexesFromScript(script);
        assertEquals(0, outputIndexes.size());
    }
}