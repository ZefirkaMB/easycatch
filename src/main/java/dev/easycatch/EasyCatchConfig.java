package dev.easycatch;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Simple config: config/easycatch.properties (created on first launch).
 * Restart the game after editing it.
 */
public final class EasyCatchConfig {
    public static boolean enabled = true;
    /** 1.0 = vanilla. 2.0 = pearl hitbox twice as wide. */
    public static double pearlHitboxScale = 2.0;
    /** 1.0 = vanilla. */
    public static double windChargeHitboxScale = 2.0;

    static {
        load();
    }

    private EasyCatchConfig() {
    }

    private static double parse(Properties p, String key, double def) {
        try {
            double v = Double.parseDouble(p.getProperty(key, String.valueOf(def)).trim());
            return Math.max(1.0, Math.min(10.0, v));
        } catch (Exception e) {
            return def;
        }
    }

    private static void load() {
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("easycatch.properties");
            Properties p = new Properties();
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    p.load(in);
                }
            }
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true").trim());
            pearlHitboxScale = parse(p, "pearlHitboxScale", 2.0);
            windChargeHitboxScale = parse(p, "windChargeHitboxScale", 2.0);

            p.setProperty("enabled", String.valueOf(enabled));
            p.setProperty("pearlHitboxScale", String.valueOf(pearlHitboxScale));
            p.setProperty("windChargeHitboxScale", String.valueOf(windChargeHitboxScale));
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "Easy Catch config (1.0 = vanilla, max 10.0). Restart the game after editing.");
            }
        } catch (Exception ignored) {
            // keep defaults
        }
    }
}
