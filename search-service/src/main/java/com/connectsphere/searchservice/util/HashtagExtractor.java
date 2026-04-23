package com.connectsphere.searchservice.util;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HashtagExtractor {

    // ✅ Regex for hashtags
    // Matches: #java, #SpringBoot, #AI2026
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

    public Set<String> extract(String content) {

        Set<String> hashtags = new HashSet<>();

        if (content == null || content.isEmpty()) {
            return hashtags;
        }

        Matcher matcher = HASHTAG_PATTERN.matcher(content);

        while (matcher.find()) {
            String tag = matcher.group(1); // without #

            // Normalize to lowercase
            hashtags.add(tag.toLowerCase());
        }

        return hashtags;
    }
}