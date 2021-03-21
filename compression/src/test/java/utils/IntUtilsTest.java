package utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IntUtilsTest {


    @Test
    void testBitPrefix() {
        int i = 0xFFFF_FFFF;

        Assertions.assertEquals(0xFF000000, IntUtils.bitPrefix(i, 8));
        Assertions.assertEquals(0x80000000, IntUtils.bitPrefix(i, 1));
        Assertions.assertEquals(i, IntUtils.bitPrefix(i, 32));
    }
}
