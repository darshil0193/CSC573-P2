class P2_Utils {
    static StringBuilder getCheckSum(StringBuilder data) {
        String hex;
        int i, j, checksum = 0;
        for (i = 0; i < data.length() - 2; i = i + 2) {
            j = (int) (data.charAt(i));
            hex = Integer.toHexString(j);
            j = (int) (data.charAt(i + 1));
            hex = hex + Integer.toHexString(j);
            j = Integer.parseInt(hex, 16);
            checksum += j;
        }

        if (data.length() % 2 == 0) {
            j = (int) (data.charAt(i));
            hex = Integer.toHexString(j);
            j = (int) (data.charAt(i + 1));
            hex = hex + Integer.toHexString(j);
            j = Integer.parseInt(hex, 16);
        } else {
            j = (int) (data.charAt(i));
            hex = "00" + Integer.toHexString(j);
            j = Integer.parseInt(hex, 16);
        }
        checksum += j;
        hex = Integer.toHexString(checksum);
        if (hex.length() > 4) {
            int carry = Integer.parseInt(("" + hex.charAt(0)), 16);
            hex = hex.substring(1, 5);
            checksum = Integer.parseInt(hex, 16);
            checksum += carry;
        }

        StringBuilder binCheckSum = new StringBuilder(Integer.toBinaryString(Integer.parseInt("FFFF", 16) - checksum));
        for(i=binCheckSum.length(); i<16; ++i) {
            binCheckSum.insert(0, "0");
        }

        return binCheckSum;
    }

    static StringBuilder data(byte[] a)
    {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0)
        {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }
}
