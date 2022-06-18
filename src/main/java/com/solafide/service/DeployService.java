package com.solafide.service;

import org.apache.commons.io.FileUtils;
import java.nio.file.Paths;
import lombok.SneakyThrows;

public class DeployService {
    @SneakyThrows
    public static void main(String[] args) {
        String projectDirectory = args[0];
        FileUtils.copyDirectory(
                Paths.get(projectDirectory, "pilot").toFile(),
                Paths.get(projectDirectory, "production").toFile());
    }
}
