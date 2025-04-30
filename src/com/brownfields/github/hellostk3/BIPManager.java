package com.brownfields.github.hellostk3;

import javacard.framework.JCSystem;
import javacard.framework.UserException;
import javacard.framework.Util;
import uicc.toolkit.*;

import static uicc.toolkit.ToolkitConstants.*;
/**
 * BIPManager class
 *
 * Handles Bearer Independent Protocol (BIP) operations such as opening channels,
 * sending HTTP POST requests, managing responses, and handling channel events.
 */
public class BIPManager {

    // Buffers for managing BIP state and HTTP communication
    private byte[] appStateBuffer;
    private byte[] httpHeaderBuffer;
    private byte[] tmpBuffer; // Shared temporary buffer for processing
    private byte[] bcdBuffer; // Buffer for numeric conversions (e.g., port number)

    private DiagUtil diag;

    // Index in appStateBuffer to store HTTP BIP channel identifier
    private static final short httpBIPChannelIndex = 0;

    // Bearer types
    private static final byte udpTag = 0x01;
    private static final byte tcpTag = 0x02;

    // Diagnostic error messages
    private static byte[] BIP_ERROR_GENERAL = new byte[]{'E', 'R', 'R', 'O', 'R', '_', 'B', 'I', 'P'};
    private static byte[] BIP_ERROR_TOOLKIT_1 = new byte[]{'E', 'R', 'R', 'O', 'R', '_', 'B', 'I', 'P', '_', 'T', '1'};
    private static byte[] BIP_ERROR_TOOLKIT_2 = new byte[]{'E', 'R', 'R', 'O', 'R', '_', 'B', 'I', 'P', '_', 'T', '2'};
    private static byte[] BIP_ERROR_USER_1 = new byte[]{'E', 'R', 'R', 'O', 'R', '_', 'B', 'I', 'P', '_', 'U', '1'};
    private static byte[] BIP_ERROR_USER_2 = new byte[]{'E', 'R', 'R', 'O', 'R', '_', 'B', 'I', 'P', '_', 'U', '2'};

    /**
     * Constructor
     *
     * @param diag Diagnostic utility for logging errors
     * @param tmpBuffer Shared temporary buffer
     */
    public BIPManager(DiagUtil diag, byte[] tmpBuffer) {
        appStateBuffer = JCSystem.makeTransientByteArray((short) 16, JCSystem.CLEAR_ON_RESET);
        httpHeaderBuffer = JCSystem.makeTransientByteArray((short) 320, JCSystem.CLEAR_ON_RESET);
        this.tmpBuffer = tmpBuffer;
        bcdBuffer = JCSystem.makeTransientByteArray((short) 10, JCSystem.CLEAR_ON_RESET);
        this.diag = diag;

    }

    /**
     * Opens a BIP channel to the specified address and port.
     *
     * @param udp True if UDP is used, false for TCP
     * @param addr Destination IP address
     * @param port Destination port number
     * @return Channel ID
     */
    private byte openChannel(boolean udp, byte[] addr, short port) throws UserException, ToolkitException {

        ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();
        ProactiveResponseHandler rh = ProactiveResponseHandlerSystem.getTheHandler();

        ph.init(ToolkitConstants.PRO_CMD_OPEN_CHANNEL, (byte) 0x03, ToolkitConstants.DEV_ID_TERMINAL);
        ph.appendTLV((byte) (ToolkitConstants.TAG_BEARER_DESCRIPTION | ToolkitConstants.TAG_SET_CR), (byte) 0x03);
        ph.appendTLV((byte) (ToolkitConstants.TAG_BUFFER_SIZE | ToolkitConstants.TAG_SET_CR), (short) 0x05DC);
        ph.appendTLV((byte) (ToolkitConstants.TAG_UICC_TERMINAL_TRANSPORT_LEVEL | ToolkitConstants.TAG_SET_CR), udp ? udpTag : tcpTag, port);
        ph.appendTLV((byte) (ToolkitConstants.TAG_OTHER_DATA_DESTINATION_ADDRESS | ToolkitConstants.TAG_SET_CR), (byte) 0x21, addr, (short) 0, (short) 4);


        byte openResult = ph.send();
        byte channelId = 0;
        if (openResult == RES_CMD_PERF) {
            channelId = rh.getChannelIdentifier();
        } else {
            UserException.throwIt((short) openResult);
        }
        return channelId;

    }


    /**
     * Closes a given BIP channel.
     *
     * @param bipChannelId Channel identifier to close
     */
    private void closeChannel(byte bipChannelId) {
        if (bipChannelId != 0) {
            ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();
            ph.initCloseChannel(bipChannelId);
            ph.send();
        }
    }

    /**
     * Sends data over an open BIP channel.
     *
     * @param bipChannelId Channel identifier
     * @param buffer Data to send
     * @param length Length of the data
     * @return Result code
     */
    private byte sendData(byte bipChannelId, byte[] buffer, short length) throws ToolkitException, UserException {
        ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();

        short chunkSize = (short) 0xA0;
        byte result = RES_CMD_PERF;
        short position = 0;

        while (position < length) {
            ph.init(ToolkitConstants.PRO_CMD_SEND_DATA, (byte) 0x01, (byte) (DEV_ID_CHANNEL_BASE + bipChannelId));

            short remain = (short) (length - position);
            short append;
            if (remain > chunkSize) {
                ph.appendTLV(TAG_CHANNEL_DATA, buffer, position, (short) chunkSize);
                append = chunkSize;
            } else {
                ph.appendTLV(TAG_CHANNEL_DATA, buffer, position, remain);
                append = remain;
            }
            result = ph.send();
            if (result == RES_CMD_PERF) {
                position += append;
            } else {
                closeChannel(bipChannelId);
                UserException.throwIt((short) 0x7003);
            }
        }

        return result;
    }

    // Static HTTP header components
    private static final byte[] postHeader = {'P', 'O', 'S', 'T', ' '};
    private static final byte[] httpVersionHeader = {' ', 'H', 'T', 'T', 'P', '/', '1', '.', '1'};
    private static final byte[] hostHeader = {'H', 'o', 's', 't', ':', ' '};
    private static final byte[] connectionHeader = {'C', 'o', 'n', 'n', 'e', 'c', 't', 'i', 'o', 'n', ':', ' ', 'c', 'l', 'o', 's', 'e'};
    private static final byte[] contentTypeHeader = {'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'T', 'y', 'p', 'e', ':', ' ', 'a', 'p', 'p', 'l', 'i', 'c', 'a', 't', 'i', 'o', 'n', '/', 'j', 's', 'o', 'n'};
    private static final byte[] contentLengthHeaderPrefix = {'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'L', 'e', 'n', 'g', 't', 'h', ':', ' '};
    private static final byte[] userAgentHeader = {'U', 's', 'e', 'r', '-', 'A', 'g', 'e', 'n', 't', ':', ' ', 'A', 'p', 'p', 'l', 'e', 't', '/', '0', '.', '9'};
    private static final byte[] newLineHeader = {'\r', '\n'};

    /**
     * Creates the HTTP POST header.
     *
     * @param method HTTP method (POST)
     * @param addr Destination address
     * @param host Host header
     * @param port Port number
     * @param path API path
     * @param pathLength Length of API path
     * @param bodyLength Length of the body
     * @return Total header length
     */
    private short createHttpHeader(byte[] method, byte[] addr, byte[] host, short port,
                                   byte[] path, short pathLength, short bodyLength
    ) {

        short sendBufferOffset = 0;
        sendBufferOffset = Util.arrayCopy(method, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) method.length);
        sendBufferOffset = Util.arrayCopy(path, (short) 0, httpHeaderBuffer, sendBufferOffset, pathLength);
        sendBufferOffset = Util.arrayCopy(httpVersionHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) httpVersionHeader.length);
        sendBufferOffset = Util.arrayCopy(newLineHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) newLineHeader.length);

        sendBufferOffset = Util.arrayCopy(hostHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) hostHeader.length);
        if (host == null) {
            for (short i = 0; i < (short) addr.length; i++) {
                sendBufferOffset = ByteUtil.numToCharArray((short) (addr[i] & (short) 0xFF), httpHeaderBuffer, sendBufferOffset);
                httpHeaderBuffer[sendBufferOffset++] = '.';
            }
            sendBufferOffset--;
        } else {
            sendBufferOffset = Util.arrayCopy(host, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) host.length);
        }

        if (port != 80) {
            httpHeaderBuffer[sendBufferOffset++] = ':';

            bcdBuffer[0] = (byte) 0;
            Util.setShort(bcdBuffer, (short) 1, port);
            short bcdBytes = (short) 0x0;
            sendBufferOffset = ByteUtil.bcdToCharArray(bcdBuffer, bcdBytes, httpHeaderBuffer, sendBufferOffset);
        }
        sendBufferOffset = Util.arrayCopy(newLineHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) newLineHeader.length);

        sendBufferOffset = Util.arrayCopy(connectionHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) connectionHeader.length);
        sendBufferOffset = Util.arrayCopy(newLineHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) newLineHeader.length);

        sendBufferOffset = Util.arrayCopy(contentTypeHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) contentTypeHeader.length);
        sendBufferOffset = Util.arrayCopy(newLineHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) newLineHeader.length);

        if (bodyLength > 0) {
            sendBufferOffset = Util.arrayCopy(contentLengthHeaderPrefix, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) contentLengthHeaderPrefix.length);
            sendBufferOffset = ByteUtil.numToCharArray(bodyLength, httpHeaderBuffer, sendBufferOffset);
            sendBufferOffset = Util.arrayCopy(newLineHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) newLineHeader.length);
        }

        sendBufferOffset = Util.arrayCopy(userAgentHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) userAgentHeader.length);
        sendBufferOffset = Util.arrayCopy(newLineHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) newLineHeader.length);

        sendBufferOffset = Util.arrayCopy(newLineHeader, (short) 0, httpHeaderBuffer, sendBufferOffset, (short) newLineHeader.length);

        return sendBufferOffset;

    }

    /**
     * Processes the HTTP response data.
     *
     * @param channelId Channel identifier
     * @param length Length of the response data
     * @param dstBuffer Buffer to store the response
     * @param dstOffset Offset in destination buffer
     * @param dstBufferSize Destination buffer size
     */
    private void processHTTPResponse(byte channelId, short length, byte[] dstBuffer, short dstOffset, short dstBufferSize) {
        short copied;
        short readLength;
        final short MAX_READ_SIZE = (short) 0xA0; // 0xA0バイトずつ受信する。
        ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();
        ProactiveResponseHandler rh = ProactiveResponseHandlerSystem.getTheHandler();

        while (length > 0) {
            if (length > MAX_READ_SIZE) {
                readLength = MAX_READ_SIZE;
            } else {
                readLength = (short) (length & 0xff);
            }

            ph.init(PRO_CMD_RECEIVE_DATA, (byte) 0x00, (byte) (DEV_ID_CHANNEL_BASE + channelId));
            ph.appendTLV(TAG_CHANNEL_DATA_LENGTH, (byte) readLength);
            ph.send();
            byte res = rh.getGeneralResult();
            if (res != RES_CMD_PERF) {
                break;
            } else {
                if ((short) (dstOffset + readLength) <= dstBufferSize) {
                    dstOffset = rh.findAndCopyValue(TAG_CHANNEL_DATA, dstBuffer, dstOffset);
                } else {
                }
                copied = rh.findAndCopyValue(TAG_CHANNEL_DATA_LENGTH, tmpBuffer, (short) 0);
                length = (short) (tmpBuffer[0] & 0xff);
            }
        }
    }

    /**
     * Sends an HTTP POST request over BIP.
     */
    public void sendHTTPPost(byte[] httpBodyBuffer, short contentLength,
                             byte[] serverAddr, short serverPort,
                             byte[] apiPath, byte[] hostName) throws UserException, ToolkitException {


        try {
            short headerLength = createHttpHeader(postHeader, serverAddr, hostName, serverPort, apiPath, (short) apiPath.length, contentLength);
            byte bipChannelId = openChannel(false, serverAddr, serverPort);
            appStateBuffer[httpBIPChannelIndex] = bipChannelId;
            if (bipChannelId > 0) {
                sendData(bipChannelId, httpHeaderBuffer, headerLength);
                sendData(bipChannelId, httpBodyBuffer, contentLength);
            }
        } catch (ToolkitException e) {
            if (e.getReason() >= 0x7000) {
                diag.error(BIP_ERROR_TOOLKIT_1, e.getReason());
            } else {
                diag.error(BIP_ERROR_TOOLKIT_2, e.getReason());
            }
        } catch (UserException e) {
            if (e.getReason() >= 0x7000) {
                diag.error(BIP_ERROR_USER_1, e.getReason());
            } else {
                diag.error(BIP_ERROR_USER_2, e.getReason());
            }
        } catch (Exception e) {
            DiagUtil.text(BIP_ERROR_GENERAL);

        }


    }

    /**
     * Handles EVENT_DOWNLOAD_DATA_AVAILABLE to process incoming data.
     */
    public void processEventEventDownloadDataAvailable(EnvelopeHandler eh) {
        byte channelId = eh.getChannelIdentifier();
        eh.findAndCopyValue(TAG_CHANNEL_DATA_LENGTH, tmpBuffer, (short) 0);
        short length = (short) (tmpBuffer[0] & 0xff);

        if (channelId == appStateBuffer[httpBIPChannelIndex]) {
            processHTTPResponse(channelId, length, httpHeaderBuffer, (short) 0, (short) httpHeaderBuffer.length);
            closeChannel(channelId);
            DiagUtil.text(httpHeaderBuffer, (short) 0, (short) 32);
        }
    }

    /**
     * Handles EVENT_DOWNLOAD_CHANNEL_STATUS to detect channel closures.
     */
    public void procesEventEventDownloadChannelStatus(EnvelopeHandler eh) {
        byte channelId = eh.getChannelIdentifier();
        short channelStatus = eh.getChannelStatus(channelId);
        if ((channelStatus & (short) 0x8000) == 0) {
            if (channelId == appStateBuffer[httpBIPChannelIndex]) {
                appStateBuffer[httpBIPChannelIndex] = 0;
                closeChannel(channelId);
                diag.error(BIP_ERROR_GENERAL, channelStatus);
            }
        }
    }

}
