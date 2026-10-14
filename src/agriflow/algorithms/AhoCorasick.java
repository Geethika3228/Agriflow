package agriflow.algorithms;

/**
 * Aho-Corasick: find MANY patterns in one pass over the text.
 * A trie of all patterns + "failure links" (where to fall back after a mismatch).
 * Build O(total pattern length), scan O(text length + matches).
 */
public final class AhoCorasick {
    private final int[][] go;
    private final int[] fail, outLink, term;
    private final int[] depth;
    private int nodes = 1;

    public AhoCorasick(String[] pats) {
        int total = 1;
        for (String p : pats) total += p.length();
        go = new int[total][128];
        fail = new int[total];
        outLink = new int[total];
        term = new int[total];
        depth = new int[total];
        for (int i = 0; i < total; i++) { for (int c = 0; c < 128; c++) go[i][c] = -1; term[i] = -1; }
        for (int k = 0; k < pats.length; k++) {
            if (pats[k].length() == 0) continue;
            int u = 0;
            for (int i = 0; i < pats[k].length(); i++) {
                int c = ch(pats[k].charAt(i));
                if (go[u][c] == -1) { go[u][c] = nodes; depth[nodes] = depth[u] + 1; nodes++; }
                u = go[u][c];
            }
            term[u] = k;
        }
        int[] q = new int[nodes];
        int qh = 0, qt = 0;
        for (int c = 0; c < 128; c++) {
            int v = go[0][c];
            if (v == -1) go[0][c] = 0; else { fail[v] = 0; q[qt++] = v; }
        }
        while (qh < qt) {
            int u = q[qh++];
            int f = fail[u];
            outLink[u] = term[f] >= 0 ? f : outLink[f];
            for (int c = 0; c < 128; c++) {
                int v = go[u][c];
                if (v == -1) go[u][c] = go[f][c];
                else { fail[v] = go[f][c]; q[qt++] = v; }
            }
        }
    }

    private static int ch(char c) { return c < 128 ? c : 127; }

    public int nodeCount() { return nodes; }

    /** how many nodes have a non-root failure link (the "shortcuts" the automaton uses) */
    public int failLinkCount() {
        int c = 0;
        for (int i = 1; i < nodes; i++) if (fail[i] != 0) c++;
        return c;
    }

    /** returns flattened pairs [patternIndex, startPosition, patternIndex, startPosition, ...] */
    public int[] search(String text, String[] pats) {
        IntList r = new IntList();
        int u = 0;
        for (int i = 0; i < text.length(); i++) {
            u = go[u][ch(text.charAt(i))];
            int x = term[u] >= 0 ? u : outLink[u];
            while (x > 0) {
                r.add(term[x]);
                r.add(i - pats[term[x]].length() + 1);
                x = outLink[x];
            }
        }
        return r.toArray();
    }
}
