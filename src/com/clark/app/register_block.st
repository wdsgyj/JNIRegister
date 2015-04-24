{
    JNINativeMethod* method = methods + (index++);
    method->name = <if(isCpp)>(char*)<endif> "<name>";
    method->signature = <if(isCpp)>(char*)<endif> "<desc>";
    method->fnPtr = <if(isCpp)>(void*)<endif> <nativeName>;
}