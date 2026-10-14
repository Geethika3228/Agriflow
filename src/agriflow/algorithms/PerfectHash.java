package agriflow.algorithms;

/**
 * Universal hashing + FKS perfect hashing.
 * h(x) = ((a*x + b) mod p) mod m with RANDOM a, b. For any two different keys, the chance they collide is <= 1/m,
 * no matter which keys an adversary picks. FKS uses that twice to get a table with ZERO collisions and O(1) lookup.
 */
public final class PerfectHash {
    public static final long P = 2_147_483_647L;      // 2^31 - 1, a prime

    public long a1, b1;
    public int m;
    public int[] size2;
    public long[] a2, b2;
    public long[][] slots;           // key+1 stored in slot, 0 = empty
    public int attempts1, attempts2;  // how many random (a,b) draws were needed
    public int totalSpace;

    public static long keyOf(String s) {
        long h = 0;
        for (int i = 0; i < s.length(); i++) h = (h * 131 + s.charAt(i)) % P;
        return h;
    }

    public static int hash(long a, long b, long key, int m) { return (int) (((a * key + b) % P) % m); }

    /** number of colliding PAIRS when the keys go into m buckets */
    public static long collisions(long[] keys, int m, long a, long b) {
        int[] cnt = new int[m];
        for (long k : keys) cnt[hash(a, b, k, m)]++;
        long pairs = 0;
        for (int c : cnt) pairs += (long) c * (c - 1) / 2;
        return pairs;
    }

    /** a weak, fixed hash people really write: add up the characters. Anagrams collide. */
    public static int sumHash(String s, int m) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) h += s.charAt(i);
        return h % m;
    }

    public static PerfectHash build(long[] keys, Rng rng) {
        int n = keys.length;
        PerfectHash ph = new PerfectHash();
        ph.m = n;
        int[] bucketOf = new int[n], cnt = new int[n];
        while (true) {
            ph.attempts1++;
            ph.a1 = 1 + rng.nextLong(P - 1);
            ph.b1 = rng.nextLong(P);
            for (int i = 0; i < n; i++) cnt[i] = 0;
            for (int i = 0; i < n; i++) { bucketOf[i] = hash(ph.a1, ph.b1, keys[i], n); cnt[bucketOf[i]]++; }
            long sq = 0;
            for (int c : cnt) sq += (long) c * c;
            if (sq <= 4L * n) break;                      // expected sum of squares is < 2n, so this passes about half the time
        }
        ph.size2 = new int[n]; ph.a2 = new long[n]; ph.b2 = new long[n]; ph.slots = new long[n][];
        for (int b = 0; b < n; b++) {
            if (cnt[b] == 0) continue;
            int size = cnt[b] * cnt[b];
            ph.size2[b] = size;
            ph.totalSpace += size;
            long[] mine = new long[cnt[b]];
            int c = 0;
            for (int i = 0; i < n; i++) if (bucketOf[i] == b) mine[c++] = keys[i];
            while (true) {
                ph.attempts2++;
                ph.a2[b] = 1 + rng.nextLong(P - 1);
                ph.b2[b] = rng.nextLong(P);
                long[] slot = new long[size];
                boolean clash = false;
                for (long k : mine) {
                    int h = hash(ph.a2[b], ph.b2[b], k, size);
                    if (slot[h] != 0) { clash = true; break; }
                    slot[h] = k + 1;
                }
                if (!clash) { ph.slots[b] = slot; break; }
            }
        }
        return ph;
    }

    /** O(1) worst-case: exactly two hash evaluations and one comparison */
    public boolean contains(long key) {
        int b = hash(a1, b1, key, m);
        if (size2[b] == 0) return false;
        return slots[b][hash(a2[b], b2[b], key, size2[b])] == key + 1;
    }
}
