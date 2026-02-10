#include "ResponseParser.h"
#include <regex>
#include <algorithm>
#include <iostream>

namespace wwj_core {

std::vector<ParsedItem> ResponseParser::parse(const std::string& response, bool enableExtendedChat, bool enableEmoji) {
    std::string input = preprocess(response);
    std::vector<ParsedItem> items;

    // 1. 尝试 JSON 解析 (这里目前做简单的正则提取，实际应使用 JSON 库)
    if (tryParseJson(input, items)) {
        return items;
    }

    // 2. 尝试 XML 解析
    if (tryParseXml(input, items)) {
        return items;
    }

    // 3. 兜底逻辑：作为纯文本
    ParsedItem fallback;
    fallback.type = (int)MessageType::Words;
    fallback.content = response;
    items.push_back(fallback);
    return items;
}

std::string ResponseParser::preprocess(const std::string& input) {
    std::string s = input;
    // 移除思考标记 <think>...</think>
    s = std::regex_replace(s, std::regex("<think>[\\s\\S]*?</think>"), "");
    // 移除 Markdown 代码块标记
    s = std::regex_replace(s, std::regex("```(?:json|xml)?"), "");
    s = std::regex_replace(s, std::regex("```"), "");
    return trim(s);
}

bool ResponseParser::tryParseJson(const std::string& input, std::vector<ParsedItem>& outItems) {
    // 这是一个非常简单的提取逻辑，仅用于 MVP。
    // 实际项目中应链接 nlohmann/json。
    std::regex itemRegex(R"(\{\s*"type"\s*:\s*"([^"]+)"\s*,\s*"content"\s*:\s*"([^"]+)"\s*\})");
    auto words_begin = std::sregex_iterator(input.begin(), input.end(), itemRegex);
    auto words_end = std::sregex_iterator();

    bool found = false;
    for (std::sregex_iterator i = words_begin; i != words_end; ++i) {
        std::smatch match = *i;
        ParsedItem item;
        item.type = normalizeType(match[1].str());
        item.content = match[2].str();
        outItems.push_back(item);
        found = true;
    }
    return found;
}

bool ResponseParser::tryParseXml(const std::string& input, std::vector<ParsedItem>& outItems) {
    // 简单的标签提取 <type>content</type>
    std::regex tagRegex(R"(<(\w+)>([\s\S]*?)</\1>)");
    auto words_begin = std::sregex_iterator(input.begin(), input.end(), tagRegex);
    auto words_end = std::sregex_iterator();

    bool found = false;
    for (std::sregex_iterator i = words_begin; i != words_end; ++i) {
        std::smatch match = *i;
        ParsedItem item;
        item.type = normalizeType(match[1].str());
        item.content = trim(match[2].str());
        if (!item.content.empty()) {
            outItems.push_back(item);
            found = true;
        }
    }
    return found;
}

int ResponseParser::normalizeType(const std::string& typeStr) {
    std::string s = typeStr;
    std::transform(s.begin(), s.end(), s.begin(), ::tolower);

    if (s == "words" || s == "word" || s == "text" || s == "say") return (int)MessageType::Words;
    if (s == "action" || s == "act") return (int)MessageType::Action;
    if (s == "thought" || s == "think") return (int)MessageType::Thought;
    if (s == "emoji" || s == "sticker") return (int)MessageType::Emoji;
    if (s == "image" || s == "pic") return (int)MessageType::Image;
    if (s == "state") return (int)MessageType::State;
    if (s == "memory") return (int)MessageType::Memory;
    if (s == "moment") return (int)MessageType::Moment;

    return (int)MessageType::Words; // 默认
}

std::string ResponseParser::trim(const std::string& s) {
    auto start = s.begin();
    while (start != s.end() && std::isspace(*start)) start++;
    auto end = s.end();
    do { end--; } while (std::distance(start, end) > 0 && std::isspace(*end));
    return std::string(start, end + 1);
}

} // namespace wwj_core
