package com.solafide;

import static com.solafide.util.TestUtility.gatherNoteworthyFilesFor;
import static com.solafide.util.TestUtility.walkRegularFilesFor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.SneakyThrows;

public class SoundfontTest {
    @Test
    void necessarySoundfontPatchesRemainConstant() {
        List<Path> hymnals = walkRegularFilesFor(Paths.get("hymnals", "hymnal"));
        List<Path> nwcTxtFiles = gatherNoteworthyFilesFor(hymnals);
        Set<String> uniquePatches = nwcTxtFiles.stream()
                .map(this::findUniquePatchesFor)
                .flatMap(Set::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        HashSet<String> expectedPatches = new HashSet<>(asList(
                "0", "11", "14", "15", "16", "19", "20", "21", "23", "33", "35", "40", "41", "42",
                "43", "48", "49", "52", "53", "56", "60", "64", "68", "70", "71", "73", "75", "88", "91"));
        assertEquals(expectedPatches, uniquePatches);
    }

    @SneakyThrows
    private Set<String> findUniquePatchesFor(Path noteworthyFilePath) {
        return Files.readAllLines(noteworthyFilePath, UTF_8).stream()
                .map(l -> {
                    Pattern instrumentPattern = Pattern.compile("\\|StaffInstrument\\|.*\\|Patch:(.*?)\\|");
                    Matcher instrumentMatcher = instrumentPattern.matcher(l);
                    return instrumentMatcher.find() ? instrumentMatcher.group(1) : null;
                })
                .collect(Collectors.toSet());
    }
}
