package com.brownfields.github.hellostk3;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import uicc.toolkit.ProactiveHandler;
import uicc.toolkit.ProactiveHandlerSystem;


public class DiagUtil {

    private static final byte DCS_7_BIT_DATA = 0x00; //ETSI TS 102223 Clause 8.15.0
    private static final byte DCS_8_BIT_DATA = 0x04; //ETSI TS 102223 Clause 8.15.0
    private byte[] scratchText;
    private byte[] reasonBytes;
    private final short maxDataLength = 64;

    public DiagUtil() {
        // デバッグ用 文字列作成領域をRAM上に確保する
        short length = (short) (maxDataLength * 2);
        length += (maxDataLength / 16) + 1;

        scratchText = JCSystem.makeTransientByteArray(length, JCSystem.CLEAR_ON_RESET);
        reasonBytes = JCSystem.makeTransientByteArray((short) 2, JCSystem.CLEAR_ON_RESET);
    }

    public static void text(byte[] text, short offset, short length) {
        ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();
        ph.initDisplayText((byte) 0x81, DCS_8_BIT_DATA, text, offset, length);
        ph.send();
    }

    public static void text(byte[] text) {
        text(text, (short) 0, (short) text.length);
    }


    public void error(byte[] exceptionName, short reason) {
        reasonBytes[0] = (byte) (reason >> 8 & 0xFF);
        reasonBytes[1] = (byte) (reason & 0xFF);

        short i = (short) exceptionName.length;
        Util.arrayCopyNonAtomic(exceptionName, (short) 0, scratchText, (short) 0, (short) exceptionName.length);
        scratchText[i++] = (byte) ':';
        short len = ByteUtil.bytesToHex(reasonBytes, (short) 0, (short) 2, scratchText, i);

        text(scratchText, (short) 0, len);
    }

    public void displayBytes(byte[] data, short dataOffset, short dataLength) {
        short length;
        if (dataLength > maxDataLength) {
            scratchText[(short) 0] = '?';
            length = 1;
        } else {
            length = ByteUtil.bytesToHex(data, dataOffset, dataLength, scratchText, (short) 0);
        }

        ProactiveHandler proHdlr = ProactiveHandlerSystem.getTheHandler();
        proHdlr.initDisplayText((byte) 0, DCS_8_BIT_DATA, scratchText, (short) 0, length);
        proHdlr.send();
    }

    public void displayBytes(byte[] data) {
        displayBytes(data, (short) 0, (short) data.length);
    }
}
