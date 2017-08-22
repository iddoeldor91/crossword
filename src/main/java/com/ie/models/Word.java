package com.ie.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class Word {

    @JsonProperty("r")
    @Getter @Setter private int row;
    @JsonProperty("c")
    @Getter @Setter  private int col;
    @JsonProperty("v")
    @Getter @Setter private int vertical;
    @JsonProperty("w")
    @Getter @Setter private String word;
    @JsonProperty("h")
    @Getter @Setter private String clue;

    public Word(String word, String clue) {
        this.word = word;
        this.clue = clue;
    }
}
