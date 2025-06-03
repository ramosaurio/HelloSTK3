package com.brownfields.github.hellostk3;

/**
 * Project: HelloSTK3 - SIM Toolkit and BIP Communication Applet
 *
 * This project implements a JavaCard applet that integrates with the SIM Toolkit (STK)
 * environment and uses Bearer Independent Protocol (BIP) to send information collected
 * from the UICC (SIM card) to a remote HTTP server.
 *
 * The applet follows a modular architecture, splitting responsibilities across different components:
 *
 * - {@link com.brownfields.github.hellostk3.STKHandler}: Coordinates event handling, builds JSON payloads,
 *   and initiates HTTP communications.
 *
 * - {@link com.brownfields.github.hellostk3.BIPManager}: Manages BIP channels and the sending/receiving of data.
 *
 * - {@link com.brownfields.github.hellostk3.UICCInfoProvider}: Handles retrieval of UICC information such as ICCID, IMEI, MCC, and MNC.
 *
 * - {@link com.brownfields.github.hellostk3.JsonUtil}: Utility class for generating compact JSON structures.
 *
 * - {@link com.brownfields.github.hellostk3.DiagUtil}: Simplified diagnostic utility for error reporting.
 *
 * ## Supported STK Events:
 * - EVENT_MENU_SELECTION
 * - EVENT_DOWNLOAD_DATA_AVAILABLE
 * - EVENT_DOWNLOAD_CHANNEL_STATUS
 *
 * ## Main Features:
 * - Dynamic STK menu entry.
 * - Real-time reading of UICC data fields.
 * - Construction and sending of HTTP POST requests through BIP.
 * - JSON payload construction optimized for resource-limited environments.
 *
 * Author: Rafael Moreno Campos
 * Version: 1.0
 */
import javacard.framework.*;
import uicc.toolkit.*;

/**
 * HelloSTK3 Applet
 * <p>
 * This applet creates a basic SIM Toolkit (STK) menu entry and handles several events
 * such as menu selection, channel status, and data available notifications.
 * It also establishes a BIP (Bearer Independent Protocol) channel to send an HTTP POST request.
 * <p>
 * Note: This code follows JavaCard 3.0+ and UICC Toolkit API standards.
 */
public class HelloSTK3 extends Applet implements ToolkitInterface, ToolkitConstants {

    // ToolkitRegistry is used to manage the STK menu and events.
    public ToolkitRegistry toolkitRegistry;


    // Menu Item ID returned after registering the menu entry.
    private byte helloMenuItem;
    private byte iccidMenuItem;
    private byte imeiMenuItem;
    private byte mncmccMenuItem;
    private byte JSONMenuItem;


    // Static buffers for text displayed or used as debug messages.
    static byte[] menuItemText = new byte[]{'H', 'e', 'l', 'l', 'o', ',', ' ', 'S', 'T', 'K'};

    static byte[] iccidMenuItemText = new byte[]{'I', 'C', 'C', 'I', 'D'};
    static byte[] imeiMenuItemText = new byte[]{'I', 'M', 'E', 'I'};
    static byte[] mncmccdMenuItemText = new byte[]{'M', 'N', 'C', ' ', 'M','C','C'};
    static byte[] JSONMenuItemText = new byte[]{'J', 'S', 'O', 'N'};


    // Diagnostic utility for displaying debug messages.
    private DiagUtil diag;
    private STKHandler stkHandler;


    private byte[] tmpBuffer;


    /**
     * Applet constructor
     * Initializes buffers, registers menu entry, and sets event listeners.
     * <p>
     * Note: Buffers are transient (cleared on reset) to save EEPROM usage and enhance performance.
     */
    public HelloSTK3() {
        register(); // Register applet instance

        toolkitRegistry = ToolkitRegistrySystem.getEntry();
        helloMenuItem = toolkitRegistry.initMenuEntry(menuItemText, (short) 0, (short) menuItemText.length, (byte) 0, false,
                (byte) 0, (short) 0);
        iccidMenuItem = toolkitRegistry.initMenuEntry(iccidMenuItemText, (short) 0, (short) iccidMenuItemText.length, (byte) 0, false,
                (byte) 0, (short) 0);
        imeiMenuItem = toolkitRegistry.initMenuEntry(imeiMenuItemText, (short) 0, (short) imeiMenuItemText.length, (byte) 0, false,
                (byte) 0, (short) 0);
        mncmccMenuItem = toolkitRegistry.initMenuEntry(mncmccdMenuItemText, (short) 0, (short) mncmccdMenuItemText.length, (byte) 0, false,
                (byte) 0, (short) 0);
        JSONMenuItem = toolkitRegistry.initMenuEntry(JSONMenuItemText, (short) 0, (short) JSONMenuItemText.length, (byte) 0, false,
                (byte) 0, (short) 0);

        // Set events to be notified to this applet
        toolkitRegistry.setEvent(ToolkitConstants.EVENT_EVENT_DOWNLOAD_DATA_AVAILABLE);
        toolkitRegistry.setEvent(ToolkitConstants.EVENT_EVENT_DOWNLOAD_CHANNEL_STATUS);

        // Create transient byte arrays (cleared on card reset)

        tmpBuffer = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_RESET);

        diag = new DiagUtil(); // Initialize diagnostic utility

        stkHandler = new STKHandler(diag, tmpBuffer);


    }

    /**
     * JavaCard entry point for installation
     */
    public static void install(byte bArray[], short bOffset, byte bLength) {
        HelloSTK3 thisApplet = new HelloSTK3();
    }

    /**
     * Shareable interface method to allow access from other applets if needed
     */
    public Shareable getShareableInterfaceObject(AID aid, byte p) {
        if (aid == null && p == (byte) 1) {
            return this;
        }
        return null;
    }

    /**
     * Main APDU handler (not used for STK events)
     */
    public void process(APDU apdu) throws ISOException {
    }

    public void processToolkit(short event) {
        if (event == EVENT_MENU_SELECTION) {
            EnvelopeHandler eh = EnvelopeHandlerSystem.getTheHandler();
            byte selectedItemId =  eh.getItemIdentifier();

            if(selectedItemId == helloMenuItem ){
                stkHandler.eventMenuSelection();

            }else if(selectedItemId == iccidMenuItem){
                stkHandler.displayIccidOnMenuSelection();

            }else if(selectedItemId == mncmccMenuItem){
                stkHandler.displayMncMccOnMenuSelection();

            }else if(selectedItemId == JSONMenuItem){
                stkHandler.displayJSONOnMenuSelection();

            }else if(selectedItemId == imeiMenuItem){
                stkHandler.displayImeiOnMenuSelection();

            }
        }

        if (event == EVENT_EVENT_DOWNLOAD_DATA_AVAILABLE) {
            EnvelopeHandler eh = EnvelopeHandlerSystem.getTheHandler();
            stkHandler.processEventEventDownloadDataAvailable(eh);
        }

        if (event == EVENT_EVENT_DOWNLOAD_CHANNEL_STATUS) {
            EnvelopeHandler eh = EnvelopeHandlerSystem.getTheHandler();
            stkHandler.procesEventEventDownloadChannelStatus(eh);

        }

    }


}
