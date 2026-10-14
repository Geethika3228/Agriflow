package agriflow.algorithms;

/** Tiny growable int array. Hand-built so the engine never needs java.util. */
public final class IntList {
    private int[] a = new int[16];
    private int n;

    public void add(int x) {
        if (n == a.length) {
            int[] b = new int[n * 2];
            System.arraycopy(a, 0, b, 0, n);
            a = b;
        }
        a[n++] = x;
    }

    public int size() { return n; }
    public int get(int i) { return a[i]; }

    public int[] toArray() {
        int[] r = new int[n];
        System.arraycopy(a, 0, r, 0, n);
        return r;
    }
}
