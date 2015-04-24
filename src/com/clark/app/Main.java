package com.clark.app;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by clark on 14-9-28.
 */
public class Main {
    public static void main(String[] args) throws ParseException, IOException {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("jar", true, "Java jar package file");
        options.addOption("class", true, "Java class file or dir");
        options.addOption("o", true, "Output directory");
        options.addOption("cpp", false, "Support C++ source");
        options.addOption("ignore", true, "which java class should be ignore");
        CommandLine cli = parser.parse(options, args);

        final String[] jars = cli.getOptionValues("jar");
        final String[] classes = cli.getOptionValues("class");
        final String outputDir = cli.getOptionValue("o", "jni");
        final String[] ignoreClasses = cli.getOptionValues("ignore");

        List<File> jarFiles = new LinkedList<File>();
        List<File> classFiles = new LinkedList<File>();
        if (jars != null) {
            for (String s : jars) {
                jarFiles.add(new File(s));
            }
        }
        if (classes != null) {
            for (String s : classes) {
                findClassFiles(new File(s), classFiles);
            }
        }

        JNICollector collector = new JNICollector(jarFiles, classFiles, ignoreClasses);
        collector.process();

        final File dir = new File(outputDir);
        dir.mkdirs();

        if (!dir.isDirectory()) {
            System.err.println("Can not create directory [" + dir.getAbsolutePath() + "]");
            System.exit(1);
        }

        JNIWriter writer = new JNIWriter(collector.getJavaClasses(), cli.hasOption("cpp"), dir);
        writer.render();

        System.out.println("Success!");
    }

    private static void findClassFiles(File file, List<File> out) {
        if (file == null) return;

        if (file.isFile()) {
            out.add(file);
            return;
        }

        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            LinkedList<File> files = new LinkedList<File>();
            if (listFiles != null) {
                files.addAll(Arrays.asList(listFiles));
            }
            while (files.size() > 0) {
                File f = files.pollFirst();
                String name = f.getName();
                if (f.isFile() && name.endsWith(".class")) {
                    out.add(f);
                } else if (f.isDirectory()) {
                    listFiles = f.listFiles();
                    if (listFiles != null) {
                        files.addAll(Arrays.asList(listFiles));
                    }
                }
            }
        }

    }
}
