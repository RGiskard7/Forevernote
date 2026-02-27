package com.example.forevernote.tests;

import java.nio.file.Path;
import java.nio.file.Paths;

public class test_empty_id {
    public static void main(String[] args) {
        String originalRelativePath = "folderA";
        String id = ".trash/folderA";

        if (id.startsWith(".trash/")) {
            originalRelativePath = id.substring(7);
        } else if (id.equals(".trash")) {
            System.out.println("is .trash");
        } else if (id.startsWith(".trash")) {
            originalRelativePath = id.substring(6);
            if (originalRelativePath.startsWith("/") || originalRelativePath.startsWith("\\")) {
                originalRelativePath = originalRelativePath.substring(1);
            }
        }
        System.out.println("originalRelPath: " + originalRelativePath);
        Path rootPath = Paths.get("/Users/edu/Library/Mobile Documents");
        Path targetPath = rootPath.resolve(originalRelativePath);
        System.out.println("targetPath parent: " + targetPath.getParent());
        if (targetPath.getParent() == null) {
            System.out.println("parent is null, path length: " + targetPath.toString().length());
        }
    }
}
