package agriflow.algorithms;

/**
 * Quicksort, Las Vegas style: the answer is ALWAYS sorted, only the running time is random.
 * First-element pivot dies on already-sorted data (n^2/2 comparisons). A random pivot makes ~1.39 n log2 n on ANY input.
 */
public final class RandomizedSort {
    private RandomizedSort() {}

    /** sorts a in place, returns {comparisons, maxStackDepth} */
    public static long[] sort(int[] a, boolean randomPivot, Rng rng) {
        int n = a.length;
        long cmp = 0;
        int maxDepth = 0;
        if (n < 2) return new long[]{0, 0};
        int[] loS = new int[n + 1], hiS = new int[n + 1], dS = new int[n + 1];
        int sp = 0;
        loS[sp] = 0; hiS[sp] = n - 1; dS[sp] = 1; sp++;
        while (sp > 0) {
            sp--;
            int lo = loS[sp], hi = hiS[sp], d = dS[sp];
            if (d > maxDepth) maxDepth = d;
            if (lo >= hi) continue;
            int pi = randomPivot ? lo + rng.nextInt(hi - lo + 1) : lo;
            int t = a[pi]; a[pi] = a[hi]; a[hi] = t;       // pivot to the end (Lomuto)
            int pivot = a[hi], i = lo;
            for (int j = lo; j < hi; j++) {
                cmp++;
                if (a[j] < pivot) { int x = a[i]; a[i] = a[j]; a[j] = x; i++; }
            }
            t = a[i]; a[i] = a[hi]; a[hi] = t;
            loS[sp] = lo; hiS[sp] = i - 1; dS[sp] = d + 1; sp++;
            loS[sp] = i + 1; hiS[sp] = hi; dS[sp] = d + 1; sp++;
        }
        return new long[]{cmp, maxDepth};
    }

    public static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }
}
