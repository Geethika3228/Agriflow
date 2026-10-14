# AgriFlow Algorithm Lab

AgriFlow (farm-data search engine) rebuilt with every algorithm from the Advanced Algorithms syllabus (Modules 1-6),
implemented by hand in Java. The browser page is only a front end: it sends a query, the Java server runs the algorithm,
the page draws the answer.

## Run
    ./run.sh            # compile + start on http://localhost:8080
    ./run.sh --test     # run the brute-force self-check first (54k+ checks), then start
    ./run.sh 9000       # different port
Needs a JDK 17+ (built and tested on 21). No libraries, no build tool.

## Engine rule (from Module 1)
Everything in `src/agriflow/algorithms` uses only arrays and `java.lang`. No `java.util` collections (`IntList` and `Rng`
are hand-built). Check it yourself:  `grep -rn "java.util" src/agriflow/algorithms`  (only a comment matches).
The server and the tests may use the JDK.

## Where each syllabus topic lives
| Module | Topic | File |
|---|---|---|
| 2 | naive, KMP, Z, Rabin-Karp (double hash + random prime) | NaiveSearch, KMP, ZAlgorithm, RabinKarp |
| 2 | Aho-Corasick | AhoCorasick |
| 2 | suffix array (prefix doubling), LCP (Kasai), longest repeat / common substring | SuffixArray, LCPArray, SuffixTools |
| 3 | Levenshtein, Damerau, weighted edit distance | EditDistance |
| 3 | Needleman-Wunsch, Smith-Waterman, affine gaps (Gotoh) | Alignment |
| 3 | matrix-chain, optimal BST (+ Knuth O(n^2)) | IntervalDP |
| 3 | TSP (Held-Karp), Hamiltonian path, set cover | Tsp, SetCover |
| 3 | tree DP: diameter, rerooting, centroid, independent set | TreeDP |
| 3 | SOS DP, greedy-vs-DP coin example | SubsetDP |
| 4 | Edmonds-Karp, Dinic, min-cut, min-cost flow (SSP) | FlowNetwork |
| 4 | bipartite matching + Koenig cover, project selection | FlowApps |
| 5 | vertex cover: exact, 2-approx, FPT + kernel | VertexCover |
| 5 | knapsack exact / greedy / 1/2-approx / FPTAS, subset sum | Knapsack |
| 5 | TSP nearest-neighbour and MST 2-approximation | Tsp |
| 6 | Miller-Rabin (witnesses, Carmichael demo) | MillerRabin |
| 6 | randomised quicksort | RandomizedSort |
| 6 | universal + FKS perfect hashing | PerfectHash |
| 6 | reservoir sampling | Reservoir |
| 6 | parallel reduce, Blelloch scan, fork-join merge sort, work/span/Brent | ParallelOps |

## API
Every lab is a GET endpoint (the page shows the exact URL under each result). The original endpoints are unchanged:
`/api/health`, `/api/search`, `/api/edit-distance`, `/api/suffix`.

## Notes
- The original zip contained only compiled `.class` files, so this source is a rewrite that keeps the same classes,
  endpoints and 15 sample records. `web/index.html` was not in that zip, so the page is new.
- Parallel speed-ups depend on your core count and the page shows it. On small inputs threads often lose to one thread.
