package ai.skaile.mcpo.ppt.tooling.infra;

/**
 * PowerPoint unit conversions. PowerPoint's native persistence unit is the EMU
 * (English Metric Unit) — {@code 914400} EMU per inch, {@code 12700} EMU per
 * point. Tool schemas in this server accept <strong>points</strong> at their
 * boundary; every raw {@code 914400} / {@code 12700} constant must live inside
 * this class so units stay explicit and auditable.
 *
 * <p>Java2D rendering operates in pixels; conversions between points and
 * pixels depend on the target DPI (typically 72 or 96).
 */
public final class PptUnits {

    /** EMU per inch. */
    public static final int EMU_PER_INCH = 914_400;

    /** EMU per point (72 points per inch). */
    public static final int EMU_PER_POINT = EMU_PER_INCH / 72;

    /** Default screen DPI for render-related conversions. */
    public static final double DEFAULT_RENDER_DPI = 96.0;

    /** Typographic DPI — 72 pt per inch. */
    public static final double POINTS_PER_INCH = 72.0;

    private PptUnits() {
    }

    public static long pointsToEmu(double points) {
        return Math.round(points * EMU_PER_POINT);
    }

    public static double emuToPoints(long emu) {
        return ((double) emu) / EMU_PER_POINT;
    }

    public static double pointsToPixels(double points, double dpi) {
        requirePositive(dpi, "dpi");
        return points * dpi / POINTS_PER_INCH;
    }

    public static double pixelsToPoints(double pixels, double dpi) {
        requirePositive(dpi, "dpi");
        return pixels * POINTS_PER_INCH / dpi;
    }

    public static long inchesToEmu(double inches) {
        return Math.round(inches * EMU_PER_INCH);
    }

    public static double emuToInches(long emu) {
        return ((double) emu) / EMU_PER_INCH;
    }

    private static void requirePositive(double value, String name) {
        if (!(value > 0)) {
            throw new IllegalArgumentException(name + " must be positive, got " + value);
        }
    }
}
