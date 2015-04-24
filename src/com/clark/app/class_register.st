{
    jclass cls = <if(isCpp)>env->FindClass("<class_name>");<else>(*env)->FindClass(env, "<class_name>");<endif>
    JNINativeMethod methods[<method_count>];
    int index = 0;

    <register_blocks>

    <if(isCpp)>env->RegisterNatives(cls, methods, index);<else>(*env)->RegisterNatives(env, cls, methods, index);<endif>
    <if(isCpp)>env->DeleteLocalRef(cls);<else>(*env)->DeleteLocalRef(env, cls);<endif>
}