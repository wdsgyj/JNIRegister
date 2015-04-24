package com.clark.app;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by clark on 14-9-28.
 */
public class JNICollector {
    private List<File> jarFiles;
    private List<File> classFiles;

    private List<JavaClass> javaClasses = new LinkedList<JavaClass>();
    private HashSet<String> ignoreClassesPre = new HashSet<String>();

    public JNICollector(List<File> jarFiles, List<File> classFiles, String[] ignoreClasses) {
        this.jarFiles = jarFiles;
        this.classFiles = classFiles;

        if (ignoreClasses != null) {
            for (String s : ignoreClasses) {
                ignoreClassesPre.add(s.replace('.', '/'));
            }
        }
    }

    public void process() throws IOException {
        if (jarFiles != null) {
            for (File zipFile : jarFiles) {
                processZipFile(javaClasses, zipFile, ignoreClassesPre);
            }
        }

        if (classFiles != null) {
            for (File f : classFiles) {
                processClassFile(javaClasses, new FileInputStream(f), ignoreClassesPre);
            }
        }
    }

    public List<JavaClass> getJavaClasses() {
        return javaClasses;
    }

    private static void processZipFile(List<JavaClass> javaClasses, File zipFile, Set<String> ignoreClassesPre) throws IOException {
        ZipFile zip = new ZipFile(zipFile);
        try {
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                final String name = zipEntry.getName();
                if (name.endsWith(".class")) {
                    processClassFile(javaClasses, zip.getInputStream(zipEntry), ignoreClassesPre);
                }
            }
        } finally {
            zip.close();
        }
    }

    private static void processClassFile(List<JavaClass> javaClasses, InputStream inputStream, Set<String> ignoreClassesPre) throws IOException {
        if (inputStream == null) return;

        try {
            ClassReader classReader = new ClassReader(inputStream);
            final MyClassVisitor classVisitor = new MyClassVisitor(javaClasses, ignoreClassesPre);
            classReader.accept(classVisitor, ClassReader.SKIP_CODE
                    | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } finally {
            inputStream.close();
        }
    }
}

final class MyClassVisitor extends ClassVisitor {

    private List<JavaClass> javaClasses;

    private JavaClass javaClass = new JavaClass();
    private List<JNIMethod> jniMethods = new LinkedList<JNIMethod>();

    private HashSet<String> methodNames = new HashSet<String>();
    private HashSet<String> overLoadMethodNames = new HashSet<String>();

    private Set<String> ignoreClassesPre;
    private boolean ignore;

    MyClassVisitor(List<JavaClass> javaClasses, Set<String> ignoreClassesPre) {
        super(Opcodes.ASM5);
        this.javaClasses = javaClasses;
        this.ignoreClassesPre = ignoreClassesPre;
    }

    @Override
    public void visit(int i, int i2, String s, String s2, String s3, String[] strings) {
        javaClass.javaName = s;
        for (String pre : ignoreClassesPre) {
            if (s.startsWith(pre)) {
                ignore = true;
                break;
            }
        }
    }

    @Override
    public void visitEnd() {
        if (!ignore && jniMethods.size() > 0) {
            // 计算重载方法
            ensureOverloadMethod(jniMethods, overLoadMethodNames);
            for (JNIMethod m : jniMethods) {
                // 计算 JNI 函数的名字
                getJniFunctionName(javaClass.javaName, m);
                // 计算 JNI 参数列表以及返回值类型
                NativeType.getNativeNames(m);
            }
            javaClass.jniMethods = jniMethods;
            javaClasses.add(javaClass);
        }
    }

    @Override
    public MethodVisitor visitMethod(int i, String s, String s2, String s3, String[] strings) {
        if (!ignore && (Opcodes.ACC_NATIVE & i) != 0) {
            JNIMethod method = new JNIMethod();
            method.javaName = s;
            method.signature = s2;
            method.isStatic = (Opcodes.ACC_STATIC & i) != 0;
            if (methodNames.contains(s)) {
                overLoadMethodNames.add(s);  // 该名字的方法是重载方法
            } else {
                methodNames.add(s); // 添加方法的名字，以便确定后续方法是否为重载方法
            }
            jniMethods.add(method);
        }
        return null;
    }

    private static void ensureOverloadMethod(List<JNIMethod> jniMethods, Set<String> overLoadNames) {
        for (JNIMethod m : jniMethods) {
            if (overLoadNames.contains(m.javaName)) {
                m.isOverload = true;
            } else {
                m.isOverload = false;
            }
        }
    }

    private static String getJniFunctionName(String className, JNIMethod method) {
        StringBuilder builder = new StringBuilder("Java_");
        builder.append(convertJNIName(className));
        builder.append("_");
        builder.append(convertJNIName(method.javaName));
        if (method.isOverload) {
            builder.append("__");
            String paramList = method.signature.substring(method.signature.indexOf('(') + 1,
                    method.signature.indexOf(')'));
            if (paramList.length() > 0) {
                builder.append(convertJNIName(paramList));
            }
        }
        method.jniFuncName = builder.toString();
        return method.jniFuncName;
    }

    private static String convertJNIName(String javaName) {
        char[] javaNameChs = javaName.toCharArray();
        StringBuilder builder = new StringBuilder();
        for (char c : javaNameChs) {
            switch (c) {
                case '/':
                    builder.append("_");
                    break;

                case '_':
                    builder.append("_1");
                    break;

                case ';':
                    builder.append("_2");
                    break;

                case '[':
                    builder.append("_3");
                    break;

                default:
                    builder.append(c);
                    break;
            }
        }
        return builder.toString();
    }
}

final class JavaClass {
    public String javaName;
    public List<JNIMethod> jniMethods = new LinkedList<JNIMethod>();
}

final class JNIMethod {
    public String javaName;
    public String signature;
    public String jniFuncName;
    public boolean isOverload; // 是否为重载方法
    public boolean isStatic;   // 是否为静态方法

    public List<String> nativeParamNames = new LinkedList<String>();
    public String nativeReturnName;
}

final class NativeType {
    private static final HashMap<String, String> nativeTypeMap = new HashMap<String, String>();

    static {
        nativeTypeMap.put("Z", "jboolean");
        nativeTypeMap.put("C", "jchar");
        nativeTypeMap.put("B", "jbyte");
        nativeTypeMap.put("S", "jshort");
        nativeTypeMap.put("I", "jint");
        nativeTypeMap.put("J", "jlong");
        nativeTypeMap.put("F", "jfloat");
        nativeTypeMap.put("D", "jdouble");

        nativeTypeMap.put("V", "void");

        nativeTypeMap.put("Ljava/lang/Class;", "jclass");
        nativeTypeMap.put("Ljava/lang/String;", "jstring");
        // jthrowable TODO 暂时使用 jobject 替代
        // jarray 用不到
        nativeTypeMap.put("[Z", "jbooleanArray");
        nativeTypeMap.put("[B", "jbyteArray");
        nativeTypeMap.put("[C", "jcharArray");
        nativeTypeMap.put("[S", "jshortArray");
        nativeTypeMap.put("[I", "jintArray");
        nativeTypeMap.put("[J", "jlongArray");
        nativeTypeMap.put("[F", "jfloatArray");
        nativeTypeMap.put("[D", "jdoubleArray");
        // jobjectArray
    }

    private static String getNativeTypeName(String signatureName) {
        String value = nativeTypeMap.get(signatureName);
        if (value != null) {
            return value;
        }

        if (signatureName.startsWith("[")) {
            return "jobjectArray";
        }

        if (signatureName.startsWith("L")) {
            return "jobject";
        }

        throw new IllegalStateException("Can't reach here!");
    }

    private static void parseParamList(List<String> list, char[] buf) {
        int start = 0, end = 0;
        char ch;
        do {
            ch = buf[end];
            switch (ch) {
                case 'Z':
                case 'C':
                case 'B':
                case 'S':
                case 'I':
                case 'F':
                case 'J':
                case 'D': {
                    end++;
                    list.add(getNativeTypeName(new String(buf, start, end - start)));
                    start = end;
                    break;
                }

                case 'L': {
                    do {
                        end++;
                    } while (buf[end] != ';');

                    end++;
                    list.add(getNativeTypeName(new String(buf, start, end - start)));
                    start = end;
                    break;
                }

                case '[': {
                    end++;
                    break;
                }

                default: {
                    throw new IllegalStateException("Unknown char: " + ch);
                }
            }
        } while (end < buf.length);
    }

    public static void getNativeNames(JNIMethod method) {
        final String desc = method.signature;

        int paramStartIndex = desc.indexOf('(') + 1;
        int paramEndIndex = desc.indexOf(')');

        if (paramStartIndex < paramEndIndex) {
            char[] buf = desc.substring(paramStartIndex, paramEndIndex).toCharArray();
            parseParamList(method.nativeParamNames, buf);
        }

        method.nativeReturnName = getNativeTypeName(desc.substring(paramEndIndex + 1));
    }
}