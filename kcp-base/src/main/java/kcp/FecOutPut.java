package kcp;

import com.backblaze.erasure.IFecEncode;
import io.netty.buffer.ByteBuf;

/**
 * fec
 * Created by JinMiao
 * 2018/7/27.
 */
public class FecOutPut implements KcpOutput {

    protected final KcpOutput output;

    protected final IFecEncode fecEncode;

    protected FecOutPut(KcpOutput output, IFecEncode fecEncode) {
        this.output = output;
        this.fecEncode = fecEncode;
    }

    @Override
    public void out(ByteBuf msg, IKcp kcp) {
        ByteBuf[] byteBufArr = fecEncode.encode(msg);
        //  out之后会自动释放你内存
        output.out(msg, kcp);
        if (byteBufArr == null) {
            return;
        }
        for (ByteBuf parityByteBuf : byteBufArr) {
            output.out(parityByteBuf, kcp);
        }
    }
}
