package agriflow.algorithms;

/** Z-function: z[i] = how long the string starting at i matches the start of the string. O(n). */
public final class ZAlgorithm {
    private ZAlgorithm() {}

    public static int[] zArray(String s) {
        int n = s.length();
        int[] z = new int[n];
        if (n == 0) return z;
        z[0] = n;
        for (int i = 1, l = 0, r = 0; i < n; i++) {
            if (i < r) z[i] = Math.min(r - i, z[i - l]);
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
            if (i + z[i] > r) { l = i; r = i + z[i]; }
        }
        return z;
    }

    public static int[] findAll(String t, String p) {
        IntList r = new IntList();
        int m = p.length();
        if (m == 0 || m > t.length()) return r.toArray();
        int[] z = zArray(p + '\u0001' + t);
        for (int i = m + 1; i < z.length; i++) if (z[i] == m) r.add(i - m - 1);
        return r.toArray();
    }
}
