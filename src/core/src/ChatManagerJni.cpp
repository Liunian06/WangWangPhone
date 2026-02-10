#include <jni.h>
#include <string>
#include "ChatManager.h"
#include "ResponseParser.h"

// 简单的辅助函数：std::string -> jstring
jstring stdToJString(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

// jstring -> std::string
std::string jStringToStd(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string str(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return str;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_WangWangPhone_core_ChatManager_initialize(JNIEnv* env, jobject thiz) {
    return (jboolean)wwj_core::ChatManager::getInstance().initialize();
}

JNIEXPORT jstring JNICALL
Java_com_WangWangPhone_core_ChatManager_getAllSessionsJson(JNIEnv* env, jobject thiz) {
    // 这里应将 vector<ChatSession> 转换为 JSON string
    // 为了 MVP，我们目前返回一个空的 JSON 数组，后续接入 JSON 库后补全
    return stdToJString(env, "[]");
}

JNIEXPORT jstring JNICALL
Java_com_WangWangPhone_core_ChatManager_getMessagesJson(JNIEnv* env, jobject thiz, jstring session_id, jint limit, jint offset) {
    return stdToJString(env, "[]");
}

JNIEXPORT jstring JNICALL
Java_com_WangWangPhone_core_ChatManager_parseResponse(JNIEnv* env, jobject thiz, jstring response, jboolean enable_extended, jboolean enable_emoji) {
    std::string res = jStringToStd(env, response);
    auto items = wwj_core::ResponseParser::parse(res, (bool)enable_extended, (bool)enable_emoji);
    
    // 转换为 JSON 数组格式: [{"type": 0, "content": "..."}]
    std::string json = "[";
    for (size_t i = 0; i < items.size(); ++i) {
        json += "{\"type\":" + std::to_string(items[i].type) + ",\"content\":\"" + items[i].content + "\"}";
        if (i < items.size() - 1) json += ",";
    }
    json += "]";
    
    return stdToJString(env, json);
}

// 其余方法暂留空实现...

}
