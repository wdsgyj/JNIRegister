#include \<jni.h>

<functionDeclares>

jint JNI_OnLoad (JavaVM *vm, void *reserved)
{
    JNIEnv* env;
    <if(isCpp)>vm->GetEnv((void**) &env, JNI_VERSION_1_6);<else>(*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6);<endif>

    <functionRegisters>

    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
}