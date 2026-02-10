#ifndef RESPONSE_PARSER_H
#define RESPONSE_PARSER_H

#include <string>
#include <vector>
#include <map>
#include "ChatManager.h"

namespace wwj_core {

struct ParsedItem {
    int type; // MessageType as int
    std::string content;
};

class ResponseParser {
public:
    static std::vector<ParsedItem> parse(
        const std::string& response,
        bool enableExtendedChat = true,
        bool enableEmoji = true
    );

private:
    static std::string preprocess(const std::string& input);
    static bool tryParseJson(const std::string& input, std::vector<ParsedItem>& outItems);
    static bool tryParseXml(const std::string& input, std::vector<ParsedItem>& outItems);
    
    static int normalizeType(const std::string& typeStr);
    static std::string trim(const std::string& s);
};

} // namespace wwj_core

#endif // RESPONSE_PARSER_H
