package com.maximys.nexus.client.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

// Информация об обновления из GitHub Releases API

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubReleaseInfo {
    
    @JsonProperty("tag_name")
    private String tagName; // Например, "v0.3.0"

    @JsonProperty("body")
    private String body; // Текст описания релиза (Changelog)

    private List<Map<String, Object>> skippedReleases;
}
