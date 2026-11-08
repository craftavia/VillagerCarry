package net.craftavia.villagercarry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFilesTest {

    private static Properties load(final String name) throws IOException {
        try (InputStream in = MessageFilesTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, name);
            final Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }

    @Test
    void bundledLanguagesHaveTheSameKeys() throws IOException {
        final Set<String> english = new TreeSet<>(load("messages_en.properties").stringPropertyNames());
        final Set<String> turkish = new TreeSet<>(load("messages_tr.properties").stringPropertyNames());
        assertEquals(english, turkish);
    }

    @Test
    void leadingSpacesSurvive() throws IOException {
        assertTrue(load("messages_en.properties").getProperty("lore-trade-info").startsWith("   "));
    }
}
