package us.leaf3stones.snm.message;

import java.io.IOException;

public class NetIOException extends IOException {
    public final boolean isAbnormalIOException;

    public NetIOException(String message, boolean isAbnormalIO) {
        super(message);
        this.isAbnormalIOException = isAbnormalIO;
    }

    public NetIOException(Exception e, boolean isAbnormalIO) {
        super(e);
        this.isAbnormalIOException = isAbnormalIO;
    }

    @Override
    public String toString() {
        return super.toString() + (isAbnormalIOException ? " [Abnormal Close]" : " [Clean Close]");
    }
}
