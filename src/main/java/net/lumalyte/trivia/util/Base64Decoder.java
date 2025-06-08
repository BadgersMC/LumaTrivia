package net.lumalyte.trivia.util;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class Base64Decoder {
    public static String decode(String base64) {
        return new String(Base64.getDecoder().decode(base64));
    }

    public static List<String> decodeList(List<String> base64List) {
        return base64List.stream()
            .map(Base64Decoder::decode)
            .collect(Collectors.toList());
    }
} 