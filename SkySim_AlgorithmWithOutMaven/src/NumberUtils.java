

import java.util.Random;

public class NumberUtils {

    public static void main(String[] args) {
	int d = 4;
	int length = 90;

	Random r = new Random();
	float[] p = new float[d];
	double[] pShifted = new double[d];
	for (int i = 0; i < d; i++) {
	    p[i] = r.nextFloat();
	    pShifted[i] = p[i];
	}

	long[] z = unsignedZAddress(p, length);

	for (int k = 0; k < z.length; k++) {
	    System.out.println(NumberUtils.binaryString(z[k], Long.SIZE));
	}

	for (int j = 0; j < length; j++) {
	    long mask = 1l << (Long.SIZE - 1 - (j % Long.SIZE));
	    int bitpos = j % Long.SIZE;
	    int i = j % d;
	    int k = j / Long.SIZE;
	    long bit = (z[k] & mask) >>> (Long.SIZE - 1 - bitpos);
	    System.out.format("pShifted[%d] = %f,  bit = %d%n", i, pShifted[i], bit);
	    if (pShifted[i] > 0.5) {
		pShifted[i] -= 0.5;
	    }
	    pShifted[i] *= 2;
	}

    }

    // converts a float value of range [0, 1] to an unsigned int value
    // by retaining only its fractional part in binary representation (23 bits)
    public static int unsignedFloatToIntZ(float f) {
	// According to the IEEE standard, f is represented as
	//   (-1)^sign * 2^(exponent) * 1.(fraction)_2
	//     bit 1: sign
	//     bits 2 to 9: exponent + 127
	//     bits 10 to 32 : fraction
	//   --> Since f is between 0 and 1, we know that sign == 0.
	//   --> We also know that the exponent of (f + 1) is 0.
	//   ==> Therefore, the bits 1 to 9 are: 001111111
	return Float.floatToRawIntBits(1 + f) << 9;
	// The returned int value has the following structure:
	// Bits 1 to 23: f's fractional part
	// Bits 24 to 32: all equals 0
    }

    // interleaves the ints contained in @x,
    // thus returning @x's Z address (aka Morton address)
    public static long unsignedInterleave(int[] x) {
	int d = x.length;
	long z = 0;
	long maskZ = 1l << (Long.SIZE - 1);
	int i = 0;
	int maskXi = 1 << (Integer.SIZE - 1);
	while (maskZ != 0) {
	    if ((x[i] & maskXi) != 0) {
		z |= maskZ;
	    }
	    i++;
	    if (i == d) {
		i = 0;
		maskXi >>>= 1;
	    }
	    maskZ >>>= 1;
	}
	return z;
    }

    // Starts interleaving at bit @startBit (counting from 0).
    public static long unsignedInterleaveStartingAt(int[] x, int startBit) {
	int d = x.length;
	long z = 0;
	long maskZ = 1l << (Long.SIZE - 1);
	
	// Skip startBit / d full iterations.
	int skipIters = startBit / d;
	int maskXi = 1 << (Integer.SIZE - 1 - skipIters);

	// Skip startBit % d dimensions.
	int skipDims = startBit % d;
	int i = skipDims;
	
	while (maskZ != 0) {
	    if ((x[i] & maskXi) != 0) {
		z |= maskZ;
	    }
	    i++;
	    if (i == d) {
		i = 0;
		maskXi >>>= 1;
	    }
	    maskZ >>>= 1;
	}
	return z;
    }

    /*
     * Interleaving continues until @length bits have been generated.
     */
    public static long[] unsignedInterleave(int[] x, int length) {
	int d = x.length;

	int numLongs = DIV_ROUND_UP(length, Long.SIZE);
	long[] z = new long[numLongs];

	int i = 0;
	int maskXi = 1 << (Integer.SIZE - 1);
	for (int k = 0; k < numLongs; k++) {
	    long maskZ = 1l << (Long.SIZE - 1);
	    while (maskZ != 0) {
		if ((x[i] & maskXi) != 0) {
		    z[k] |= maskZ;
		}
		i++;
		if (i == d) {
		    i = 0;
		    maskXi >>>= 1;
		}
		maskZ >>>= 1;
	    }
	}
	return z;
    }
	
    public static long unsignedZAddress(float[] p) {
	int d = p.length;
	int[] x = new int[d];
	for (int i = d - 1; i >= 0; i--) {
	    x[i] = unsignedFloatToIntZ(p[i]);
	}
	return unsignedInterleave(x);
    }

    /*
     * Generate Z address, starting at bit @startBit (counting from 0).
     */
    public static long unsignedZAddressStartingAt(float[] p, int startBit) {
	int d = p.length;
	int[] x = new int[d];
	for (int i = d - 1; i >= 0; i--) {
	    x[i] = unsignedFloatToIntZ(p[i]);
	}
	return unsignedInterleaveStartingAt(x, startBit);
    }

    /*
     * Generate Z address, starting at bit @startBit (counting from 0).
     */
    public static long unsignedZAddressStartingAt(float[] data, int posStartData, int d, int startBit) {
	int[] x = new int[d];
	for (int i = d - 1; i >= 0; i--) {
	    x[i] = unsignedFloatToIntZ(data[posStartData + i]);
	}
	return unsignedInterleaveStartingAt(x, startBit);
    }

    /*
     * Returns a z address of length @zLength, which might be more
     * than Long.SIZE.
     */
    public static long[] unsignedZAddress(float[] p, int zLength) {
	int d = p.length;
	int[] x = new int[d];
	for (int i = d - 1; i >= 0; i--) {
	    x[i] = unsignedFloatToIntZ(p[i]);
	}
	return unsignedInterleave(x, zLength);
    }

    /*
     * Returns a z address of length @zLength, which might be more
     * than Long.SIZE.
     */
    public static long[] unsignedZAddress(float[] data, int posStart, int d, int zLength) {
	int[] x = new int[d];
	for (int i = 0, pos = posStart; i < d; i++, pos++) {
	    x[i] = unsignedFloatToIntZ(data[pos]);
	}
	return unsignedInterleave(x, zLength);
    }

    public static long unsignedZAddress(float[] data, int posStart, int d) {
	int[] x = new int[d];
	for (int i = 0, pos = posStart; i < d; i++, pos++) {
	    x[i] = unsignedFloatToIntZ(data[pos]);
	}
	return unsignedInterleave(x);
    }

    // Finds the lengths of the longest common binary prefix of two unsigned longs
    public static int unsignedLongestCommonPrefix(long z1, long z2) {
	return Long.numberOfLeadingZeros(z1 ^ z2);
    }

    // unsigned > relation
    public static boolean UNSIGNED_GT(long a, long b) {
	return (a > b) ^ (a < 0) ^ (b < 0);
    }

    // unsigned >= relation
    public static boolean UNSIGNED_GTEQ(long a, long b) {
	return (a >= b) ^ (a < 0) ^ (b < 0);
    }

    public static boolean EQ_VEC(long[] a, int posAStart, long[] b, int posBStart, int blocksize) {
	for (int i = 0, posA = posAStart, posB = posBStart; i < blocksize; i++, posA++, posB++) {
	    if (a[posA] != b[posB]) {
		return false;
	    }
	}
	return true;
    }

    // unsigned > relation
    public static boolean UNSIGNED_GT_VEC(long[] a, int posAStart, long[] b, int posBStart, int blocksize) {
	for (int i = 0, posA = posAStart, posB = posBStart; i < blocksize; i++, posA++, posB++) {
	    if (UNSIGNED_GT(a[posA], b[posB])) {
		return true;
	    } else if (a[posA] != b[posB]) {
		// a[posA] is unsigned smaller than b[posB].
		return false;
	    }
	}
	// Equality is no enough.
	return false;
    }

    // unsigned >= relation
    public static boolean UNSIGNED_GTEQ_VEC(long[] a, int posAStart, long[] b, int posBStart, int blocksize) {
	for (int i = 0, posA = posAStart, posB = posBStart; i < blocksize; i++, posA++, posB++) {
	    if (UNSIGNED_GT(a[posA], b[posB])) {
		return true;
	    } else if (a[posA] != b[posB]) {
		// a[posA] is unsigned smaller than b[posB].
		return false;
	    }
	}
	// Equality is enough.
	return true;
    }

    // returns (int)ceil((float)a / (float)b)
    public static int DIV_ROUND_UP(int a, int b) {
	return (a + b - 1) / b;
    }

    // Returns the largest multiple of x that is less than or equal to y.
    public static int largestMultipleOfXLeqY(int x, int y) {
	return (y / x) * x;
    }

    public static String binaryString(long x, int start) {
	String result = Long.toBinaryString(x);
	int numLeadingZeros = Long.SIZE - result.length();
	if (numLeadingZeros > 0) {
	    result = StringUtils.repeat('0', numLeadingZeros) + result;
	}
	return result.substring(0, start);
    }

    public static String binaryString(long x, int start, int length) {
	String result = Long.toBinaryString(x);
	int numLeadingZeros = Long.SIZE - result.length();
	if (numLeadingZeros > 0) {
	    result = StringUtils.repeat('0', numLeadingZeros) + result;
	}
	return result.substring(start, start + length);
    }

    public static String binaryString(int x, int start) {
	String result = Integer.toBinaryString(x);
	int numLeadingZeros = Integer.SIZE - result.length();
	if (numLeadingZeros > 0) {
	    result = StringUtils.repeat('0', numLeadingZeros) + result;
	}
	return result.substring(0, start);
    }

    public static String binaryString(int x, int start, int length) {
	String result = Integer.toBinaryString(x);
	int numLeadingZeros = Integer.SIZE - result.length();
	if (numLeadingZeros > 0) {
	    result = StringUtils.repeat('0', numLeadingZeros) + result;
	}
	return result.substring(start, start + length);
    }

    public static String arrayToString(float[] p, int numPlaces) {
	int d = p.length;
	String result = "[";
	for (int i = 0; i < d; i++) {
	    result += String.format("%." + numPlaces + "f", p[i]);
	    if (i != d - 1) {
		result += ", ";
	    }
	}
	result += "]";
	return result;
    }

    /*
     * Returns (a + inc) % base.
     * Requires 0 <= inc <= base and a < base.
     */
    public static int fastCyclicIncrement(int a, int inc, int base) {
	a = a + inc;
	if (a >= base) {
	    a -= base;
	}
	return a;
    }
}
