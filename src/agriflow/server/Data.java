package agriflow.server;

import agriflow.algorithms.LCPArray;
import agriflow.algorithms.SuffixArray;
import agriflow.model.FarmRecord;

/** The AgriFlow knowledge base + the indexes built once at start-up. */
final class Data {
    final FarmRecord[] records = sampleRecords();
    final String corpus;          // all records, lower-case, joined by single spaces
    final int[] recStart;         // where each record starts inside the corpus
    final int[] sa, lcp;          // suffix array + LCP array of the corpus
    final String[] vocab;         // distinct lower-case words (3+ letters), sorted
    final String[] cropsSorted;   // crop names, alphabetical

    // sample farm sites for the routing demo (coordinates are made-up map units, 0..100)
    static final String[] SITE_NAMES = {"Rice farm", "Wheat farm", "Tomato farm", "Cotton farm", "Maize farm", "Groundnut farm",
            "Chilli farm", "Potato farm", "Soybean farm", "Sugarcane farm", "Banana farm", "Turmeric farm"};
    static final int[] SITE_X = {12, 30, 52, 80, 68, 88, 40, 22, 60, 92, 72, 10};
    static final int[] SITE_Y = {70, 20, 40, 15, 62, 48, 80, 45, 90, 82, 28, 10};

    Data() {
        StringBuilder sb = new StringBuilder();
        recStart = new int[records.length];
        for (int i = 0; i < records.length; i++) {
            recStart[i] = sb.length();
            sb.append(records[i].searchableText().toLowerCase()).append(' ');
        }
        corpus = sb.toString();
        sa = SuffixArray.build(corpus);
        lcp = LCPArray.kasai(corpus, sa);
        vocab = buildVocab(corpus);
        cropsSorted = new String[records.length];
        for (int i = 0; i < records.length; i++) cropsSorted[i] = records[i].getCrop();
        sortStrings(cropsSorted);
    }

    /** which record does corpus position `pos` belong to? (binary search on recStart) */
    int recordAt(int pos) {
        int lo = 0, hi = recStart.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (recStart[mid] <= pos) lo = mid; else hi = mid - 1;
        }
        return lo;
    }

    static void sortStrings(String[] a) {
        for (int i = 1; i < a.length; i++) {
            String x = a[i];
            int j = i - 1;
            while (j >= 0 && a[j].compareTo(x) > 0) { a[j + 1] = a[j]; j--; }
            a[j + 1] = x;
        }
    }

    private static String[] buildVocab(String text) {
        String[] words = text.split("[^a-z]+");
        int c = 0;
        for (String w : words) if (w.length() >= 3) c++;
        String[] all = new String[c];
        c = 0;
        for (String w : words) if (w.length() >= 3) all[c++] = w;
        sortStrings(all);
        int u = 0;
        for (int i = 0; i < all.length; i++) if (i == 0 || !all[i].equals(all[i - 1])) all[u++] = all[i];
        String[] out = new String[u];
        System.arraycopy(all, 0, out, 0, u);
        return out;
    }

    private static FarmRecord[] sampleRecords() {
        return new FarmRecord[]{
            new FarmRecord(1, "Rice", "Blast", "Clay", "Urea + NPK", "High", "Kharif", "Flood irrigation",
                "Rice grows well in clay soil and requires careful water management. Blast disease can be reduced through balanced fertilizer and field sanitation."),
            new FarmRecord(2, "Wheat", "Rust", "Loamy", "NPK", "Medium", "Rabi", "Timely irrigation",
                "Wheat prefers well-drained loamy soil. Rust monitoring and balanced fertilizer support healthy crop growth."),
            new FarmRecord(3, "Tomato", "Blight", "Sandy Loam", "Compost + NPK", "Medium", "Rabi", "Drip irrigation",
                "Tomato production benefits from drip irrigation, organic compost and regular monitoring for blight."),
            new FarmRecord(4, "Cotton", "Wilt", "Black Soil", "NPK", "Medium", "Kharif", "Mulching",
                "Cotton in black soil benefits from moisture conservation, balanced fertilizer and disease monitoring."),
            new FarmRecord(5, "Maize", "Leaf Blight", "Loamy", "Urea + NPK", "Medium", "Kharif", "Row planting",
                "Maize needs balanced nutrition and adequate moisture. Leaf blight should be monitored during humid periods."),
            new FarmRecord(6, "Groundnut", "Tikka Disease", "Sandy Loam", "Gypsum + NPK", "Low", "Kharif", "Raised beds",
                "Groundnut performs well in sandy loam. Gypsum and good drainage can support pod development."),
            new FarmRecord(7, "Chilli", "Leaf Curl", "Loamy", "Compost + NPK", "Medium", "Kharif", "Drip irrigation",
                "Chilli needs consistent moisture. Drip irrigation and field sanitation are useful management practices."),
            new FarmRecord(8, "Paddy", "Blast", "Clay", "Urea + NPK", "High", "Kharif", "System of rice intensification",
                "Paddy production requires efficient irrigation, balanced fertilizer and monitoring for blast disease."),
            new FarmRecord(9, "Potato", "Late Blight", "Sandy Loam", "Compost + NPK", "Medium", "Rabi", "Ridge planting",
                "Potato benefits from well-drained soil and careful irrigation. Late blight should be monitored in cool wet conditions."),
            new FarmRecord(10, "Soybean", "Rust", "Black Soil", "NPK", "Medium", "Kharif", "Conservation farming",
                "Soybean benefits from black soil, balanced fertilizer and timely moisture management."),
            new FarmRecord(11, "Sugarcane", "Red Rot", "Loamy", "NPK + Compost", "High", "Annual", "Drip irrigation",
                "Sugarcane requires a long growing period, sufficient water and integrated nutrient management."),
            new FarmRecord(12, "Mustard", "White Rust", "Loamy", "NPK", "Low", "Rabi", "Line sowing",
                "Mustard is a rabi crop with moderate nutrient needs and lower water requirements."),
            new FarmRecord(13, "Banana", "Panama Wilt", "Loamy", "Compost + Potash", "High", "Annual", "Drip irrigation",
                "Banana requires regular water and nutrient supply. Organic matter and drainage are important."),
            new FarmRecord(14, "Turmeric", "Rhizome Rot", "Loamy", "Compost + NPK", "Medium", "Kharif", "Mulching",
                "Turmeric benefits from organic compost, mulch and proper drainage to reduce rhizome rot."),
            new FarmRecord(15, "Pigeon Pea", "Wilt", "Black Soil", "NPK", "Low", "Kharif", "Intercropping",
                "Pigeon pea can perform in black soil with moderate inputs and careful wilt management.")
        };
    }
}
