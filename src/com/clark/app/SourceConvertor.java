package com.clark.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by clark on 14-9-28.
 */
public class SourceConvertor {
    private List<File> sourceDirs;
    private File outputDir;

    private String inCharSet;
    private String outCharSet;

    public SourceConvertor(List<File> sourceDirs, File outputDir, String inCharSet, String outCharSet) {
        this.sourceDirs = sourceDirs;
        this.outputDir = outputDir;
        this.inCharSet = inCharSet;
        this.outCharSet = outCharSet;
    }

    public void process() throws IOException {
        for (File srcDir : sourceDirs) {
            final File[] srcs = srcDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".c")
                            || name.endsWith(".C")
                            || name.endsWith(".cpp")
                            || name.endsWith(".cxx")
                            || name.endsWith(".cc")
                            || name.endsWith(".cp")
                            || name.endsWith(".c++")
                            || name.endsWith(".CPP");
                }
            });

            if (srcs == null) {
                continue;
            }

            final File outDir = new File(outputDir, srcDir.getName());
            outDir.mkdirs();
            if (!outDir.isDirectory()) {
                throw new IOException("创建文件夹 " + outDir.getAbsolutePath() + " 失败！");
            }

            for (File src : srcs) {
                copyAndModify(new InputStreamReader(new FileInputStream(src), inCharSet),
                        new OutputStreamWriter(new FileOutputStream(new File(outDir, src.getName())), outCharSet));
            }
        }
    }

    // jlong JNICALL Java_com_baidu_javalite_Backup_sqlite3_1backup_1init(JNIEnv *env,
    private static final Pattern JNI_FUNC_PATTERN = Pattern.compile(".*Java_\\S+.*");
    private static boolean isJNIFaction(String line) {
        return JNI_FUNC_PATTERN.matcher(line).matches();
    }

    private static void copyAndModify(Reader reader, Writer writer) throws IOException {
        BufferedReader bufferedReader = reader instanceof  BufferedReader ?
                (BufferedReader) reader : new BufferedReader(reader);
        BufferedWriter bufferedWriter = writer instanceof BufferedWriter ?
                (BufferedWriter) writer : new BufferedWriter(writer);
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                if (isJNIFaction(line)) {
                    bufferedWriter.append("static ");
                }
                bufferedWriter.append(line);
                bufferedWriter.newLine();
            }
        } finally {
            bufferedReader.close();
            bufferedWriter.close();
        }
    }
}
