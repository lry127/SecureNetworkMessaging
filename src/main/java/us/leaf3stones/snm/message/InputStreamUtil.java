package us.leaf3stones.snm.message;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamUtil {
    public static byte[] readNBytes(InputStream in, int n) throws NetIOException {

        byte[] buffer = new byte[n];
        int bytesRead = 0;
        while (bytesRead < n) {
            try {
                int readLen = in.read(buffer, bytesRead, n - bytesRead);
                if (readLen == -1) {
                    break;
                }
                bytesRead += readLen;
            } catch (IOException ioException) {
                throw new NetIOException(ioException, true);
            }
        }

        if (bytesRead < n) {
            throw new NetIOException("unable to fully fill required " + n + " bytes", false);
        }

        return buffer;
    }
}

