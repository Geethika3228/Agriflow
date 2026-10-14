package agriflow.algorithms;

/**
 * Reservoir sampling (Algorithm R): keep k items from a stream of unknown length, every item equally likely,
 * using only O(k) memory and one pass. Proof: item i enters with prob k/i and survives each later step j with prob 1 - 1/j,
 * so P(in sample at the end) = k/i * (i/(i+1)) * ... * ((n-1)/n) = k/n.
 */
public final class Reservoir {
    private Reservoir() {}

    /** returns the k sampled stream positions (0..n-1) */
    public static int[] sample(int n, int k, Rng rng) {
        int[] res = new int[Math.min(k, n)];
        for (int i = 0; i < n; i++) {
            if (i < k) res[i] = i;
            else {
                int j = rng.nextInt(i + 1);
                if (j < k) res[j] = i;
            }
        }
        return res;
    }

    /** how many times each stream position ended up in the sample across `trials` independent runs */
    public static int[] inclusionCounts(int n, int k, int trials, Rng rng) {
        int[] cnt = new int[n];
        for (int t = 0; t < trials; t++) for (int x : sample(n, k, rng)) cnt[x]++;
        return cnt;
    }
}
