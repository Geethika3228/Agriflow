package agriflow.algorithms;

/** Slide the pattern one step at a time and compare. O(n*m) worst case. */
public final class NaiveSearch {
    private NaiveSearch() {}

    public static int[] findAll(String t, String p) {
        IntList r = new IntList();
        int n = t.length(), m = p.length();
        if (m == 0 || m > n) return r.toArray();
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m && t.charAt(i + j) == p.charAt(j)) j++;
            if (j == m) r.add(i);
        }
        return r.toArray();
    }
}
