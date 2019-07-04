package com.mycelium.wapi;

import com.mrd.bitlib.util.HexUtils;
import com.mycelium.WapiLogger;

import com.mycelium.wapi.wallet.ColuTransferInstructionsParser;
import org.junit.Test;

import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.*;

public class ColuTransferInstructionsParserTest {
    private final ColuTransferInstructionsParser coluTransferInstructionsParser = new ColuTransferInstructionsParser(WapiLogger.NULL_LOGGER);
    @Test
    public void getAmountTotalBytesSFFC() throws Exception {
        for (byte b: ColuTransferInstructionsParser.SFFC_FLAG_BYTES_MAP.keySet()) {
            assertEquals((int)ColuTransferInstructionsParser.SFFC_FLAG_BYTES_MAP.get(b), ColuTransferInstructionsParser.getAmountTotalBytesSFFC(b));
        }
    }

    @Test
    public void retrieveOutputIndexesFromScript() throws Exception {
        byte[] script = HexUtils.toBytes("00 00 43 43" + // colored coin protocol
                "02 15 " +      // TODO: document
                "40 01 20 73" + // output: 1, amount: 7000
                "25 0a" +       // output: 5, amount: 10
                "00 03");       // output: 0, amount: 3
        List<Integer> outputIndexes = coluTransferInstructionsParser.retrieveOutputIndexesFromScript(script);
        assertEquals(3, outputIndexes.size());
        assertEquals(1, (int)outputIndexes.get(0));
        assertEquals(5, (int)outputIndexes.get(1));
        assertEquals(0, (int)outputIndexes.get(2));
    }

    @Test
    public void retrieveOutputIndexesFromTooSmallScript() throws Exception {
        byte[] script = HexUtils.toBytes("00 00 43 43 02");
        List<Integer> outputIndexes = coluTransferInstructionsParser.retrieveOutputIndexesFromScript(script);
        assertEquals(0, outputIndexes.size());
    }

    @Test(expected = ParseException.class)
    public void retrieveOutputFromCrashingScript() throws Exception {
        // Transaction 811ce060b88cec67e5066f7bb75cd8acef43c3f24c0787fb1fe426510d4fe38b contains the invalid script
        byte[] script = HexUtils.toBytes("6a 07 43 43 02 15 00 21 64");
        // this may not crash but result in no output being found.
        // Or am I interpreting wrongly the fact that the CC blockexplorer doesn't see this as a CC transaction, neither?
        // http://coloredcoins.org/explorer/list/811ce060b88cec67e5066f7bb75cd8acef43c3f24c0787fb1fe426510d4fe38b
        List<Integer> outputIndexes = coluTransferInstructionsParser.retrieveOutputIndexesFromScript(script);
        assertEquals(0, outputIndexes.size());
    }
}