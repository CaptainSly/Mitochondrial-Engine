package com.duckcraftian.upf.utils;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public class FileUtils {

    private static final int MAX_NAME_LENGTH = 48;
    private static final String EMPTY_NAME = "unnamed";

    // Windows refuses to create files or folders with these names, whatever the extension.
    private static final Set<String> WINDOWS_RESERVED = Set.of(
            "con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    /**
     * Turns any text into a name that is safe to use as a folder or file name on Linux and Windows.
     * <p>
     * Accents are dropped (é becomes e), the text is lowercased, anything outside a-z, 0-9, '_' and '-' becomes '_',
     * runs of '_' collapse into one, leading and trailing '_' and '-' are removed, and the result is cut to 48
     * characters. Text with nothing usable left (for example only symbols, or only non-Latin letters) becomes
     * "unnamed". A name Windows reserves (con, nul, com1...) gets a '_' added. Never returns null or an empty string.
     */
    public static String normalizeText(String text) {
        if (text == null)
            return EMPTY_NAME;

        String name = Normalizer.normalize(text.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^[_-]+|[_-]+$", "");

        if (name.length() > MAX_NAME_LENGTH)
            name = name.substring(0, MAX_NAME_LENGTH).replaceAll("[_-]+$", "");

        if (name.isEmpty())
            return EMPTY_NAME;

        if (WINDOWS_RESERVED.contains(name))
            return name + "_";

        return name;
    }

    public static File getFile(String filePath) {
        return new File(filePath);
    }

    public static String normalizeAssetId(String path) {
        String noExtension = path;
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex != -1)
            noExtension = path.substring(0, dotIndex);

        return noExtension.toLowerCase().replace(' ', '_');
    }

    public static File getLocalFile(String fileName) throws URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(fileName);
        Path filePath = Path.of(url.toURI());
        return filePath.toFile();
    }

    public static File resolveAssetFile(String path) throws URISyntaxException {
        if (Path.of(path).isAbsolute())
            return new File(path);
        return getLocalFile(path);
    }
}