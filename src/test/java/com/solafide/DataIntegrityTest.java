package com.solafide;

import static com.fasterxml.jackson.databind.MapperFeature.USE_ANNOTATIONS;
import static com.solafide.util.TestUtility.readLinesFor;
import static com.solafide.util.TestUtility.walkRegularFilesFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solafide.domain.Hymn;
import com.solafide.domain.Hymnal;
import com.solafide.util.TestUtility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.SneakyThrows;

class DataIntegrityTest {
    private ObjectMapper objectMapper;
    private List<List<Path>> nwcTxtFiles;
    private List<Hymnal> hymnalJsonHymns;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(USE_ANNOTATIONS);
        List<List<Path>> inputFilePaths = Files.list(Paths.get("hymnals", "hymnal"))
                .map(TestUtility::walkRegularFilesFor)
                .collect(Collectors.toList());
        nwcTxtFiles = inputFilePaths.stream()
                .map(TestUtility::gatherNoteworthyFilesFor)
                .collect(Collectors.toList());
        hymnalJsonHymns = Files.list(Paths.get("hymnals", "hymnal"))
                .map(f -> mapToHymnal(Paths.get(f.toString(), "hymnal.json")))
                .collect(Collectors.toList());
    }

    @SneakyThrows
    @Test
    void allHymnalsPresent() {
        List<Hymnal> hymnals = objectMapper.readValue(Paths.get("hymnals", "hymnals.json").toFile(),
                new TypeReference<List<Hymnal>>(){});
        assertEquals(2, hymnals.size());
        Hymnal hymnal0 = hymnals.get(0);
        assertEquals(0, hymnal0.getId());
        assertEquals("The Cyber Hymnal\u2122 (www.hymntime.com/tch)", hymnal0.getTitle());
        Hymnal hymnal1 = hymnals.get(1);
        assertEquals(1, hymnal1.getId());
        assertEquals("Common Service Book of the Lutheran Church", hymnal1.getTitle());
    }

    @Test
    void allHymnsHaveCorrespondingTunes() {
        List<Integer> incompleteHymnals = singletonList(1);
        for(int i = 0; i < hymnalJsonHymns.size(); ++i) {
            if(incompleteHymnals.contains(i)) {
                continue;
            }
            List<Hymn> flatHymns = hymnalJsonHymns.get(i).getHymns();
            List<Path> nwcTxts = nwcTxtFiles.get(i);
            flatHymns.forEach(j ->
                    assertTrue(j.getTunes().stream().anyMatch(t ->
                            nwcTxts.stream().anyMatch(n -> n.getFileName().toString().startsWith(t.getId()))),
                    "JSON did not have corresponding tune: " + j.getId()));
        }
    }

    @Test
    void allTunesHaveCorrespondingHymns() {
        for(int i = 0; i < nwcTxtFiles.size(); ++i) {
            List<Hymn> flatHymns = hymnalJsonHymns.get(i).getHymns();
            List<Path> nwcTxts = nwcTxtFiles.get(i);
            nwcTxts.forEach(n -> {
                String name = n.getFileName().toString().split("\\.")[0];
                assertTrue(flatHymns.stream().anyMatch(j ->
                                j.getTunes().stream().anyMatch(t -> t.getId().equals(name))),
                        "Tune did not have corresponding JSON: " + name);
            });
        }
    }

    @Test
    void allJsonIsMinified() {
        walkRegularFilesFor(Paths.get("hymnals", "hymnal")).stream()
                .parallel()
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .forEach(j -> assertEquals(1, readLinesFor(j).size(), j + " is not minified"));
    }

    @Test
    void tuneFilesHaveLyricIndices() {
        nwcTxtFiles.forEach(t ->
            t.forEach(n -> {
                List<String> lyrics = findLyricsFor(n);
                Optional<Integer> maxLyricIndex = lyrics.stream()
                        .map(l -> {
                            Pattern lyricIndexPattern = Pattern.compile("\\|Lyric(\\d)\\|");
                            Matcher lyricMatcher = lyricIndexPattern.matcher(l);
                            return lyricMatcher.find() ? Integer.parseInt(lyricMatcher.group(1)) : null;
                        })
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo);
                Optional<Integer> maxLyricTextIndex = lyrics.stream()
                        .map(l -> {
                            Pattern lyricIndexPattern = Pattern.compile("(\\d)\\._");
                            Matcher lyricMatcher = lyricIndexPattern.matcher(l);
                            return lyricMatcher.find() ? Integer.parseInt(lyricMatcher.group(1)) : null;
                        })
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo);
                assertTrue(maxLyricIndex.isPresent()
                                && maxLyricTextIndex.isPresent()
                                && maxLyricTextIndex.get() >= maxLyricIndex.get(),
                        "Noteworthy file with incorrect lyric indices: " + n);
            }));
    }

    @Test
    void tunesWithTheSameTuneAndDifferentLyricsHaveSameMeterAndSyllables() {
        List<String> excludedTunes = new ArrayList<>(
                asList("Kum ba Yah.nwctxt", "AnimaChristi.nwctxt"));
        nwcTxtFiles.forEach(nwcTxt -> {
            Map<String, List<Path>> tunesByTuneName = nwcTxt.stream()
                    .filter(n -> {
                        String filename = n.getFileName().toString();
                        return filename.contains("___") && !excludedTunes.contains(filename.split("___")[1]);
                    })
                    .collect(groupingBy(n -> n.getFileName().toString().split("___")[1]));
            tunesByTuneName.forEach((s, p) -> p.forEach(noteworthyFilePath -> {
                List<Integer> syllableCounts = findSyllableCountsFor(noteworthyFilePath);
                long distinctSyllables = syllableCounts.stream().distinct().count();
                if (distinctSyllables > 1) {
                    fail("Noteworthy file with different syllable counts: " + noteworthyFilePath);
                }
            }));
        });
    }

    @Test
    void hyphenationsAreConsistent() {
        Map<String, List<String>> hyphenatedStrings = nwcTxtFiles.stream()
                .flatMap(t -> t.stream()
                        .filter(n -> n.getFileName().toString().contains("___"))
                        .flatMap(n -> findLyricsFor(n).stream()
                                .map(this::findLyricTextFor)
                                .filter(Objects::nonNull)
                                .flatMap(l -> Arrays.stream(findWordsFor(l))))
        ).collect(groupingBy(s -> s.replaceAll("-", "")));
        hyphenatedStrings.forEach((h, s) -> {
            Map<Object, Set<String>> hyphenationsByHyphenCount = s.stream()
                    .collect(groupingBy(hyphenated -> hyphenated.chars().filter(c -> c == '-').count(), toSet()));
            hyphenationsByHyphenCount.forEach((c, s2) ->
                    assertEquals(1, s2.size(), "Noteworthy same syllable hyphenated differently: " + s2));
        });
    }

    @SneakyThrows
    private Hymnal mapToHymnal(Path path) {
        return objectMapper.readValue(path.toFile(), Hymnal.class);
    }

    @SneakyThrows
    private List<String> findLyricsFor(Path noteworthyFilePath) {
        return Files.readAllLines(noteworthyFilePath, UTF_8).stream()
                .filter(l -> {
                    Pattern lyricPattern = Pattern.compile("\\|Lyric\\d\\|Text:\".*\"");
                    return lyricPattern.matcher(l).find();
                })
                .collect(Collectors.toList());
    }

    private String findLyricTextFor(String lyric) {
        Pattern lyricPattern = Pattern.compile("\\|Lyric\\d\\|Text:\"(.*)\"");
        Matcher lyricMatcher = lyricPattern.matcher(lyric);
        return lyricMatcher.find() ? lyricMatcher.group(1) : null;
    }

    private String[] findWordsFor(String lyric) {
        return lyric.replace("\\n", " ").trim().split("[\\s]+");
    }

    private String[] findSyllablesFor(String lyric) {
        return lyric.replace("\\n", " ").trim().split("[-\\s]+");
    }

    private List<Integer> findSyllableCountsFor(Path noteworthyFilePath) {
        return findLyricsFor(noteworthyFilePath).stream()
                .map(l -> findSyllablesFor(l).length)
                .collect(toList());
    }
}
