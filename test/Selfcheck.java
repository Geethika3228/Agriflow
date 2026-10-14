import agriflow.algorithms.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Checks every algorithm against a slow-but-obviously-correct brute force on lots of random inputs.
 * (Tests may use java.util; the engine itself may not.)   Run:  java -cp out:testout Selfcheck
 */
public class Selfcheck {
    static int checks = 0, failures = 0;
    static Random R = new Random(12345);

    static void ok(boolean cond, String what) {
        checks++;
        if (!cond) { failures++; System.out.println("  FAIL: " + what); }
    }

    static String rs(int len, int alpha) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append((char) ('a' + R.nextInt(alpha)));
        return sb.toString();
    }

    static void section(String s) { System.out.println("== " + s); }

    public static void main(String[] args) {
        strings(); dp(); flows(); np(); randomised(); parallel();
        System.out.println("\n" + checks + " checks, " + failures + " failures");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------------------------------ strings
    static void strings() {
        section("Module 2: strings");
        Rng rng = new Rng(7);
        for (int it = 0; it < 3000; it++) {
            String t = rs(R.nextInt(60), 1 + R.nextInt(3)), p = rs(1 + R.nextInt(5), 1 + R.nextInt(3));
            int[] want = NaiveSearch.findAll(t, p);
            // independent brute force using String.indexOf
            int cnt = 0;
            for (int i = t.indexOf(p); i >= 0; i = t.indexOf(p, i + 1)) cnt++;
            ok(want.length == cnt, "naive count " + t + "/" + p);
            ok(Arrays.equals(want, KMP.findAll(t, p)), "KMP " + t + "/" + p);
            ok(Arrays.equals(want, ZAlgorithm.findAll(t, p)), "Z " + t + "/" + p);
            ok(Arrays.equals(want, RabinKarp.findAll(t, p)), "RK " + t + "/" + p);
            ok(Arrays.equals(want, RabinKarp.findAllRandom(t, p, rng)), "RK random " + t + "/" + p);
            RabinKarp.Result weak = RabinKarp.scan(t, p, 7, 3, 0, 0, true);
            ok(Arrays.equals(want, weak.positions), "RK tiny modulus still correct when verified");
            // suffix array search
            String low = t;
            int[] sa = SuffixArray.build(low);
            ok(SuffixArray.upperBound(low, sa, p) - SuffixArray.lowerBound(low, sa, p) == want.length, "SA count " + t + "/" + p);
        }
        for (int it = 0; it < 1500; it++) {
            String t = rs(R.nextInt(80), 3);
            int k = 1 + R.nextInt(5);
            String[] pats = new String[k];
            for (int i = 0; i < k; i++) pats[i] = rs(1 + R.nextInt(4), 3);
            AhoCorasick ac = new AhoCorasick(pats);
            int[] got = ac.search(t, pats);
            long gotSum = 0, wantSum = 0;
            int gotN = got.length / 2, wantN = 0;
            for (int i = 0; i < gotN; i++) gotSum += got[2 * i] * 1000003L + got[2 * i + 1];
            for (int i = 0; i < k; i++)
                for (int pos : NaiveSearch.findAll(t, pats[i])) { wantN++; wantSum += i * 1000003L + pos; }
            // duplicate patterns: Aho keeps only the LAST index, so compare by position sets per distinct text
            boolean dup = false;
            for (int i = 0; i < k; i++) for (int j = i + 1; j < k; j++) if (pats[i].equals(pats[j])) dup = true;
            if (!dup) ok(gotN == wantN && gotSum == wantSum, "AhoCorasick " + t + " " + Arrays.toString(pats));
        }
        for (int it = 0; it < 600; it++) {
            String s = rs(1 + R.nextInt(40), 1 + R.nextInt(3));
            int n = s.length();
            int[] sa = SuffixArray.build(s);
            Integer[] idx = new Integer[n];
            for (int i = 0; i < n; i++) idx[i] = i;
            Arrays.sort(idx, (x, y) -> s.substring(x).compareTo(s.substring(y)));
            boolean same = true;
            for (int i = 0; i < n; i++) if (idx[i] != sa[i]) same = false;
            ok(same, "suffix array order " + s);
            int[] lcp = LCPArray.kasai(s, sa);
            boolean lcpOk = true;
            for (int i = 1; i < n; i++) {
                int h = 0;
                String a = s.substring(sa[i - 1]), b = s.substring(sa[i]);
                while (h < a.length() && h < b.length() && a.charAt(h) == b.charAt(h)) h++;
                if (h != lcp[i]) lcpOk = false;
            }
            ok(lcpOk, "LCP " + s);
            Set<String> subs = new HashSet<>();
            for (int i = 0; i < n; i++) for (int j = i + 1; j <= n; j++) subs.add(s.substring(i, j));
            ok(SuffixTools.distinctSubstrings(n, lcp) == subs.size(), "distinct substrings " + s);
            int bestRep = 0;
            for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
                int h = 0;
                while (j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
                bestRep = Math.max(bestRep, h);
            }
            int[] rep = SuffixTools.longestRepeated(s);
            ok(rep[1] == bestRep, "longest repeated " + s);
            String u = rs(R.nextInt(30), 3);
            int bestC = 0;
            for (int i = 0; i < s.length(); i++) for (int j = 0; j < u.length(); j++) {
                int h = 0;
                while (i + h < s.length() && j + h < u.length() && s.charAt(i + h) == u.charAt(j + h)) h++;
                bestC = Math.max(bestC, h);
            }
            int[] com = SuffixTools.longestCommon(s, u);
            ok(com[2] == bestC && s.regionMatches(com[0], u, com[1], com[2]), "longest common " + s + "/" + u);
        }
        // adversarial: naive should do way more comparisons than KMP
        StringBuilder a = new StringBuilder();
        for (int i = 0; i < 5000; i++) a.append('a');
        String text = a.toString(), pat = "aaaaaaaaaaaaaaaaaaab";
        ok(StringLab.naiveComparisons(text, pat) > 8 * StringLab.kmpComparisons(text, pat) && StringLab.kmpComparisons(text, pat) <= 2L * text.length(), "naive worse than KMP on a^n");
    }

    // ------------------------------------------------------------------------------------------ DP
    static int levBrute(String a, String b, int i, int j, int[][] memo) {
        if (i == 0) return j;
        if (j == 0) return i;
        if (memo[i][j] >= 0) return memo[i][j];
        int r = Math.min(levBrute(a, b, i - 1, j, memo) + 1, levBrute(a, b, i, j - 1, memo) + 1);
        r = Math.min(r, levBrute(a, b, i - 1, j - 1, memo) + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
        return memo[i][j] = r;
    }

    static long mcBrute(int[] d, int i, int j) {
        if (i == j) return 0;
        long best = Long.MAX_VALUE;
        for (int k = i; k < j; k++) best = Math.min(best, mcBrute(d, i, k) + mcBrute(d, k + 1, j) + (long) d[i] * d[k + 1] * d[j + 1]);
        return best;
    }

    static long obstBrute(long[] f, int i, int j) {   // [i,j)
        if (i >= j) return 0;
        long sum = 0, best = Long.MAX_VALUE;
        for (int k = i; k < j; k++) sum += f[k];
        for (int k = i; k < j; k++) best = Math.min(best, obstBrute(f, i, k) + obstBrute(f, k + 1, j));
        return best + sum;
    }

    static void permute(int[] a, int k, double[][] d, double[] best) {
        if (k == a.length) { best[0] = Math.min(best[0], Tsp.tourCost(d, a)); return; }
        for (int i = k; i < a.length; i++) {
            int t = a[k]; a[k] = a[i]; a[i] = t;
            permute(a, k + 1, d, best);
            t = a[k]; a[k] = a[i]; a[i] = t;
        }
    }

    static void dp() {
        section("Module 3: dynamic programming");
        for (int it = 0; it < 1500; it++) {
            String a = rs(R.nextInt(9), 3), b = rs(R.nextInt(9), 3);
            int want = levBrute(a, b, a.length(), b.length(), filled(a.length() + 1, b.length() + 1));
            ok(EditDistance.distance(a, b) == want, "lev " + a + "/" + b);
            ok(EditDistance.table(a, b)[a.length()][b.length()] == want, "lev table");
            ok(EditDistance.damerau(a, b) <= want, "damerau <= lev");
            ok(EditDistance.weighted(a, b, 1, 1, 1) <= want, "weighted(1,1,1) <= lev (vowel discount)");
            String[] ops = EditDistance.ops(a, b);
            StringBuilder out = new StringBuilder(), src = new StringBuilder();
            int edits = 0;
            for (String o : ops) {
                if (o.startsWith("M:")) { out.append(o.charAt(2)); src.append(o.charAt(2)); }
                else if (o.startsWith("S:")) { src.append(o.charAt(2)); out.append(o.charAt(4)); edits++; }
                else if (o.startsWith("D:")) { src.append(o.charAt(2)); edits++; }
                else { out.append(o.charAt(2)); edits++; }
            }
            ok(out.toString().equals(b) && src.toString().equals(a) && edits == want, "edit script " + a + "->" + b);
        }
        ok(EditDistance.distance("kitten", "sitting") == 3, "kitten/sitting");
        ok(EditDistance.damerau("ca", "abc") == 2, "true Damerau ca->abc is 2");
        ok(EditDistance.damerau("blast", "balst") == 1, "swap counts as one edit");
        ok(EditDistance.distance("blast", "balst") == 2, "Levenshtein needs two");

        for (int it = 0; it < 800; it++) {
            String a = rs(1 + R.nextInt(10), 3), b = rs(1 + R.nextInt(10), 3);
            Alignment.Result g = Alignment.global(a, b, 2, -1, 2);
            ok(g.alignedA.replace("-", "").equals(a) && g.alignedB.replace("-", "").equals(b), "global alignment keeps letters");
            ok(g.alignedA.length() == g.alignedB.length(), "global equal length");
            int rescore = 0;
            for (int k = 0; k < g.alignedA.length(); k++) {
                char x = g.alignedA.charAt(k), y = g.alignedB.charAt(k);
                rescore += (x == '-' || y == '-') ? -2 : (x == y ? 2 : -1);
            }
            ok(rescore == g.score, "global score matches its own alignment");
            // brute-force global score by recursion
            ok(g.score == alignBrute(a, b, a.length(), b.length(), 2, -1, 2, new Integer[a.length() + 1][b.length() + 1]), "NW score " + a + "/" + b);
            Alignment.Result l = Alignment.local(a, b, 2, -1, 2);
            ok(l.score >= 0 && l.score >= Math.max(0, bestSingle(a, b)), "local >= 0");
            int ls = 0;
            for (int k = 0; k < l.alignedA.length(); k++) {
                char x = l.alignedA.charAt(k), y = l.alignedB.charAt(k);
                ls += (x == '-' || y == '-') ? -2 : (x == y ? 2 : -1);
            }
            ok(ls == l.score, "local score matches its own alignment " + a + "/" + b);
            ok(l.score >= g.score, "local >= global");
            Alignment.Result af = Alignment.affine(a, b, 2, -1, 2, 2);
            ok(af.score == g.score, "affine with open==extend equals linear " + a + "/" + b + " " + af.score + " vs " + g.score);
            Alignment.Result af2 = Alignment.affine(a, b, 2, -1, 4, 1);
            ok(af2.alignedA.replace("-", "").equals(a) && af2.alignedB.replace("-", "").equals(b), "affine keeps letters");
            int s2 = 0;
            boolean inGapA = false, inGapB = false;
            for (int k = 0; k < af2.alignedA.length(); k++) {
                char x = af2.alignedA.charAt(k), y = af2.alignedB.charAt(k);
                if (x == '-') { s2 -= inGapA ? 1 : 4; inGapA = true; inGapB = false; }
                else if (y == '-') { s2 -= inGapB ? 1 : 4; inGapB = true; inGapA = false; }
                else { s2 += x == y ? 2 : -1; inGapA = inGapB = false; }
            }
            ok(s2 == af2.score, "affine score matches own alignment " + a + "/" + b + " " + af2.alignedA + " " + af2.alignedB + " " + s2 + " vs " + af2.score);
        }

        for (int it = 0; it < 300; it++) {
            int k = 2 + R.nextInt(6);
            int[] d = new int[k + 1];
            for (int i = 0; i <= k; i++) d[i] = 1 + R.nextInt(30);
            IntervalDP.ChainResult c = IntervalDP.matrixChain(d);
            ok(c.cost == mcBrute(d, 0, k - 1), "matrix chain " + Arrays.toString(d));
            ok(c.cost <= c.leftToRightCost, "optimal <= left-to-right");
            int n = 1 + R.nextInt(8);
            long[] f = new long[n];
            for (int i = 0; i < n; i++) f[i] = 1 + R.nextInt(40);
            IntervalDP.ObstResult o = IntervalDP.obst(f);
            ok(o.cost == obstBrute(f, 0, n), "OBST " + Arrays.toString(f));
            ok(o.opsKnuth >= 0, "Knuth agrees with cubic DP " + Arrays.toString(f));
            ok(o.opsKnuth <= o.opsCubic, "Knuth never does more work");
        }

        for (int it = 0; it < 150; it++) {
            int n = 2 + R.nextInt(6);
            double[][] pts = new double[n][2];
            for (double[] p : pts) { p[0] = R.nextInt(100); p[1] = R.nextInt(100); }
            double[][] d = dist(pts);
            Tsp.Result ex = Tsp.exact(d);
            double[] best = {Double.MAX_VALUE};
            int[] perm = new int[n];
            for (int i = 0; i < n; i++) perm[i] = i;
            permute(perm, 1, d, best);
            ok(Math.abs(ex.cost - best[0]) < 1e-9, "TSP exact n=" + n);
            ok(Math.abs(Tsp.tourCost(d, ex.order) - ex.cost) < 1e-9, "TSP order matches cost");
            Tsp.Result ap = Tsp.mstApprox(d);
            ok(ap.cost <= 2 * ex.cost + 1e-9, "MST preorder is a 2-approximation");
            ok(Tsp.mstWeight(d) <= ex.cost + 1e-9, "MST weight is a lower bound");
            ok(Tsp.nearestNeighbour(d).cost >= ex.cost - 1e-9, "NN >= optimal");
        }
        for (int it = 0; it < 400; it++) {
            int n = 1 + R.nextInt(7);
            int[] adj = new int[n];
            for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) if (R.nextInt(100) < 35) { adj[i] |= 1 << j; adj[j] |= 1 << i; }
            int[] path = Tsp.hamiltonianPath(n, adj);
            boolean brute = hamBrute(n, adj);
            ok((path != null) == brute, "Hamiltonian path existence");
            if (path != null) {
                boolean valid = true;
                boolean[] seen = new boolean[n];
                for (int i = 0; i < n; i++) { if (seen[path[i]]) valid = false; seen[path[i]] = true; if (i > 0 && (adj[path[i - 1]] & (1 << path[i])) == 0) valid = false; }
                ok(valid, "Hamiltonian path is a real path");
            }
        }
        for (int it = 0; it < 300; it++) {
            int u = 1 + R.nextInt(8), k = 1 + R.nextInt(7);
            int[] sets = new int[k];
            int all = 0;
            for (int i = 0; i < k; i++) { sets[i] = R.nextInt(1 << u); all |= sets[i]; }
            int[] ex = SetCover.exact(sets, u), gr = SetCover.greedy(sets, u);
            boolean coverable = all == (1 << u) - 1;
            ok((ex != null) == coverable && (gr != null) == coverable, "set cover feasibility");
            if (ex != null) {
                int bestK = Integer.MAX_VALUE;
                for (int m = 0; m < (1 << k); m++) {
                    int cov = 0;
                    for (int i = 0; i < k; i++) if ((m & (1 << i)) != 0) cov |= sets[i];
                    if (cov == (1 << u) - 1) bestK = Math.min(bestK, Integer.bitCount(m));
                }
                ok(ex.length == bestK, "set cover exact optimal");
                ok(gr.length >= ex.length, "greedy >= exact");
                int cov = 0;
                for (int s : ex) cov |= sets[s];
                ok(cov == (1 << u) - 1, "exact cover is a cover");
            }
        }
        for (int it = 0; it < 300; it++) {
            int n = 1 + R.nextInt(12);
            int[] eu = new int[n - 1], ev = new int[n - 1], ew = new int[n - 1];
            long[] nw = new long[n];
            for (int i = 1; i < n; i++) { eu[i - 1] = R.nextInt(i); ev[i - 1] = i; ew[i - 1] = 1 + R.nextInt(9); }
            for (int i = 0; i < n; i++) nw[i] = 1 + R.nextInt(20);
            TreeDP.Result t = TreeDP.analyze(n, eu, ev, ew, nw);
            long[][] dd = new long[n][n];
            for (long[] row : dd) Arrays.fill(row, Long.MAX_VALUE / 4);
            for (int i = 0; i < n; i++) dd[i][i] = 0;
            for (int i = 0; i < n - 1; i++) { dd[eu[i]][ev[i]] = ew[i]; dd[ev[i]][eu[i]] = ew[i]; }
            for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) dd[i][j] = Math.min(dd[i][j], dd[i][k] + dd[k][j]);
            long diam = 0;
            boolean sumOk = true;
            for (int i = 0; i < n; i++) { long s = 0; for (int j = 0; j < n; j++) { s += dd[i][j]; diam = Math.max(diam, dd[i][j]); } if (s != t.sumDist[i]) sumOk = false; }
            ok(sumOk, "rerooting sums n=" + n);
            ok(t.diameter == diam, "diameter");
            long pl = 0;
            for (int i = 1; i < t.diameterPath.length; i++) pl += dd[t.diameterPath[i - 1]][t.diameterPath[i]];
            ok(pl == diam, "diameter path length");
            long bestSum = Long.MAX_VALUE;
            for (int i = 0; i < n; i++) bestSum = Math.min(bestSum, t.sumDist[i]);
            ok(t.sumDist[t.bestHub] == bestSum, "best hub");
            long bestIS = 0;
            for (int m = 0; m < (1 << n); m++) {
                boolean good = true;
                long w = 0;
                for (int i = 0; i < n; i++) if ((m & (1 << i)) != 0) w += nw[i];
                for (int i = 0; i < n - 1 && good; i++) if ((m & (1 << eu[i])) != 0 && (m & (1 << ev[i])) != 0) good = false;
                if (good) bestIS = Math.max(bestIS, w);
            }
            ok(t.maxIndependent == bestIS, "tree independent set");
            long pw = 0;
            boolean indep = true;
            for (int i = 0; i < n; i++) if (t.picked[i]) pw += nw[i];
            for (int i = 0; i < n - 1; i++) if (t.picked[eu[i]] && t.picked[ev[i]]) indep = false;
            ok(indep && pw == bestIS, "picked set is independent and optimal");
        }
        for (int it = 0; it < 100; it++) {
            int k = 1 + R.nextInt(9);
            long[] f = new long[1 << k];
            for (int i = 0; i < f.length; i++) f[i] = R.nextInt(10);
            ok(Arrays.equals(SubsetDP.sos(k, f), SubsetDP.sosSlow(k, f)), "SOS DP k=" + k);
        }
        for (int it = 0; it < 300; it++) {
            int[] coins = {1 + R.nextInt(6), 2 + R.nextInt(8), 3 + R.nextInt(10)};
            int target = R.nextInt(40);
            int[] dpc = SubsetDP.coinDP(coins, target);
            int bestN = Integer.MAX_VALUE;
            for (int a = 0; a * coins[0] <= target; a++) for (int b = 0; a * coins[0] + b * coins[1] <= target; b++) {
                int rest = target - a * coins[0] - b * coins[1];
                if (rest % coins[2] == 0) bestN = Math.min(bestN, a + b + rest / coins[2]);
            }
            ok((dpc == null) == (bestN == Integer.MAX_VALUE) && (dpc == null || dpc.length == bestN), "coin DP " + Arrays.toString(coins) + " " + target);
        }
        int[] g = SubsetDP.coinGreedy(new int[]{1, 3, 4}, 6), d2 = SubsetDP.coinDP(new int[]{1, 3, 4}, 6);
        ok(g.length == 3 && d2.length == 2, "greedy fails on {1,3,4} target 6");
    }

    static int bestSingle(String a, String b) { return 0; }

    static int alignBrute(String a, String b, int i, int j, int ma, int mi, int gap, Integer[][] memo) {
        if (memo[i][j] != null) return memo[i][j];
        int r;
        if (i == 0 && j == 0) r = 0;
        else {
            r = Integer.MIN_VALUE;
            if (i > 0 && j > 0) r = Math.max(r, alignBrute(a, b, i - 1, j - 1, ma, mi, gap, memo) + (a.charAt(i - 1) == b.charAt(j - 1) ? ma : mi));
            if (i > 0) r = Math.max(r, alignBrute(a, b, i - 1, j, ma, mi, gap, memo) - gap);
            if (j > 0) r = Math.max(r, alignBrute(a, b, i, j - 1, ma, mi, gap, memo) - gap);
        }
        return memo[i][j] = r;
    }

    static boolean hamBrute(int n, int[] adj) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        return hamRec(p, 0, adj);
    }

    static boolean hamRec(int[] p, int k, int[] adj) {
        if (k == p.length) return true;
        for (int i = k; i < p.length; i++) {
            int t = p[k]; p[k] = p[i]; p[i] = t;
            boolean ok = (k == 0 || (adj[p[k - 1]] & (1 << p[k])) != 0) && hamRec(p, k + 1, adj);
            t = p[k]; p[k] = p[i]; p[i] = t;
            if (ok) return true;
        }
        return false;
    }

    static int[][] filled(int n, int m) {
        int[][] a = new int[n][m];
        for (int[] r : a) Arrays.fill(r, -1);
        return a;
    }

    static double[][] dist(double[][] pts) {
        int n = pts.length;
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) d[i][j] = Math.hypot(pts[i][0] - pts[j][0], pts[i][1] - pts[j][1]);
        return d;
    }

    // ------------------------------------------------------------------------------------------ flows
    static void flows() {
        section("Module 4: network flow");
        for (int it = 0; it < 600; it++) {
            int n = 3 + R.nextInt(6), m = n + R.nextInt(2 * n);
            int[] eu = new int[m], ev = new int[m], ec = new int[m];
            for (int i = 0; i < m; i++) { eu[i] = R.nextInt(n); ev[i] = R.nextInt(n); if (eu[i] == ev[i]) ev[i] = (ev[i] + 1) % n; ec[i] = R.nextInt(10); }
            int s = 0, t = n - 1;
            FlowNetwork a = new FlowNetwork(n, m), b = new FlowNetwork(n, m);
            int[] ia = new int[m];
            for (int i = 0; i < m; i++) { ia[i] = a.addEdge(eu[i], ev[i], ec[i], 0); b.addEdge(eu[i], ev[i], ec[i], 0); }
            long fa = a.edmondsKarp(s, t), fb = b.dinic(s, t);
            ok(fa == fb, "EK == Dinic");
            long bestCut = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << n); mask++) {
                if ((mask & 1) == 0 || (mask & (1 << t)) != 0) continue;
                long cut = 0;
                for (int i = 0; i < m; i++) if ((mask & (1 << eu[i])) != 0 && (mask & (1 << ev[i])) == 0) cut += ec[i];
                bestCut = Math.min(bestCut, cut);
            }
            ok(fa == bestCut, "max-flow == brute-force min-cut");
            boolean[] side = a.sourceSide(s);
            long cap = 0;
            for (int i = 0; i < m; i++) if (side[eu[i]] && !side[ev[i]]) cap += ec[i];
            ok(cap == fa && side[s] && !side[t], "residual-reachable set is a min cut");
            long[] bal = new long[n];
            boolean capsOk = true;
            for (int i = 0; i < m; i++) {
                int f = a.flowOn(ia[i]);
                if (f < 0 || f > ec[i]) capsOk = false;
                bal[eu[i]] -= f; bal[ev[i]] += f;
            }
            boolean cons = true;
            for (int v = 0; v < n; v++) if (v != s && v != t && bal[v] != 0) cons = false;
            ok(capsOk && cons && bal[t] == fa, "capacity + conservation hold");
        }
        for (int it = 0; it < 400; it++) {
            int nl = 1 + R.nextInt(6), nr = 1 + R.nextInt(6);
            int[][] adj = new int[nl][];
            boolean[][] e = new boolean[nl][nr];
            for (int l = 0; l < nl; l++) {
                int c = 0;
                for (int r = 0; r < nr; r++) if (R.nextInt(100) < 40) { e[l][r] = true; c++; }
                adj[l] = new int[c];
                c = 0;
                for (int r = 0; r < nr; r++) if (e[l][r]) adj[l][c++] = r;
            }
            FlowApps.Matching mt = FlowApps.bipartite(nl, nr, adj);
            ok(mt.size == matchBrute(e, 0, new boolean[nr]), "bipartite matching size");
            int cv = 0;
            for (boolean x : mt.coverL) if (x) cv++;
            for (boolean x : mt.coverR) if (x) cv++;
            boolean cover = true;
            for (int l = 0; l < nl; l++) for (int r = 0; r < nr; r++) if (e[l][r] && !mt.coverL[l] && !mt.coverR[r]) cover = false;
            ok(cover && cv == mt.size, "Koenig: cover is valid and as small as the matching");
        }
        for (int it = 0; it < 300; it++) {
            int n = 1 + R.nextInt(4);
            int[][] cost = new int[n][n];
            for (int[] row : cost) for (int j = 0; j < n; j++) row[j] = 1 + R.nextInt(20);
            FlowNetwork net = new FlowNetwork(2 * n + 2, n * n + 2 * n);
            int s = 2 * n, t = s + 1;
            for (int i = 0; i < n; i++) { net.addEdge(s, i, 1, 0); net.addEdge(n + i, t, 1, 0); }
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) net.addEdge(i, n + j, 1, cost[i][j]);
            long f = net.minCostFlow(s, t, 1000);
            int[] p = new int[n];
            for (int i = 0; i < n; i++) p[i] = i;
            long[] best = {Long.MAX_VALUE};
            assignRec(p, 0, cost, best);
            ok(f == n && net.totalCost == best[0], "min-cost flow == best assignment (n=" + n + ")");
        }
        for (int it = 0; it < 300; it++) {
            int n = 1 + R.nextInt(8);
            long[] profit = new long[n];
            for (int i = 0; i < n; i++) profit[i] = R.nextInt(41) - 20;
            int k = R.nextInt(2 * n);
            int[] fr = new int[k], to = new int[k];
            for (int i = 0; i < k; i++) { fr[i] = R.nextInt(n); to[i] = R.nextInt(n); if (fr[i] == to[i]) to[i] = (to[i] + 1) % n; }
            if (n == 1) { fr = new int[0]; to = new int[0]; k = 0; }
            FlowApps.Selection sel = FlowApps.projectSelection(profit, fr, to);
            long best = 0;
            for (int m = 0; m < (1 << n); m++) {
                boolean closed = true;
                long v = 0;
                for (int i = 0; i < k && closed; i++) if ((m & (1 << fr[i])) != 0 && (m & (1 << to[i])) == 0) closed = false;
                for (int i = 0; i < n; i++) if ((m & (1 << i)) != 0) v += profit[i];
                if (closed) best = Math.max(best, v);
            }
            long mine = 0;
            boolean closedMine = true;
            for (int i = 0; i < n; i++) if (sel.chosen[i]) mine += profit[i];
            for (int i = 0; i < k; i++) if (sel.chosen[fr[i]] && !sel.chosen[to[i]]) closedMine = false;
            ok(sel.profit == best && mine == best && closedMine, "project selection");
        }
    }

    static void assignRec(int[] p, int k, int[][] cost, long[] best) {
        int n = p.length;
        if (k == n) { long c = 0; for (int i = 0; i < n; i++) c += cost[i][p[i]]; best[0] = Math.min(best[0], c); return; }
        for (int i = k; i < n; i++) {
            int t = p[k]; p[k] = p[i]; p[i] = t;
            assignRec(p, k + 1, cost, best);
            t = p[k]; p[k] = p[i]; p[i] = t;
        }
    }

    static int matchBrute(boolean[][] e, int l, boolean[] used) {
        if (l == e.length) return 0;
        int best = matchBrute(e, l + 1, used);
        for (int r = 0; r < used.length; r++) if (e[l][r] && !used[r]) {
            used[r] = true;
            best = Math.max(best, 1 + matchBrute(e, l + 1, used));
            used[r] = false;
        }
        return best;
    }

    // ------------------------------------------------------------------------------------------ NP
    static void np() {
        section("Module 5: NP-completeness + approximation");
        for (int it = 0; it < 300; it++) {
            int n = 2 + R.nextInt(9);
            int m = R.nextInt(n * 2);
            Set<Long> seen = new HashSet<>();
            int[] eu = new int[m], ev = new int[m];
            int c = 0;
            for (int i = 0; i < m; i++) {
                int u = R.nextInt(n), v = R.nextInt(n);
                if (u == v) continue;
                long key = Math.min(u, v) * 100L + Math.max(u, v);
                if (!seen.add(key)) continue;
                eu[c] = u; ev[c] = v; c++;
            }
            eu = Arrays.copyOf(eu, c); ev = Arrays.copyOf(ev, c);
            boolean[] ex = VertexCover.exact(n, eu, ev), ap = VertexCover.approx2(n, eu, ev);
            ok(VertexCover.isCover(n, eu, ev, ex) && VertexCover.isCover(n, eu, ev, ap), "covers are covers");
            int opt = VertexCover.size(ex);
            ok(VertexCover.size(ap) <= 2 * opt, "2-approximation ratio");
            ok(VertexCover.size(ap) >= opt, "approx >= optimal");
            VertexCover.Fpt yes = VertexCover.fpt(n, eu, ev, opt);
            ok(yes.cover != null && VertexCover.isCover(n, eu, ev, yes.cover) && VertexCover.size(yes.cover) <= opt, "FPT finds cover of size OPT");
            if (opt > 0) ok(VertexCover.fpt(n, eu, ev, opt - 1).cover == null, "FPT says no for OPT-1");
            boolean[] indep = new boolean[n];
            for (int i = 0; i < n; i++) indep[i] = !ex[i];
            boolean isInd = true;
            for (int i = 0; i < eu.length; i++) if (indep[eu[i]] && indep[ev[i]]) isInd = false;
            ok(isInd, "complement of a cover is an independent set");
        }
        for (int it = 0; it < 400; it++) {
            int n = 1 + R.nextInt(10), cap = 1 + R.nextInt(40);
            int[] v = new int[n], w = new int[n];
            for (int i = 0; i < n; i++) { v[i] = 1 + R.nextInt(60); w[i] = 1 + R.nextInt(20); }
            Knapsack.Result ex = Knapsack.exact(v, w, cap);
            long best = 0;
            for (int m = 0; m < (1 << n); m++) {
                int ww = 0; long vv = 0;
                for (int i = 0; i < n; i++) if ((m & (1 << i)) != 0) { ww += w[i]; vv += v[i]; }
                if (ww <= cap) best = Math.max(best, vv);
            }
            ok(ex.value == best, "knapsack exact");
            long tv = 0; int tw = 0;
            for (int i = 0; i < n; i++) if (ex.take[i]) { tv += v[i]; tw += w[i]; }
            ok(tv == best && tw <= cap && tw == ex.weight, "knapsack chosen set is consistent");
            double eps = 0.1 + R.nextInt(5) * 0.1;
            Knapsack.Result fp = Knapsack.fptas(v, w, cap, eps);
            ok(fp.weight <= cap && fp.value >= (1 - eps) * best - 1e-9, "FPTAS within (1-eps) of OPT: eps=" + eps + " got " + fp.value + " opt " + best);
            Knapsack.Result gh = Knapsack.greedyHalf(v, w, cap);
            ok(gh.weight <= cap && 2 * gh.value >= best, "greedy+best-item is a 1/2-approximation");
            int[] nums = new int[n];
            for (int i = 0; i < n; i++) nums[i] = 1 + R.nextInt(15);
            int target = R.nextInt(60);
            int[] ss = Knapsack.subsetSum(nums, target);
            boolean can = false;
            for (int m = 0; m < (1 << n) && !can; m++) { int s = 0; for (int i = 0; i < n; i++) if ((m & (1 << i)) != 0) s += nums[i]; if (s == target) can = true; }
            ok((ss != null) == can, "subset sum decision");
            if (ss != null) { int s = 0; for (int i : ss) s += nums[i]; ok(s == target, "subset sum witness verifies"); }
        }
    }

    // ------------------------------------------------------------------------------------------ randomised
    static void randomised() {
        section("Module 6: randomised algorithms");
        for (long n = 0; n < 200_000; n++) if (MillerRabin.isPrime(n) != MillerRabin.trialDivision(n)) { ok(false, "prime mismatch at " + n); break; }
        ok(true, "Miller-Rabin matches trial division below 200000");
        ok(MillerRabin.isPrime(1_000_000_007L) && MillerRabin.isPrime(998_244_353L), "known primes");
        ok(MillerRabin.isPrime(2305843009213693951L), "2^61-1 is prime");
        ok(MillerRabin.isPrime(9223372036854775783L), "largest prime below 2^63");
        ok(MillerRabin.isPrime(1_000_000_000_000_000_003L), "1e18+3 is prime");
        ok(!MillerRabin.isPrime(2147483647L * 2147483647L), "(2^31-1)^2 is composite");
        ok(!MillerRabin.isPrime(3215031751L), "strong pseudoprime to bases 2,3,5,7 is caught");
        for (int n : new int[]{561, 1105, 1729, 2465, 2821, 6601}) {
            boolean allFermat = true;
            for (long a = 2; a < n; a++) if (gcd(a, n) == 1 && !MillerRabin.fermatPasses(a, n)) allFermat = false;
            ok(allFermat, n + " is a Carmichael number: Fermat is fooled by every coprime base");
            boolean someWitness = false;
            for (long a = 2; a < n; a++) if (MillerRabin.isWitness(a, n)) someWitness = true;
            ok(someWitness && !MillerRabin.isPrime(n), n + " is exposed by Miller-Rabin");
        }
        Rng rng = new Rng(99);
        int falsePos = 0;
        for (int i = 0; i < 2000; i++) { long c = 3 + rng.nextLong(100000); if (MillerRabin.probablyPrime(c, 10, rng) && !MillerRabin.trialDivision(c)) falsePos++; }
        ok(falsePos == 0, "randomised test (10 rounds) had no errors on 2000 numbers");
        long rp = MillerRabin.randomPrime(1_000_000_000L, 2_000_000_000L, rng);
        ok(MillerRabin.trialDivision(rp) && rp >= 1_000_000_000L && rp < 2_000_000_000L, "random prime");

        for (int it = 0; it < 100; it++) {
            int n = R.nextInt(300);
            int[] a = new int[n], b;
            for (int i = 0; i < n; i++) a[i] = R.nextInt(50);
            b = a.clone();
            Arrays.sort(b);
            int[] c = a.clone(), d = a.clone();
            RandomizedSort.sort(c, true, rng);
            RandomizedSort.sort(d, false, rng);
            ok(Arrays.equals(b, c) && Arrays.equals(b, d), "quicksort sorts");
        }
        int[] sorted = new int[2000];
        for (int i = 0; i < sorted.length; i++) sorted[i] = i;
        long first = RandomizedSort.sort(sorted.clone(), false, rng)[0], rnd = RandomizedSort.sort(sorted.clone(), true, rng)[0];
        ok(first == 2000L * 1999 / 2, "first-element pivot on sorted input = n(n-1)/2 comparisons");
        ok(rnd < 6 * 2000 * Math.log(2000), "random pivot stays near n log n on the same input");

        long[] keys = new long[300];
        Set<Long> have = new HashSet<>();
        for (int i = 0; i < keys.length; i++) { do { keys[i] = rng.nextLong(PerfectHash.P); } while (!have.add(keys[i])); }
        PerfectHash ph = PerfectHash.build(keys, rng);
        boolean allIn = true;
        for (long k : keys) if (!ph.contains(k)) allIn = false;
        int falseHits = 0;
        for (int i = 0; i < 5000; i++) { long k = rng.nextLong(PerfectHash.P); if (!have.contains(k) && ph.contains(k)) falseHits++; }
        ok(allIn && falseHits == 0, "perfect hash: every key found, no false hits");
        ok(ph.totalSpace <= 4 * keys.length, "perfect hash space is O(n)");
        double avg = 0;
        for (int i = 0; i < 300; i++) avg += PerfectHash.collisions(keys, 300, 1 + rng.nextLong(PerfectHash.P - 1), rng.nextLong(PerfectHash.P));
        avg /= 300;
        ok(avg < 1.5 * (300.0 * 299 / 2 / 300), "universal hash: average colliding pairs near the C(n,2)/m bound");

        int n = 20, k = 5, trials = 40000;
        int[] cnt = Reservoir.inclusionCounts(n, k, trials, rng);
        double expect = trials * (double) k / n, sd = Math.sqrt(trials * (k / (double) n) * (1 - k / (double) n));
        boolean uniform = true;
        for (int c : cnt) if (Math.abs(c - expect) > 5 * sd) uniform = false;
        ok(uniform, "reservoir sampling is uniform");
        int[] one = Reservoir.sample(n, k, rng);
        ok(one.length == k, "reservoir returns k items");
    }

    static long gcd(long a, long b) { return b == 0 ? a : gcd(b, a % b); }

    // ------------------------------------------------------------------------------------------ parallel
    static void parallel() {
        section("Module 6: parallel primitives");
        Rng rng = new Rng(5);
        int n = 300_001;
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = rng.nextInt(1000);
        int[] sorted = a.clone();
        Arrays.sort(sorted);
        long[] want = ParallelOps.scanSeq(a);
        for (int th : new int[]{1, 2, 3, 4, 8}) {
            ok(ParallelOps.sumParallel(a, th) == ParallelOps.sumSeq(a), "parallel sum threads=" + th);
            ok(Arrays.equals(want, ParallelOps.scanParallel(a, th)), "Blelloch scan threads=" + th);
            int[] c = a.clone();
            ParallelOps.mergeSortParallel(c, th);
            ok(Arrays.equals(c, sorted), "parallel merge sort threads=" + th);
        }
        int[] tiny = {5, 1, 4};
        ok(Arrays.equals(ParallelOps.scanParallel(tiny, 4), new long[]{5, 6, 10}), "scan on tiny array");
        long[] ws = ParallelOps.sortWorkSpan(1024);
        ok(ws[0] == 1024L * 10 && ws[1] == 2046, "sort work = n log n, span = 2n-2 with sequential merge: " + Arrays.toString(ws));
    }
}
