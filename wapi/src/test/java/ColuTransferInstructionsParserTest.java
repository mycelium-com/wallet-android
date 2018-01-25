import com.mycelium.wapi.ColuTransferInstructionsParser;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;

/*
    Unit tests for ColuTransferInstructionsParser
 */
public class ColuTransferInstructionsParserTest {
    @Test
    public void parseTest() {
        /*
            { skip: false, range: true, percent: false, output: 1, amount: 7000 } - 0x40 0x01 0x20 0x73
            { skip: false, range: false, percent: true, output: 5, amount: 10}    - 0x25, 0xa
            { skip: false, range: false, percent: false, output: 0, amount: 3}    - 0x0, 0x3
         */

        byte[] script = {0x00,  0x00, 0x43, 0x43, 0x02, 0x15, 0x40, 0x01, 0x20,0x73, 0x25, 0xa, 0x0, 0x3};
        List<Integer> outputIndexes = ColuTransferInstructionsParser.retrieveOutputIndexesFromScript(script);
        assertEquals(outputIndexes.size(), 3);
        assertEquals((int)outputIndexes.get(0), 1);
        assertEquals((int)outputIndexes.get(1), 5);
        assertEquals((int)outputIndexes.get(2), 0);
    }
}
