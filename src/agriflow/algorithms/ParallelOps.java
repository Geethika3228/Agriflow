package agriflow.algorithms;

/**
 * Parallel primitives with plain Threads: reduce, Blelloch prefix-sum scan, fork-join merge sort.
 * Also the theory numbers: work (T1), span (T-infinity), Brent's bound  T_p <= T1/p + T_inf.
 */
public final class ParallelOps {
    private ParallelOps() {}

    public interface Chunk { void run(int lo, int hi); }

    private static final int GRAIN = 20_000;   // below this, starting threads costs more than it saves

    /** split [0,total) into `threads` slices and run them at the same time */
    public static void parallelFor(int total, int threads, Chunk c) {
        int t = Math.min(threads, total);
        if (t <= 1 || total < GRAIN) { c.run(0, total); return; }
        Thread[] ts = new Thread[t];
        for (int i = 0; i < t; i++) {
            final int lo = (int) ((long) total * i / t), hi = (int) ((long) total * (i + 1) / t);
            ts[i] = new Thread(() -> c.run(lo, hi));
            ts[i].start();
        }
        for (Thread th : ts) join(th);
    }

    private static void join(Thread t) {
        try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ------------------------------------------------------------------ reduce
    public static long sumSeq(int[] a) {
        long s = 0;
        for (int x : a) s += x;
        return s;
    }

    public static long sumParallel(int[] a, int threads) {
        int t = Math.max(1, threads);
        long[] part = new long[t];
        if (t == 1 || a.length < GRAIN) return sumSeq(a);
        Thread[] ts = new Thread[t];
        for (int i = 0; i < t; i++) {
            final int id = i, lo = (int) ((long) a.length * i / t), hi = (int) ((long) a.length * (i + 1) / t);
            ts[i] = new Thread(() -> { long s = 0; for (int k = lo; k < hi; k++) s += a[k]; part[id] = s; });
            ts[i].start();
        }
        for (Thread th : ts) join(th);
        long total = 0;
        for (long p : part) total += p;
        return total;
    }

    // ------------------------------------------------------------------ scan (prefix sums)
    public static long[] scanSeq(int[] a) {
        long[] r = new long[a.length];
        long s = 0;
        for (int i = 0; i < a.length; i++) { s += a[i]; r[i] = s; }
        return r;
    }

    /** Blelloch scan: an up-sweep builds partial sums in a tree, a down-sweep fills in the prefixes. Every level is parallel. */
    public static long[] scanParallel(int[] a, int threads) {
        int n = a.length;
        int size = 1;
        while (size < n) size <<= 1;
        final long[] t = new long[size];
        for (int i = 0; i < n; i++) t[i] = a[i];
        for (int d = 1; d < size; d <<= 1) {
            final int dd = d, stride = 2 * d;
            parallelFor(size / stride, threads, (lo, hi) -> {
                for (int k = lo; k < hi; k++) { int i = k * stride; t[i + stride - 1] += t[i + dd - 1]; }
            });
        }
        t[size - 1] = 0;
        for (int d = size >> 1; d >= 1; d >>= 1) {
            final int dd = d, stride = 2 * d;
            parallelFor(size / stride, threads, (lo, hi) -> {
                for (int k = lo; k < hi; k++) {
                    int i = k * stride;
                    long tmp = t[i + dd - 1];
                    t[i + dd - 1] = t[i + stride - 1];
                    t[i + stride - 1] += tmp;
                }
            });
        }
        long[] r = new long[n];
        for (int i = 0; i < n; i++) r[i] = t[i] + a[i];     // exclusive -> inclusive
        return r;
    }

    // ------------------------------------------------------------------ merge sort
    public static void mergeSortSeq(int[] a) { sortRec(a, new int[a.length], 0, a.length, 0); }

    public static void mergeSortParallel(int[] a, int threads) {
        int depth = 0;
        while ((1 << depth) < threads) depth++;
        sortRec(a, new int[a.length], 0, a.length, depth);
    }

    private static void sortRec(int[] a, int[] buf, int lo, int hi, int forkDepth) {
        if (hi - lo < 2) return;
        int mid = (lo + hi) >>> 1;
        if (forkDepth > 0 && hi - lo > GRAIN) {
            Thread left = new Thread(() -> sortRec(a, buf, lo, mid, forkDepth - 1));
            left.start();
            sortRec(a, buf, mid, hi, forkDepth - 1);
            join(left);
        } else {
            sortRec(a, buf, lo, mid, 0);
            sortRec(a, buf, mid, hi, 0);
        }
        int i = lo, j = mid, o = lo;
        while (i < mid && j < hi) buf[o++] = a[j] < a[i] ? a[j++] : a[i++];
        while (i < mid) buf[o++] = a[i++];
        while (j < hi) buf[o++] = a[j++];
        System.arraycopy(buf, lo, a, lo, hi - lo);
    }

    // ------------------------------------------------------------------ theory: work and span
    public static int log2ceil(long n) { int k = 0; while ((1L << k) < n) k++; return k; }

    /** {work, span} for a tree reduction of n numbers */
    public static long[] reduceWorkSpan(long n) { return new long[]{n - 1, log2ceil(n)}; }

    /** {work, span} for the Blelloch scan of n numbers (padded to a power of two) */
    public static long[] scanWorkSpan(long n) {
        long size = 1L << log2ceil(n);
        return new long[]{2 * (size - 1) + n, 2L * log2ceil(size) + 1};
    }

    /** {work, span} for merge sort with a SEQUENTIAL merge: W(n)=2W(n/2)+n, S(n)=S(n/2)+n  =>  parallelism only ~log n */
    public static long[] sortWorkSpan(long n) {
        if (n <= 1) return new long[]{0, 0};
        long[] h = sortWorkSpan(n / 2), h2 = sortWorkSpan(n - n / 2);
        return new long[]{h[0] + h2[0] + n, Math.max(h[1], h2[1]) + n};
    }

    /** Brent's theorem: any greedy scheduler finishes in at most work/p + span steps */
    public static double brentBound(long work, long span, int p) { return (double) work / p + span; }
}
