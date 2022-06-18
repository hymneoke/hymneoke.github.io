package com.solafide.util;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import lombok.SneakyThrows;

public class TestUtility {
    @SneakyThrows
    public static File findTestResource(String path) {
        URL resource = TestUtility.class.getClassLoader().getResource(path);
        if(resource == null) {
            return null;
        }
        return new File(URLDecoder.decode(resource.getPath(), "UTF-8"));
    }

    @SneakyThrows
    public static List<String> readLinesFor(Path path) {
        return Files.readAllLines(path);
    }

    @SneakyThrows
    public static List<Path> walkRegularFilesFor(Path h) {
        return Files.walk(h)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
    }

    public static List<Path> gatherNoteworthyFilesFor(List<Path> i) {
        return i.stream()
                .parallel()
                .filter(p -> p.getFileName().toString().endsWith(".nwctxt"))
                .collect(Collectors.toList());
    }
}
