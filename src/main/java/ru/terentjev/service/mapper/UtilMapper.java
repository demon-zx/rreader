package ru.terentjev.service.mapper;

public class UtilMapper {

    public static String trim(String source) {
        if (source == null) {
            return null;
        }
        return source.trim();
    }

}