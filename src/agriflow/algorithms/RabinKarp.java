package agriflow.algorithms;

/**
 * Rabin-Karp with a polynomial rolling hash. Slide a window, update the hash in O(1),
 * and only compare real characters when the hashes match.
 * Double hashing (two moduli) makes false alarms basically impossible; we still verify.
 */
public final class RabinKarp {
    public static final long MOD1 = 1_000_000_007L, BASE1 = 911_382_323L;
    public static final long MOD2 = 998_244_353L, BASE2 = 972_663_749L;

    private RabinKarp() {}

    public static final class Result {
        public int[] positions;   // verified matches (or raw hash hits if verify == false)
        public int hashHits;      // windows whose hash equalled the pattern hash
        public int spurious;      // hash hits that were NOT real matches (collisions)
    }

    /** mod2 == 0 means single hash. Moduli must be below 2^31 so products fit in a long. */
    public static Result scan(String t, String p, long mod1, long base1, long mod2, long base2, boolean verify) {
        Result res = new Result();
        IntList out = new IntList();
        int n = t.length(), m = p.length();
        if (m == 0 || m > n) { res.positions = out.toArray(); return res; }
        boolean two = mod2 != 0;
        long hp1 = 0, ht1 = 0, pw1 = 1, hp2 = 0, ht2 = 0, pw2 = 1;
        for (int i = 0; i < m; i++) {
            hp1 = (hp1 * base1 + p.charAt(i)) % mod1;
            ht1 = (ht1 * base1 + t.charAt(i)) % mod1;
            if (two) {
                hp2 = (hp2 * base2 + p.charAt(i)) % mod2;
                ht2 = (ht2 * base2 + t.charAt(i)) % mod2;
            }
            if (i > 0) { pw1 = pw1 * base1 % mod1; if (two) pw2 = pw2 * base2 % mod2; }
        }
        for (int i = 0; ; i++) {
            if (ht1 == hp1 && (!two || ht2 == hp2)) {
                res.hashHits++;
                boolean real = equalsAt(t, p, i);
                if (!real) res.spurious++;
                if (!verify || real) out.add(i);
            }
            if (i + m >= n) break;
            ht1 = ((ht1 - t.charAt(i) * pw1 % mod1 + mod1) * base1 + t.charAt(i + m)) % mod1;
            if (two) ht2 = ((ht2 - t.charAt(i) * pw2 % mod2 + mod2) * base2 + t.charAt(i + m)) % mod2;
        }
        res.positions = out.toArray();
        return res;
    }

    public static int[] findAll(String t, String p) {
        return scan(t, p, MOD1, BASE1, MOD2, BASE2, true).positions;
    }

    /** Las Vegas version: random prime modulus + random base each run, answer is always verified. */
    public static int[] findAllRandom(String t, String p, Rng rng) {
        long mod = MillerRabin.randomPrime(1_000_000_000L, 2_000_000_000L, rng);
        long base = 256 + rng.nextLong(mod - 256);
        return scan(t, p, mod, base, 0, 0, true).positions;
    }

    private static boolean equalsAt(String t, String p, int i) {
        for (int j = 0; j < p.length(); j++) if (t.charAt(i + j) != p.charAt(j)) return false;
        return true;
    }
}
