package com.solafide.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.solafide.util.TestUtility;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class HymnalServiceTest {
    private HymnalService hymnalService;

    private static final Path outputDirectory = Paths.get(".", "build", "hymnal-output");

    @BeforeEach
    void setUp() {
        hymnalService = new HymnalService(
                Paths.get(".", "src", "test", "resources", "input-hymnals").toString(),
                outputDirectory.toString());
    }

    @Test
    void deployHymnalsConvertsTunes() {
        hymnalService.deployHymnals();
        File mightyFortress = TestUtility.findTestResource("expected-hymnals/hymnal/0/A Mighty Fortress Is Our God.mid");
        File threeKings = TestUtility.findTestResource("expected-hymnals/hymnal/0/3kingsco___PaterOmnium.mid");
        File veniVeniEmmanuel = TestUtility.findTestResource("expected-hymnals/hymnal/1/1___VENI, VENI, EMMANUEL.mid");
        assertMidiEquals(mightyFortress, "0/A Mighty Fortress Is Our God.mid");
        assertMidiEquals(threeKings, "0/3kingsco___PaterOmnium.mid");
        assertMidiEquals(veniVeniEmmanuel, "1/1___VENI, VENI, EMMANUEL.mid");
    }

    @Test
    void deployHymnalsCopiesJson() {
        hymnalService.deployHymnals();
        File hymnalsJson = TestUtility.findTestResource("expected-hymnals/hymnals.json");
        File hymnal0Json = TestUtility.findTestResource("expected-hymnals/hymnal/0/hymnal.json");
        File hymnal1Json = TestUtility.findTestResource("expected-hymnals/hymnal/1/hymnal.json");
        assertJsonEquals(hymnalsJson, "hymnals.json");
        assertJsonEquals(hymnal0Json, "hymnal/0/hymnal.json");
        assertJsonEquals(hymnal1Json, "hymnal/1/hymnal.json");
    }

    @SneakyThrows
    private void assertMidiEquals(File expectedFile, String actualPath) {
        assertFilesEqual(expectedFile, new File(Paths.get(outputDirectory.toString(),
                "hymnals", "hymnal", actualPath).toString()));
    }

    @SneakyThrows
    private void assertJsonEquals(File expectedFile, String actualPath) {
        assertFilesEqual(expectedFile, new File(Paths.get(outputDirectory.toString(),
                "hymnals", actualPath).toString()));
    }

    private void assertFilesEqual(File expectedFile, File actualFile) throws IOException {
        assertEquals(FileUtils.readFileToString(expectedFile, "utf-8"),
                FileUtils.readFileToString(actualFile, "utf-8"));
    }
}