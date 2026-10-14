package agriflow.algorithms;

/** xorshift64* random generator (hand-built, seedable, so demos are repeatable). */
public final class Rng {
    private long s;

    public Rng(long seed) { s = seed == 0 ? 0x9E3779B97F4A7C15L : seed; }

    public long nextLong() {
        s ^= s >>> 12;
        s ^= s << 25;
        s ^= s >>> 27;
        return s * 0x2545F4914F6CDD1DL;
    }

    /** uniform-ish in [0, bound), bound > 0 */
    public int nextInt(int bound) { return (int) ((nextLong() >>> 1) % bound); }

    /** uniform-ish in [0, bound), bound > 0 */
    public long nextLong(long bound) { return (nextLong() >>> 1) % bound; }

    public double nextDouble() { return (nextLong() >>> 11) * (1.0 / (1L << 53)); }
}
