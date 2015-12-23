#include "com_romide_terminal_jni_KivaTerminal.h"
#include <unistd.h>
#include <stdlib.h>

/*
 * Class:     com_romide_terminal_jni_KivaTerminal
 * Method:    setenv
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_com_romide_terminal_jni_KivaTerminal_setenv
(JNIEnv *env, jclass cls, jstring jname, jstring jval, jint replace) {

	const char *name_char = env->GetStringUTFChars(jname, JNI_FALSE);
	const char *value_char =  env->GetStringUTFChars(jval, JNI_FALSE);

	setenv(name_char, value_char, replace);

	env->ReleaseStringUTFChars(jname, name_char);
	env->ReleaseStringUTFChars(jval, value_char);
}

