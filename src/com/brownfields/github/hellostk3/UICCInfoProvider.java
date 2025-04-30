package com.brownfields.github.hellostk3;

import javacard.framework.JCSystem;
import javacard.framework.UserException;
import javacard.framework.Util;
import uicc.access.FileView;
import uicc.access.UICCConstants;
import uicc.access.UICCSystem;
import uicc.toolkit.*;

import static uicc.toolkit.ToolkitConstants.*;

/**
 * UICCInfoProvider class
 * <p>
 * Provides access to SIM/UICC information such as ICCID, IMEI, MCC, and MNC.
 * Buffers are internally managed and data is lazily loaded upon request.
 */
public class UICCInfoProvider {

    // Main buffer containing all information
    private final byte[] fullBuffer;

    // Offsets and lengths for each field inside fullBuffer
    private final short[] fieldOffsets;
    private final short[] fieldLengths;

    // Buffers used for reading and storing UICC information
    private final byte[] readBuffer;
    private final byte[] tmpBuffer;

    // Private buffers for each extracted field
    private byte[] imeiCopyBuffer;
    private byte[] mccCopyBuffer;
    private byte[] mncCopyBuffer;
    private byte[] iccidCopyBuffer;

    private short iccidLength;


    // FileView allows access to UICC filesystem to read files like ICCID.
    private FileView uiccFileView;

    // Flags to track loaded data
    private boolean localInfoLoaded;
    private boolean imeiLoaded;
    private boolean iccidLoaded;

    // Index constants for fields
    private static final short INDEX_ICCID = 0;
    private static final short INDEX_IMEI = 1;
    private static final short INDEX_MCC = 2;
    private static final short INDEX_MNC = 3;

    private static byte[] INFO_ERROR_GENERAL = new byte[]{'E', 'R', 'R', 'O', 'R', '_', 'I', 'N', 'F'};
    private static byte[] INFO_ERROR_TOOLKIT_1 = new byte[]{'E', 'R', 'R', 'O', 'R', '_', 'I', 'N', 'F', '_', 'T', '1'};
    private static byte[] INFO_ERROR_1 = new byte[]{'E', 'R', 'R', 'O', 'R', '_', 'I', 'N', 'F', '_', 'E', '1'};
    private static byte[] INFO_ERROR_2 = new byte[]{'E', 'R', 'R', 'O', 'R', '_', 'I', 'N', 'F', '_', 'E', '2'};

    private DiagUtil diag;

    /**
     * Constructor
     *
     * @param tmpBuffer Shared temporary buffer
     */
    public UICCInfoProvider(byte[] tmpBuffer, DiagUtil diag) {
        uiccFileView = UICCSystem.getTheUICCView(JCSystem.NOT_A_TRANSIENT_OBJECT);

        readBuffer = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
        fullBuffer = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_RESET); // 64 bytes is enough
        fieldOffsets = JCSystem.makeTransientShortArray((short) 4, JCSystem.CLEAR_ON_RESET);
        fieldLengths = JCSystem.makeTransientShortArray((short) 4, JCSystem.CLEAR_ON_RESET);
        this.tmpBuffer = tmpBuffer;

        localInfoLoaded = false;
        imeiLoaded = false;
        iccidLoaded = false;

        this.diag = diag;
    }

    /**
     * Returns the full buffer containing all fields.
     */
    public byte[] getFullBuffer() throws UserException {
        ensureAllDataLoaded();
        return fullBuffer;
    }

    /**
     * Returns the array of field offsets.
     */
    public short[] getFullOffsets() throws UserException {
        ensureAllDataLoaded();
        return fieldOffsets;
    }

    /**
     * Returns the array of field lengths.
     */
    public short[] getFullLengths() throws UserException {
        ensureAllDataLoaded();
        return fieldLengths;
    }

    private void ensureAllDataLoaded() throws UserException {
        if (!iccidLoaded) {
            iccidLength = extractICCID(true);
            iccidLoaded = true;
        }
        if (!imeiLoaded) {
            fetchDeviceImei();
            imeiLoaded = true;
        }
        if (!localInfoLoaded) {
            retrieveNetworkIdentifiers();
            localInfoLoaded = true;
        }
    }

    /**
     * Returns the ICCID buffer.
     * If not loaded yet, loads the ICCID from the UICC.
     */
    public byte[] getIccidBuffer() {
        if (!iccidLoaded) {
            iccidLength = extractICCID(true);
            iccidLoaded = true;
        }

        if (iccidCopyBuffer == null) {
            short offset = fieldOffsets[INDEX_ICCID];
            short length = fieldLengths[INDEX_ICCID];
            iccidCopyBuffer = JCSystem.makeTransientByteArray(length, JCSystem.CLEAR_ON_RESET);
            Util.arrayCopyNonAtomic(fullBuffer, offset, iccidCopyBuffer, (short) 0, length);
        }

        return iccidCopyBuffer;

    }

    /**
     * Returns the length of the ICCID.
     * Ensures the ICCID is loaded before returning its length.
     */
    public short getIccidLength() {
        if (!iccidLoaded) {
            iccidLength = extractICCID(true);
            iccidLoaded = true;
        }
        return iccidLength;

    }

    /**
     * Returns the IMEI buffer.
     * If not loaded yet, retrieves the IMEI from the device.
     */
    public byte[] getImeiBuffer() throws UserException {
        if (!imeiLoaded) {
            fetchDeviceImei();
            imeiLoaded = true;
        }

        if (imeiCopyBuffer == null) {
            short offset = fieldOffsets[INDEX_IMEI];
            short length = fieldLengths[INDEX_IMEI];
            imeiCopyBuffer = JCSystem.makeTransientByteArray(length, JCSystem.CLEAR_ON_RESET);
            Util.arrayCopyNonAtomic(fullBuffer, offset, imeiCopyBuffer, (short) 0, length);
        }

        return imeiCopyBuffer;

    }

    /**
     * Returns the MCC (Mobile Country Code) buffer.
     * If not loaded yet, loads local information.
     */
    public byte[] getMccBuffer() {
        if (!localInfoLoaded) {
            retrieveNetworkIdentifiers();
            localInfoLoaded = true;
        }

        if (mccCopyBuffer == null) {
            short offset = fieldOffsets[INDEX_MCC];
            short length = fieldLengths[INDEX_MCC];
            mccCopyBuffer = JCSystem.makeTransientByteArray(length, JCSystem.CLEAR_ON_RESET);
            Util.arrayCopyNonAtomic(fullBuffer, offset, mccCopyBuffer, (short) 0, length);
        }

        return mccCopyBuffer;
    }

    /**
     * Returns the MNC (Mobile Network Code) buffer.
     * If not loaded yet, loads local information.
     */
    public byte[] getMncBuffer() {
        if (!localInfoLoaded) {
            retrieveNetworkIdentifiers();
            localInfoLoaded = true;
        }

        if (mncCopyBuffer == null) {
            short offset = fieldOffsets[INDEX_MNC];
            short length = fieldLengths[INDEX_MNC];
            mncCopyBuffer = JCSystem.makeTransientByteArray(length, JCSystem.CLEAR_ON_RESET);
            Util.arrayCopyNonAtomic(fullBuffer, offset, mncCopyBuffer, (short) 0, length);
        }

        return mncCopyBuffer;
    }

    /**
     * Loads local information from the device (MCC and MNC).
     */
    private void retrieveNetworkIdentifiers() {

        try {
            // Prepare and send the proactive command to get local info
            ProactiveHandler handler = ProactiveHandlerSystem.getTheHandler();
            handler.init(PRO_CMD_PROVIDE_LOCAL_INFORMATION, (byte) 0x00, DEV_ID_TERMINAL);
            handler.send();

            // Handle the response and extract location data if available
            ProactiveResponseHandler response = ProactiveResponseHandlerSystem.getTheHandler();
            if (response.findTLV((byte) TAG_LOCATION_INFORMATION, (byte) 0x01) != TLV_NOT_FOUND) {
                short dataLength = response.copyValue((short) 0, readBuffer, (short) 0, (short) 8);

                ByteUtil.nibbleSwap(readBuffer, (short) 0, dataLength);
                ByteUtil.bytesToHex(readBuffer, (short) 0, dataLength, tmpBuffer, (short) 0);

                short currentOffset = (short) (fieldOffsets[INDEX_MNC - 1] + fieldLengths[INDEX_MNC - 1]);
                fieldOffsets[INDEX_MCC] = currentOffset;
                fieldLengths[INDEX_MCC] = 3;
                Util.arrayCopyNonAtomic(tmpBuffer, (short) 0, fullBuffer, currentOffset, (short) 3);

                currentOffset += 3;
                fieldOffsets[INDEX_MNC] = currentOffset;
                fieldLengths[INDEX_MNC] = 2;
                Util.arrayCopyNonAtomic(tmpBuffer, (short) 4, fullBuffer, currentOffset, (short) 2);
            }

        } catch (ToolkitException ex) {
            diag.error(INFO_ERROR_TOOLKIT_1, ex.getReason());
        } catch (ArrayIndexOutOfBoundsException ex) {
            DiagUtil.text(INFO_ERROR_1);
        } catch (NullPointerException ex) {
            DiagUtil.text(INFO_ERROR_2);
        } catch (Exception ex) {
            DiagUtil.text(INFO_ERROR_GENERAL);
        }
    }

    /**
     * Loads the IMEI from the device and stores it in the buffer.
     */
    private void fetchDeviceImei() throws ToolkitException, UserException {
        // Initialize handlers for proactive command and response
        ProactiveHandler cmdHandler = ProactiveHandlerSystem.getTheHandler();
        ProactiveResponseHandler respHandler = ProactiveResponseHandlerSystem.getTheHandler();

        // Request IMEI from the terminal
        cmdHandler.init(PRO_CMD_PROVIDE_LOCAL_INFORMATION, (byte) 0x01, DEV_ID_TERMINAL);
        cmdHandler.send();

        // Check if the command was successfully executed
        if (respHandler.getGeneralResult() == RES_CMD_PERF) {
            short imeiLength = respHandler.findAndCopyValue(TAG_IMEI, readBuffer, (short) 0);

            // Convert IMEI format and calculate check digit
            ByteUtil.nibbleSwap(readBuffer, (short) 0, imeiLength);
            ByteUtil.bytesToHex(readBuffer, (short) 0, imeiLength, tmpBuffer, (short) 0);
            short check = ByteUtil.calcCheckDigitByLuhn(tmpBuffer, (short) 1, (short) 14);
            tmpBuffer[15] = (byte) (check + '0');

            // Store result in the final buffer
            short destOffset = (short) (fieldOffsets[INDEX_ICCID] + fieldLengths[INDEX_ICCID]);
            fieldOffsets[INDEX_IMEI] = destOffset;
            fieldLengths[INDEX_IMEI] = 15;
            Util.arrayCopyNonAtomic(tmpBuffer, (short) 1, fullBuffer, destOffset, (short) 15);
        } else {
            // Raise custom exception if the command failed
            UserException.throwIt((short) 0x7001);
        }
    }

    /**
     * Loads the ICCID from the UICC filesystem.
     *
     * @param trimTrailingF True to remove 'F' padding at the end
     * @return Length of the valid ICCID digits
     */
    private short extractICCID(boolean trimTrailingF) throws ToolkitException {
        try {
            short rawLength = 10;
            short charCount = 20;

            // Read ICCID data from the SIM file system
            readBinaryFromEF(uiccFileView, UICCConstants.FID_EF_ICCID, readBuffer, (short) 0, rawLength);
            ByteUtil.nibbleSwap(readBuffer, (short) 0, rawLength);
            ByteUtil.bytesToHex(readBuffer, (short) 0, rawLength, tmpBuffer, (short) 0);

            // Optionally strip padding characters from the end
            if (trimTrailingF) {
                while (tmpBuffer[(short)(charCount - 1)] == (byte) 'F') {
                    charCount--;
                }
            }

            // Store ICCID info in buffer structure
            fieldOffsets[INDEX_ICCID] = 0;
            fieldLengths[INDEX_ICCID] = charCount;
            Util.arrayCopyNonAtomic(tmpBuffer, (short) 0, fullBuffer, (short) 0, charCount);

            return charCount;
        } catch (Exception ex) {
            return 0;
        }

    }

    /**
     * Reads a binary file (EF) from the UICC.
     *
     * @param fileView   FileView instance to access files
     * @param FID        File Identifier (FID) of the EF
     * @param dstBuffer  Buffer to store the file contents
     * @param dstOffset  Offset where to start writing
     * @param readLength Number of bytes to read
     * @return Number of bytes read
     */
    private short readBinaryFromEF(FileView fileView, short FID, byte[] dstBuffer, short dstOffset, short readLength) {
        fileView.select(FID);
        return fileView.readBinary((short) 0, dstBuffer, (short) dstOffset, readLength);
    }
}
