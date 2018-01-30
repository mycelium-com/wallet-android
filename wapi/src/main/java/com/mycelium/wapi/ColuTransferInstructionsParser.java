package com.mycelium.wapi;

import com.mrd.bitlib.util.HexUtils;
import com.mycelium.WapiLogger;

import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ColuTransferInstructionsParser class provides parsing of Transfer instructions
 * that are placed inside Colu transaction's OP_RETURN output script
 * Transfer instructions binary format description can be found:
 * https://github.com/Colored-Coins/Colored-Coins-Protocol-Specification/wiki/Transfer%20Instructions
 * Amount is packed using SFFC format (https://github.com/Colored-Coins/SFFC)
 */
public class ColuTransferInstructionsParser {
    private static final int OPCODE_OFFSET = 5;
    private static final int FLAG_MASK = 0xe0;
    private static final int RANGE_FLAG = 0x40;
    private static final int PERCENT_FLAG = 0x20;

    static final Map<Byte, Integer> SFFC_FLAG_BYTES_MAP = new LinkedHashMap<Byte, Integer>(){{
        put((byte)0x20, 2);
        put((byte)0x40, 3);
        put((byte)0x60, 4);
        put((byte)0x80, 5);
        put((byte)0xa0, 6);
        put((byte)0xc0, 7);
    }};

    protected final WapiLogger logger;

    private static final int TORRENT_HASH_LEN = 20;
    private static final int SHA2_LEN = 32;

    private static final int OPCODE_ISSUANCE_TORRENT_METADATA = 0x01;
    private static final int OPCODE_TRANSFER_TORRENT_METADATA = 0x10;
    private static final int OPCODE_BURN_TRANSFER_METADATA = 0x20;
    private static final int OPCODE_ISSUANCE_TORRENT_HASH_MS = 0x02;
    private static final int OPCODE_ISSUANCE_TORRENT_HASH = 0x04;
    private static final int OPCODE_TRANSFER_TORRENT_HASH = 0x13;
    private static final int OPCODE_TRANSFER_TORRENT_HASH_NO_RULES = 0x14;
    private static final int OPCODE_BURN_TORRENT_HASH = 0x23;
    private static final int OPCODE_BURN_TORRENT_HASH_NO_RULES = 0x24;

    private static final int PROTOCOL_IDENTIFIER_BYTE = 0x43;
    public static final int SCRIPTBYTES_MIN_SIZE = 8;

    static int getAmountTotalBytesSFFC(byte flagByte) {
        for(byte ssfcFlagByte : SFFC_FLAG_BYTES_MAP.keySet()) {
            if(flagByte == ssfcFlagByte) {
                return SFFC_FLAG_BYTES_MAP.get(ssfcFlagByte);
            }
        }
        return 1;
    }

    public ColuTransferInstructionsParser(WapiLogger logger) {
        this.logger = logger;
    }

    //Checks the minimum length of script and protocol identifier
    public boolean isValidColuScript(byte []scriptBytes) {
       return (scriptBytes.length >= SCRIPTBYTES_MIN_SIZE
               && scriptBytes[2] == PROTOCOL_IDENTIFIER_BYTE
               && scriptBytes[3] == PROTOCOL_IDENTIFIER_BYTE);
    }

    public List<Integer> retrieveOutputIndexesFromScript(byte []scriptBytes) throws ParseException {
        List<Integer> indexes = new ArrayList<>();

        if (!isValidColuScript(scriptBytes)) {
            return indexes;
        }

        int offset = OPCODE_OFFSET;

        //we don't have issue byte in transfer and burn transactions
        int issueByteLen = 0;

        //Handle the case of issue transaction - it will contain issue byte at the end of OP_RETURN data
        if (scriptBytes[offset] >= OPCODE_ISSUANCE_TORRENT_METADATA && scriptBytes[offset] <= 0x0F)
            issueByteLen = 1;

        //Analyze OP_CODE value to determine type of CC transaction
        switch(scriptBytes[offset]) {
            case OPCODE_ISSUANCE_TORRENT_METADATA:
                offset += (TORRENT_HASH_LEN + SHA2_LEN + 1);
                offset += getAmountTotalBytesSFFC(scriptBytes[offset]);
                break;
            case OPCODE_TRANSFER_TORRENT_METADATA:
            case OPCODE_BURN_TRANSFER_METADATA:
                offset += (TORRENT_HASH_LEN + SHA2_LEN + 1);
                break;
            case OPCODE_ISSUANCE_TORRENT_HASH_MS:
            case OPCODE_ISSUANCE_TORRENT_HASH:
            case OPCODE_TRANSFER_TORRENT_HASH:
            case OPCODE_TRANSFER_TORRENT_HASH_NO_RULES:
            case OPCODE_BURN_TORRENT_HASH:
            case OPCODE_BURN_TORRENT_HASH_NO_RULES:
                offset += (TORRENT_HASH_LEN + 1);
                break;
            default:
                offset += 1;
        }
        try {
            while (offset < (scriptBytes.length - issueByteLen)) {
                byte curByte = scriptBytes[offset];
                if ((curByte & RANGE_FLAG) == RANGE_FLAG) {
                    //Range flag is set - output index is between 0..8191
                    int outputIndex = new BigInteger(new byte[]{(byte) (curByte & (~FLAG_MASK)), scriptBytes[offset + 1]}).intValue();
                    indexes.add(outputIndex);
                    offset += 2;
                } else {
                    //Range flag is not set - output index is between 0..31
                    indexes.add(curByte & (~FLAG_MASK));
                    offset += 1;
                }

                if ((curByte & PERCENT_FLAG) == PERCENT_FLAG) {
                    //Amount in percents consumes 1 byte
                    offset += 1;
                } else {
                    //Number of units in SFFC format consumes totally 2 to 7 bytes
                    int amountTotalBytes = getAmountTotalBytesSFFC(scriptBytes[offset]);
                    offset += amountTotalBytes;
                }
            }
        } catch(IndexOutOfBoundsException ex) {
            logger.logError("retrieveOutputIndexesFromScript(" + HexUtils.toHex(scriptBytes) + ") script could not be parsed. Assuming invalid script.");
            throw new ParseException("Can't parse the script", offset);
            // TODO: 30.01.18 make it not throw here. we throw as we are not 100% sure this is not a colored coind script.
        }
        return indexes;
    }
}
