package com.example.forevernote.tests;

import java.nio.file.Path;
import java.nio.file.Paths;

public class test_del_fix {
    public static void main(String[] args) {
        String idFolder = "Title";
        Path rootPath = Paths.get("/Users/edu/Library/Mobile Documents");
        Path sourcePath = rootPath.resolve(idFolder);

        Path trashDir = rootPath.resolve(".trash");
        Path targetPath = trashDir.resolve(idFolder);

        System.out.println("originalRelPath: " + idFolder);
        System.out.println("targetPath: " + targetPath);
    }
}
