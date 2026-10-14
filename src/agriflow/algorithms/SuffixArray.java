package agriflow.algorithms;

/**
 * Suffix array by prefix doubling: sort suffixes by their first 1, 2, 4, 8... characters.
 * Each round is a hand-written merge sort, so total practical cost is O(n log^2 n).
 * (SA-IS gets O(n) but is much longer; this is the version the syllabus asks you to implement.)
 */
public final class SuffixArray {
    private SuffixArray() {}

    public static int[] build(String s) {
        int n = s.length();
        int[] sa = new int[n], rank = new int[n], tmp = new int[n], buf = new int[n];
        for (int i = 0; i < n; i++) { sa[i] = i; rank[i] = s.charAt(i); }
        for (int k = 1; n > 1; k <<= 1) {
            mergeSort(sa, buf, 0, n, rank, k, n);
            tmp[sa[0]] = 0;
            for (int i = 1; i < n; i++)
                tmp[sa[i]] = tmp[sa[i - 1]] + (less(sa[i - 1], sa[i], rank, k, n) ? 1 : 0);
            System.arraycopy(tmp, 0, rank, 0, n);
            if (rank[sa[n - 1]] == n - 1) break;
        }
        return sa;
    }

    private static int second(int[] rank, int i, int k, int n) { return i + k < n ? rank[i + k] : -1; }

    private static boolean less(int a, int b, int[] rank, int k, int n) {
        if (rank[a] != rank[b]) return rank[a] < rank[b];
        return second(rank, a, k, n) < second(rank, b, k, n);
    }

    private static void mergeSort(int[] a, int[] buf, int lo, int hi, int[] rank, int k, int n) {
        if (hi - lo < 2) return;
        int mid = (lo + hi) >>> 1;
        mergeSort(a, buf, lo, mid, rank, k, n);
        mergeSort(a, buf, mid, hi, rank, k, n);
        int i = lo, j = mid, o = lo;
        while (i < mid && j < hi) buf[o++] = less(a[j], a[i], rank, k, n) ? a[j++] : a[i++];
        while (i < mid) buf[o++] = a[i++];
        while (j < hi) buf[o++] = a[j++];
        System.arraycopy(buf, lo, a, lo, hi - lo);
    }

    /** compares the first |pat| characters of the suffix at pos with pat: <0, 0, >0 */
    private static int cmpPrefix(String s, int pos, String pat) {
        int m = pat.length();
        for (int j = 0; j < m; j++) {
            if (pos + j >= s.length()) return -1;
            int d = s.charAt(pos + j) - pat.charAt(j);
            if (d != 0) return d;
        }
        return 0;
    }

    /** first index in sa whose suffix starts with something >= pat */
    public static int lowerBound(String s, int[] sa, String pat) {
        int lo = 0, hi = sa.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cmpPrefix(s, sa[mid], pat) < 0) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    /** first index in sa whose suffix starts with something > pat (prefix-wise) */
    public static int upperBound(String s, int[] sa, String pat) {
        int lo = 0, hi = sa.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cmpPrefix(s, sa[mid], pat) <= 0) lo = mid + 1; else hi = mid;
        }
        return lo;
    }
}
