package com.ie.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Word {

    @JsonProperty("r")
    private int row;
    @JsonProperty("c")
    private int col;
    @JsonProperty("v")
    private int vertical;
    @JsonProperty("w")
    private String word;
    @JsonProperty("h")
    private String clue;

    public Word(String word, String clue) {
        this.word = word;
        this.clue = clue;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getVertical() {
        return vertical;
    }

    public void setVertical(int vertical) {
        this.vertical = vertical;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getClue() {
        return clue;
    }

    public void setClue(String clue) {
        this.clue = clue;
    }

    @Override
    public String toString() {
        return "{" +
                "row=" + row +
                ", col=" + col +
                ", vertical=" + vertical +
                ", word='" + word + '\'' +
//                ", clue='" + clue + '\'' +
                '}';
    }
}