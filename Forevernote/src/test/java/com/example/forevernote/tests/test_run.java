package com.example.forevernote.tests;

import java.nio.file.Path;
import java.nio.file.Paths;

public class test_run {
    public static void main(String[] args) {
        String idFolder = "FolderA";
        String idSubfolder = "FolderA/Subfolder";

        String originalRelativePath = idFolder;
        System.out.println("originalRelPath: " + originalRelativePath);

        originalRelativePath = idSubfolder;
        System.out.println("originalRelPath: " + originalRelativePath);
    }
}
