package agriflow.model;

/** One row of the AgriFlow knowledge base. */
public class FarmRecord {
    private final int id;
    private final String crop, disease, soil, fertilizer, water, season, practice, description;

    public FarmRecord(int id, String crop, String disease, String soil, String fertilizer,
                      String water, String season, String practice, String description) {
        this.id = id; this.crop = crop; this.disease = disease; this.soil = soil; this.fertilizer = fertilizer;
        this.water = water; this.season = season; this.practice = practice; this.description = description;
    }

    public int getId() { return id; }
    public String getCrop() { return crop; }
    public String getDisease() { return disease; }
    public String getSoil() { return soil; }
    public String getFertilizer() { return fertilizer; }
    public String getWater() { return water; }
    public String getSeason() { return season; }
    public String getPractice() { return practice; }
    public String getDescription() { return description; }

    /** everything about the record as one string - this is what the string algorithms search */
    public String searchableText() {
        return crop + " " + disease + " " + soil + " " + fertilizer + " " + water + " " + season + " " + practice + " " + description;
    }
}
