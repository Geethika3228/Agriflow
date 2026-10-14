package agriflow.algorithms;

/** Kasai's algorithm: LCP array from a suffix array in O(n). lcp[i] = common prefix of sa[i-1] and sa[i]. */
public final class LCPArray {
    private LCPArray() {}

    public static int[] kasai(String s, int[] sa) {
        int n = s.length();
        int[] rank = new int[n], lcp = new int[n];
        for (int i = 0; i < n; i++) rank[sa[i]] = i;
        int h = 0;
        for (int i = 0; i < n; i++) {
            if (rank[i] > 0) {
                int j = sa[rank[i] - 1];
                while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
                lcp[rank[i]] = h;
                if (h > 0) h--;
            } else {
                h = 0;
            }
        }
        return lcp;
    }
}
