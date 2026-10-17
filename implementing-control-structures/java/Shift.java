/** The three shifts an employee can work each day. */
public enum Shift {
    MORNING("Morning", 'M'),
    AFTERNOON("Afternoon", 'A'),
    EVENING("Evening", 'E');

    private final String label;
    private final char letter;

    Shift(String label, char letter) {
        this.label = label;
        this.letter = letter;
    }

    public String label() {
        return label;
    }

    public char letter() {
        return letter;
    }

    /**
     * Converts user text such as "m", "Morning", "2" or "eve" into a Shift.
     * Returns null when the text is not recognised.
     */
    public static Shift parse(String text) {
        if (text == null) {
            return null;
        }
        // Branching with switch: several spellings map onto the same shift.
        switch (text.trim().toLowerCase()) {
            case "m":
            case "morning":
            case "1":
                return MORNING;
            case "a":
            case "afternoon":
            case "2":
                return AFTERNOON;
            case "e":
            case "eve":
            case "evening":
            case "3":
                return EVENING;
            default:
                return null;
        }
    }
}
