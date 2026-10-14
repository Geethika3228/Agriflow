package agriflow.algorithms;

/** Things you get almost for free once you own a suffix array + LCP array. */
public final class SuffixTools {
    private SuffixTools() {}

    /** longest substring that appears at least twice: {startPosition, length} */
    public static int[] longestRepeated(String s) {
        if (s.length() < 2) return new int[]{0, 0};
        int[] sa = SuffixArray.build(s);
        int[] lcp = LCPArray.kasai(s, sa);
        int best = 0, at = 0;
        for (int i = 1; i < lcp.length; i++) if (lcp[i] > best) { best = lcp[i]; at = sa[i]; }
        return new int[]{at, best};
    }

    /** longest substring shared by a and b: {startInA, startInB, length} */
    public static int[] longestCommon(String a, String b) {
        if (a.length() == 0 || b.length() == 0) return new int[]{0, 0, 0};
        String s = a + '\u0001' + b;
        int[] sa = SuffixArray.build(s);
        int[] lcp = LCPArray.kasai(s, sa);
        int la = a.length(), best = 0, pa = 0, pb = 0;
        for (int i = 1; i < sa.length; i++) {
            int x = sa[i - 1], y = sa[i];
            boolean xa = x < la, ya = y < la;
            if (xa != ya && lcp[i] > best) {
                best = lcp[i];
                pa = xa ? x : y;
                pb = (xa ? y : x) - la - 1;
            }
        }
        return new int[]{pa, pb, best};
    }

    /** number of different substrings = n(n+1)/2 - sum(lcp) */
    public static long distinctSubstrings(int n, int[] lcp) {
        long total = (long) n * (n + 1) / 2;
        for (int v : lcp) total -= v;
        return total;
    }
}
