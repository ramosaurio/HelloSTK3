package com.brownfields.github.hellostk3;

import javacard.framework.JCSystem;
import uicc.toolkit.EnvelopeHandler;
/**
 * STKHandler class
 *
 * Handles SIM Toolkit (STK) events and coordinates UICC information retrieval,
 * JSON payload construction, and HTTP POST sending through BIP.
 */
public class STKHandler {
    // Providers and utilities
    private UICCInfoProvider uiccInfoProvider;
    private DiagUtil diag;
    private BIPManager bipManager;

    // Diagnostic error message
    private static byte[] STKHANDLER_ERROR_GENERAL = new byte[]{'E', 'R', 'R', 'O', 'R', '_', 'S', 'T', 'K'};

    // Buffer for building JSON payload
    private byte[] jsonBodyBuffer;

    // JSON keys to include in the body
    private static final byte[] keys = {
            'i', 'c', 'c', 'i', 'd',   // 5 bytes
            'i', 'm', 'e', 'i',        // 4 bytes
            'm', 'c', 'c',             // 3 bytes
            'm', 'n', 'c'              // 3 bytes
    };

    // Offsets for each key inside the flat array
    private static final short[] keyOffsets = { 0, 5, 9, 12 };

    // Lengths for each key
    private static final short[] keyLengths = { 5, 4, 3, 3 };

    // Server information
    static byte[] serverAddr = {(byte) 178, (byte) 63, (byte) 67, (byte) 106};
    static short serverPort = (short) 80;
    static final byte[] apiPath = {'/', '7', '3', '5', '2', '7', '1', 'd', '5', '-', '8', '6', '4', '0', '-', '4', '8', 'f', 'c', '-', 'b', 'f', '7', '6', '-', 'a', '4', 'f', 'e', '8', '1', '9', '6', '6', '3', '1', 'f'};
    static final byte[] hostName = {'w', 'e', 'b', 'h', 'o', 'o', 'k', '.', 's', 'i', 't', 'e'};

    /**
     * Constructor
     *
     * @param diag Diagnostic utility for logging
     * @param tmpBuffer Shared temporary buffer
     */
    public STKHandler(DiagUtil diag, byte[] tmpBuffer) {
        this.diag = diag;
        this.bipManager = new BIPManager(diag, tmpBuffer);
        uiccInfoProvider = new UICCInfoProvider(tmpBuffer, diag);
        jsonBodyBuffer = JCSystem.makeTransientByteArray((short) 320, JCSystem.CLEAR_ON_RESET);
    }

    /**
     * Handles EVENT_MENU_SELECTION.
     *
     * Builds a JSON payload with UICC information and sends it over HTTP POST using BIP.
     */
    public void eventMenuSelection() {
        try {

            byte[] fullBuffer = uiccInfoProvider.getFullBuffer();
            short[] valueOffsets = uiccInfoProvider.getFullOffsets();
            short[] valueLengths = uiccInfoProvider.getFullLengths();

            short bodyLength = JsonUtil.buildJson(
                    keys, keyOffsets, keyLengths,
                    fullBuffer, valueOffsets, valueLengths,
                    jsonBodyBuffer, (short) 0
            );

            bipManager.sendHTTPPost(jsonBodyBuffer, bodyLength, serverAddr, serverPort, apiPath, hostName);
        } catch (Exception e) {
            DiagUtil.text(STKHANDLER_ERROR_GENERAL);


        }

    }

    /**
     * Handles EVENT_DOWNLOAD_DATA_AVAILABLE.
     *
     * Delegates processing to BIPManager.
     */
    public void processEventEventDownloadDataAvailable(EnvelopeHandler eh) {
        try {
            bipManager.processEventEventDownloadDataAvailable(eh);

        } catch (Exception e) {
            DiagUtil.text(STKHANDLER_ERROR_GENERAL);

        }
    }

    /**
     * Handles EVENT_DOWNLOAD_CHANNEL_STATUS.
     *
     * Delegates processing to BIPManager.
     */
    public void procesEventEventDownloadChannelStatus(EnvelopeHandler eh) {
        try {
            bipManager.procesEventEventDownloadChannelStatus(eh);

        } catch (Exception e) {
            DiagUtil.text(STKHANDLER_ERROR_GENERAL);

        }
    }


}
