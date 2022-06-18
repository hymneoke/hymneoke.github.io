package com.solafide.service;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import lombok.SneakyThrows;

public class HymnalService {
    public static final String NWC2TXT = "C:/Program Files (x86)/Noteworthy Software/NoteWorthy Composer 2/nwc-conv.exe";

    private final Path inputDirectory;
    private final Path outputDirectory;

    public static void main(String[] args) {
        String projectDirectory = args[0];
        new HymnalService(
                Paths.get(projectDirectory, "hymnals").toString(),
                Paths.get(projectDirectory, "pilot").toString())
                .deployHymnals();
    }

    public HymnalService(String inputDirectory, String outputDirectory) {
        this.inputDirectory = Paths.get(inputDirectory);
        this.outputDirectory = Paths.get(outputDirectory);
    }

    @SneakyThrows
    public void deployHymnals() {
        File outputDirectoryFile = outputDirectory.toFile();
        FileUtils.deleteQuietly(outputDirectoryFile);
        if(!outputDirectoryFile.mkdirs()) {
            throw new RuntimeException("Output directory not created");
        }
        List<Path> inputFilePaths = Files.walk(inputDirectory)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
        inputFilePaths.stream()
                .parallel()
                .filter(p -> p.getFileName().toString().endsWith(".nwctxt"))
                .forEach(this::convertToMidi);
        inputFilePaths.stream()
                .parallel()
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .forEach(this::copy);
    }

    @SneakyThrows
    private void copy(Path filePath) {
        File existingFile = filePath.toFile();
        Path copyPath = Paths.get(outputDirectory.toString(), stripInputPathFrom(existingFile));
        Files.createDirectories(copyPath.getParent());
        Files.copy(filePath, copyPath, REPLACE_EXISTING);
    }

    @SneakyThrows
    private void convertToMidi(Path nwcTxtPath) {
        File nwcTxtFile = nwcTxtPath.toFile();
        Path midiFilePath = createMidiFilePathFor(nwcTxtFile);
        Files.createDirectories(midiFilePath.getParent());
        ProcessBuilder processBuilder = new ProcessBuilder(NWC2TXT,
                "\"" + nwcTxtFile.getPath() + "\"", "\"" + midiFilePath + "\"");
        Process process = processBuilder.start();
        process.waitFor();
    }

    public Path createMidiFilePathFor(File existingFile) {
        String strippedBasePath = stripInputPathFrom(existingFile);
        return Paths.get(outputDirectory.toString(),
                FilenameUtils.getPath(strippedBasePath),
                FilenameUtils.removeExtension(existingFile.getName()) + ".mid");
    }

    private String stripInputPathFrom(File existingFile) {
        return existingFile.getAbsolutePath().replace(inputDirectory.toAbsolutePath().toString(), "");
    }
}