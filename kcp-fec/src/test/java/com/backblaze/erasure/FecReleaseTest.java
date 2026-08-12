package com.backblaze.erasure;

import com.backblaze.erasure.fec.Fec;
import com.backblaze.erasure.fec.FecDecode;
import com.backblaze.erasure.fec.FecEncode;
import com.backblaze.erasure.fec.FecPacket;
import com.backblaze.erasure.fecNative.ReedSolomonNative;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Reference-count regression checks for FEC lifecycle cleanup.
 */
public class FecReleaseTest {

    public static void main(String[] args) {
        shouldReleasePendingEncodeShard();
        shouldKeepReturnedParityShardOwnedByCaller();
        shouldReleasePendingDecodePacket();
        shouldReleasePendingNativeEncodeShard();
        shouldReleasePendingNativeDecodePacket();
    }

    private static void shouldReleasePendingEncodeShard() {
        FecEncode encoder = new FecEncode(0, ReedSolomon.create(2, 1), 64);
        ByteBuf data = newFecDataPacket(0);

        encoder.encode(data);
        assertRefCnt(data, 2);

        encoder.release();
        assertRefCnt(data, 1);
        data.release();

        encoder.release();
    }

    private static void shouldKeepReturnedParityShardOwnedByCaller() {
        FecEncode encoder = new FecEncode(0, ReedSolomon.create(2, 1), 64);
        ByteBuf first = newFecDataPacket(0);
        ByteBuf second = newFecDataPacket(0);

        encoder.encode(first);
        ByteBuf[] parityShards = encoder.encode(second);
        ByteBuf parity = parityShards[0];

        encoder.release();
        assertRefCnt(parity, 1);

        first.release();
        second.release();
        parity.release();
    }

    private static void shouldReleasePendingDecodePacket() {
        FecDecode decoder = new FecDecode(3, ReedSolomon.create(2, 1), 64);
        ByteBuf packet = newFecDataPacket(Fec.typeData);
        FecPacket fecPacket = FecPacket.newFecPacket(packet);

        decoder.decode(fecPacket);
        assertRefCnt(packet, 2);

        decoder.release();
        assertRefCnt(packet, 1);
        packet.release();

        decoder.release();
    }

    private static void shouldReleasePendingNativeEncodeShard() {
        if (!ReedSolomonNative.isNativeSupport()) {
            return;
        }
        ReedSolomonNative codec = new ReedSolomonNative(2, 1);
        com.backblaze.erasure.fecNative.FecEncode encoder =
                new com.backblaze.erasure.fecNative.FecEncode(0, codec, 64);
        ByteBuf data = newFecDataPacket(0, true);

        encoder.encode(data);
        assertRefCnt(data, 2);

        encoder.release();
        assertRefCnt(data, 1);
        data.release();
    }

    private static void shouldReleasePendingNativeDecodePacket() {
        if (!ReedSolomonNative.isNativeSupport()) {
            return;
        }
        ReedSolomonNative codec = new ReedSolomonNative(2, 1);
        com.backblaze.erasure.fecNative.FecDecode decoder =
                new com.backblaze.erasure.fecNative.FecDecode(3, codec, 64);
        ByteBuf packet = newFecDataPacket(Fec.typeData, true);
        FecPacket fecPacket = FecPacket.newFecPacket(packet);

        decoder.decode(fecPacket);
        assertRefCnt(packet, 2);

        decoder.release();
        assertRefCnt(packet, 1);
        packet.release();
    }

    private static ByteBuf newFecDataPacket(int flag) {
        return newFecDataPacket(flag, false);
    }

    private static ByteBuf newFecDataPacket(int flag, boolean direct) {
        ByteBuf packet = direct ? Unpooled.directBuffer() : Unpooled.buffer();
        packet.writeIntLE(0);
        packet.writeShortLE(flag);
        packet.writeShort(0);
        return packet;
    }

    private static void assertRefCnt(ByteBuf byteBuf, int expected) {
        if (byteBuf.refCnt() != expected) {
            throw new AssertionError("Expected refCnt " + expected + " but was " + byteBuf.refCnt());
        }
    }
}
