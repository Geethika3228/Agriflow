package agriflow.algorithms;

/**
 * Miller-Rabin primality test. A Monte Carlo algorithm: "composite" is always right, "prime" is right with
 * probability >= 1 - 4^(-rounds). With the fixed bases below it is exact for every 64-bit number.
 */
public final class MillerRabin {
    private MillerRabin() {}

    private static long addmod(long a, long b, long m) { return a >= m - b ? a - (m - b) : a + b; }

    /** (a * b) mod m without overflowing 64 bits (double-and-add) */
    public static long mulmod(long a, long b, long m) {
        long r = 0;
        a %= m; b %= m;
        while (b > 0) {
            if ((b & 1) == 1) r = addmod(r, a, m);
            a = addmod(a, a, m);
            b >>= 1;
        }
        return r;
    }

    public static long powmod(long b, long e, long m) {
        long r = 1 % m;
        b %= m;
        while (e > 0) {
            if ((e & 1) == 1) r = mulmod(r, b, m);
            b = mulmod(b, b, m);
            e >>= 1;
        }
        return r;
    }

    /** n - 1 = d * 2^s with d odd; returns s, and d via the Fermat helper below */
    public static int twoPower(long n) { return Long.numberOfTrailingZeros(n - 1); }

    /** true if base a PROVES n composite (n odd, n > 3) */
    public static boolean isWitness(long a, long n) {
        int s = twoPower(n);
        long d = (n - 1) >> s;
        long x = powmod(a, d, n);
        if (x == 1 || x == n - 1) return false;
        for (int r = 1; r < s; r++) {
            x = mulmod(x, x, n);
            if (x == n - 1) return false;
        }
        return true;
    }

    /** the older, weaker Fermat test: a^(n-1) == 1 (mod n). Carmichael numbers (561, 1105...) fool it. */
    public static boolean fermatPasses(long a, long n) { return powmod(a, n - 1, n) == 1; }

    private static final long[] BASES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};

    /** deterministic for all n < 2^64 */
    public static boolean isPrime(long n) {
        if (n < 2) return false;
        for (long p : BASES) {
            if (n == p) return true;
            if (n % p == 0) return false;
        }
        for (long a : BASES) if (isWitness(a, n)) return false;
        return true;
    }

    /** the randomised version: `rounds` random bases */
    public static boolean probablyPrime(long n, int rounds, Rng rng) {
        if (n < 2) return false;
        if (n < 4) return true;
        if ((n & 1) == 0) return false;
        for (int i = 0; i < rounds; i++) {
            long a = 2 + rng.nextLong(n - 3);
            if (isWitness(a, n)) return false;
        }
        return true;
    }

    /** a random prime in [lo, hi): keep guessing odd numbers (prime density ~ 1/ln n, so ~20 guesses at 1e9) */
    public static long randomPrime(long lo, long hi, Rng rng) {
        while (true) {
            long c = lo + rng.nextLong(hi - lo);
            c |= 1;
            if (c < hi && isPrime(c)) return c;
        }
    }

    /** primes by trial division - slow but obviously right, used by the self-check */
    public static boolean trialDivision(long n) {
        if (n < 2) return false;
        for (long d = 2; d * d <= n; d++) if (n % d == 0) return false;
        return true;
    }
}
