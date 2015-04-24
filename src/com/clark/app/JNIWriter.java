package com.clark.app;

import org.apache.commons.io.IOUtils;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.STMessage;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by clark on 14-9-28.
 */
public class JNIWriter {
    private List<JavaClass> javaClasses;
    private boolean isCpp;
    private File outputDir;

    public JNIWriter(List<JavaClass> javaClasses, boolean isCpp, File outputDir) {
        this.javaClasses = javaClasses;
        this.isCpp = isCpp;
        this.outputDir = outputDir;
    }

    public void render() throws IOException {
        File source = new File(outputDir, isCpp ? "entry.cpp" : "entry.c");
        ST st = new ST(newStringTemplate("entry.st"));
        st.add("functionDeclares", new FunctionDeclares(isCpp));
        st.add("isCpp", isCpp);
        st.add("functionRegisters", new FunctionRegisters());
        st.write(source, new MyStErrorListener());
    }

    private static String newStringTemplate(String resource) {
        try {
            return IOUtils.toString(JNIWriter.class.getClassLoader().getResourceAsStream(
                    "com/clark/app/" + resource));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class FunctionRegisters {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (JavaClass aClass : javaClasses) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(printRegisterBlock(aClass));
            }
            return sb.toString();
        }

        private String printRegisterBlock(JavaClass aClass) {
            ST st = new ST(newStringTemplate("class_register.st"));
            st.add("isCpp", isCpp);
            st.add("class_name", aClass.javaName);
            st.add("method_count", aClass.jniMethods.size());
            st.add("register_blocks", new ClassRegisterBlock(aClass));
            return st.render();
        }

        private class ClassRegisterBlock {
            private JavaClass aClass;

            private ClassRegisterBlock(JavaClass aClass) {
                this.aClass = aClass;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                for (JNIMethod m : aClass.jniMethods) {
                    if (sb.length() > 3) {
                        sb.append("\n");
                    }
                    final ST st = new ST(newStringTemplate("register_block.st"));
                    st.add("isCpp", isCpp);
                    st.add("name", m.javaName);
                    st.add("desc", m.signature);
                    st.add("nativeName", m.jniFuncName);
                    sb.append(st.render());
                }
                return sb.toString();
            }
        }
    }

    private class FunctionDeclares {
        private boolean isCpp;

        public FunctionDeclares(boolean isCpp) {
            this.isCpp = isCpp;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (JavaClass aClass : javaClasses) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(printClassBlock(aClass));
            }
            return sb.toString();
        }

        private String printClassBlock(JavaClass aClass) {
            StringBuilder sb = new StringBuilder();
            sb.append("/********** ").append(aClass.javaName).append(" **********/");
            for (JNIMethod m : aClass.jniMethods) {
                sb.append("\n");
                if (isCpp) {
                    sb.append("extern \"C\" ");
                }
//                sb.append("JNIEXPORT ");
                sb.append(m.nativeReturnName);
//                sb.append(" JNICALL");
                sb.append(" ");
                sb.append(m.jniFuncName);
                sb.append("(");
                sb.append("JNIEnv*, ");
                sb.append(m.isStatic ? "jclass" : "jobject");
                if (m.nativeParamNames.size() > 0) {
                    for (String p : m.nativeParamNames) {
                        sb.append(", ").append(p);
                    }
                }
                sb.append(");");
            }

            return sb.toString();
        }
    }
}

class MyStErrorListener implements STErrorListener {
    @Override
    public void compileTimeError(STMessage stMessage) {
        System.out.println(stMessage);
    }

    @Override
    public void runTimeError(STMessage stMessage) {
        System.out.println(stMessage);
    }

    @Override
    public void IOError(STMessage stMessage) {
        System.out.println(stMessage);
    }

    @Override
    public void internalError(STMessage stMessage) {
        System.out.println(stMessage);
    }
}
