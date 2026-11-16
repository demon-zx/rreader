package ru.terentjev.controller.model;

import java.util.List;

public record ApiContent(String title, int position, List<ApiContent> children) {
}
