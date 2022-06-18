package com.solafide;

import static com.solafide.util.TestUtility.gatherNoteworthyFilesFor;
import static com.solafide.util.TestUtility.walkRegularFilesFor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import lombok.SneakyThrows;

public class CyberHymnalTest {
    private List<Path> nwcTxtFiles;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        List<Path> cyberHymnal = walkRegularFilesFor(Paths.get("hymnals", "hymnal", "0"));
        nwcTxtFiles = gatherNoteworthyFilesFor(cyberHymnal);
    }

}
