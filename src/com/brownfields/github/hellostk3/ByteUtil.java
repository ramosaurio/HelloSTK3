package com.brownfields.github.hellostk3;


/**
 * byteを操作するユーティリティ
 */
public class ByteUtil {
    private static final byte[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};


    public static short bytesToHex(byte[] inBuffer, short inOffset, short inLength, byte[] outBuffer, short outOffset) {
        short j = outOffset;
        for (short i = inOffset; i < (short) (inOffset + inLength); i++) {
            outBuffer[j++] = hex[(short) ((inBuffer[i] >> 4) & 0x0F)];
            outBuffer[j++] = hex[(short) (inBuffer[i] & 0x0F)];
        }
        return j;
    }

    /**
     * バイト配列の指定範囲要素の上位4bitと下位4bitを入れ替える
     *
     * @param buffer
     * @param offset
     * @param length
     */
    public static void nibbleSwap(byte[] buffer, short offset, short length) {
        byte v;
        for (short i = 0; i < length; i++) {
            v = buffer[(short) (offset + i)];
            buffer[(short) (offset + i)] = (byte) (((v >> 4) & 0x0f) | ((v & 0xf) << 4));
        }
    }

    /**
     * shortの数値をbyte配列の文字列に変換する
     *
     * @param num             対象のshort
     * @param outBuffer       出力先のbyte配列
     * @param outBufferOffset 出力先のオフセット
     * @return 出力先の先頭からの長さ
     */
    public static short numToCharArray(short num, byte[] outBuffer, short outBufferOffset) {
        if (num < 0) {
            num *= -1;
            outBuffer[outBufferOffset++] = '-';
        }

        short digit = 0;
        short target = num;
        while (target > 0) {
            digit++;
            target /= 10;
        }

        short index = (short) (digit - 1);
        while (num > 0) {
            outBuffer[(short) (outBufferOffset + index)] = (byte) ((num % (short) 10) + '0'); // shift
            num /= 10;
            index--;
        }
        return (short) (outBufferOffset + digit);
    }

    /**
     * BCD表現のbyte配列 (javacardx.framework.math.BCDUtil.convertToBCDで作成)を文字列に変換する
     *
     * @param bcdArray       a number in BCD format
     * @param bcdArrayLength length of bcdArray
     * @param outBuffer      文字列出力先のbyte配列
     * @param outOffset      出力先のoffset
     * @return 出力先の先頭からの長さ
     */
    public static short bcdToCharArray(byte[] bcdArray, short bcdArrayLength, byte[] outBuffer, short outOffset) {
        short j = outOffset;
        byte c;
        boolean skip_leading_zeroes = true;
        for (short i = 0; i < (short) (bcdArrayLength * 2); i++) {
            if ((i & 1) == 0) {
                c = hex[(short) ((bcdArray[(short) (i / 2)] >> 4) & 0x0F)];
            } else {
                c = hex[(short) (bcdArray[(short) (i / 2)] & 0x0F)];
            }
            if (!skip_leading_zeroes || (c != '0')) {
                outBuffer[j++] = c;
                skip_leading_zeroes = false;
            }
        }
        if (j == outOffset) {
            outBuffer[j++] = '0';
        }
        return j;
    }

    /**
     * LuhnアルゴリズムでIMEIのチェックデジットを計算する
     *
     * @param buffer 対象のbyte配列 (IMSI上位14桁のASCII文字列)
     * @param offset オフセット
     * @param length 長さ
     * @return チェックデジット
     */
    public static short calcCheckDigitByLuhn(byte[] buffer, short offset, short length) {
        short sum = 0;
        for (short i = offset; i < (short) (offset + length); i++) {
            short n = (short) (buffer[i] - '0');
            if (i % 2 == 0) {
                short t = (short) (n * 2);
                sum += (short) ((t / 10) + (t % 10));
            } else {
                sum += n;
            }
        }
        return (short) ((10 - (sum % 10)) % 10);
    }

}
