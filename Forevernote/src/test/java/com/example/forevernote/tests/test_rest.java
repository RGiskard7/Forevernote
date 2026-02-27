package com.example.forevernote.tests;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

public class test_rest {
    public static void main(String[] args) throws IOException {
        String id = ".trash/TestFolder";
        Path rootPath = Paths.get("/Users/edu/Library/Mobile Documents/iCloud~md~obsidian/Documents/Obsidian Vault");
        Path srcPath = rootPath.resolve(id);

        System.out.println("srcPath exists: " + Files.exists(srcPath));

        String originalRelativePath = id.substring(7);
        Path targetPath = rootPath.resolve(originalRelativePath);

        System.out.println("targetPath: " + targetPath);

        Files.move(srcPath, targetPath);
        System.out.println("Moved!");
    }
}
