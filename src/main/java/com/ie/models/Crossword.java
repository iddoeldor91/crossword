package com.ie.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


public class Crossword {

    @JsonProperty("l") // l is shortcut for level
    @Getter @Setter private int level = 1;
    @JsonProperty("t") // t is shortcut for time
    @Getter @Setter private int time;
    @JsonProperty("d") // d is shortcut for data
    @Getter @Setter private List<Word> currentWordList = new ArrayList<>();
    @JsonIgnore @Getter @Setter private char[][] grid;
    @JsonIgnore @Getter @Setter private List<Word> availableWords;

    @JsonIgnore @Getter @Setter private int cols = 10; // number of columns todo extract
    @JsonIgnore @Getter @Setter private int rows = 10; // number of rows todo extract
    @JsonIgnore @Getter @Setter private int maxLoops = 1000; // todo extract
    private static final long timePermitted = 100; // to compute crossword, in milliseconds todo extract
    private static final int spins = 2; // todo not working via inject

    private static final char EMPTY_CHAR = '-';

    private Crossword() {}

    public Crossword(List<Word> availableWords) {
        this.time = ThreadLocalRandom.current().nextInt(500); // todo extract
        this.grid = new char[rows][cols];
        clearGrid();
        this.availableWords = randomizeWordList(availableWords);
    }

    private void clearGrid() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                this.grid[i][j] = EMPTY_CHAR;
            }
        }
    }

    private List<Word> randomizeWordList(List<Word> words) {
        Collections.shuffle(words);
        // sort by word length descending order
        words.sort((o1, o2) -> {
            int o1Length = o1.getWord().length(), o2Length = o2.getWord().length();
            if (o1Length > o2Length)
                return -1;
            else if (o1Length < o2Length)
                return 1;
            else
                return 0;
        });
        return words;
    }

    public void computeCrossword() {
        int count = 0;
        long startFull = System.currentTimeMillis();
        Crossword copy = new Crossword();
        copy.availableWords = this.availableWords; // for some reason it does not recognize availableWords setter
        copy.setGrid(this.grid);
//        System.out.println("timePermitted = " + this.timePermitted);
        while (System.currentTimeMillis() - startFull < timePermitted || count == 0) {
            int x = 0;
            while (x < spins) {
                copy.availableWords.stream()
                        .filter(word -> !copy.currentWordList.stream()
                                .map(Word::getWord)
                                .collect(Collectors.toList())
                                .contains(word.getWord()))
                        .forEach(copy::fitAndAdd);
                x++;
            }
            // buffer the best Crossword by comparing placed words
            if (copy.currentWordList.size() > this.currentWordList.size()) {
                this.currentWordList = copy.currentWordList;
                this.grid = copy.grid;
            }

            count++;
        }
    }

    private class Coordinate {
        int col, row, vertical, something, score;

        Coordinate(int col, int row, int vertical, int something, int score) {
            this.col = col;
            this.row = row;
            this.vertical = vertical;
            this.something = something;
            this.score = score;
        }
    }

    private List<Coordinate> suggestCoordinate(Word word) {
        List<Coordinate> coordinateList = new ArrayList<>();
        int glc = -1;
        for (char givenLetter : word.getWord().toCharArray()) { // cycle through letters in word
            glc++;
            int rowC = 0;
            for (char[] row : this.grid) { // cycle through rows
                rowC++;
                int colC = 0;
                for (char cell : row) {
                    colC++;
                    if (givenLetter == cell) { // check match letter in word to letters in row
                        // suggest vertical placement
                        // make sure word doesn't go off of grid
                        if ((rowC - glc) + word.getWord().length() <= this.rows) {
                            coordinateList.add(new Coordinate(colC, rowC - glc, 1, colC + (rowC - glc), 0));
                        }
                        // suggest horizontal placement
                        if (colC - glc > 0) { // make sure we're not suggesting a starting point off the grid
                            coordinateList.add(new Coordinate(colC - glc, rowC, 0, rowC + (colC - glc), 0));
                        }
                    }
                }
            }

        }
        return this.sortCoordinateList(coordinateList, word);
    }

    /**
     * give each coordinate a score, then sort
     */
    private List<Coordinate> sortCoordinateList(List<Coordinate> coordinateList, Word word) {
        List<Coordinate> newCoordinateList = new ArrayList<>();
        for (Coordinate coordinate : coordinateList) {
            int col = coordinate.col, row = coordinate.row, vertical = coordinate.vertical;
            coordinate.score = this.getFitScore(col, row, vertical, word); // checking score
            if (coordinate.score != 0) { // 0 scores are filtered
                newCoordinateList.add(coordinate);
            }
        }

        Collections.shuffle(newCoordinateList); // randomize coordinate list; why not?
        // put the best scores first
        newCoordinateList.sort((o1, o2) -> {
            if (o1.score > o2.score) return 1;
            if (o1.score < o2.score) return -1;
            return 0;
        });
        return newCoordinateList;
    }

    /**
     * And return score (0 signifies no fit). 1 means a fit, 2+ means a cross.
     * The more crosses the better.
     */
    private int getFitScore(int col, int row, int vertical, Word word) {
        if (col < 1 || row < 1)
            return 0;
        int count = 1, score = 1; // give score a standard value of 1, will override with 0 if collisions detected
        for (char letter : word.getWord().toCharArray()) {
            char activeCell;

            try {
                activeCell = this.getCell(col, row);
            } catch (ArrayIndexOutOfBoundsException e) { // might happen
                return 0;
            }

            if (activeCell != EMPTY_CHAR && activeCell != letter) {
                return 0;
            }

            if (activeCell == letter)
                score++;

            if (vertical == 1) {
                // check surroundings
                if (activeCell != letter) {  // don't check surroundings if cross point
                    if (!this.isCellEmpty(col + 1, row)) // check right cell
                        return 0;
                    if (!this.isCellEmpty(col - 1, row)) // check left cell
                        return 0;
                }
                if (count == 1) { // check top cell only on first letter
                    if (!this.isCellEmpty(col, row - 1)) {
                        return 0;
                    }
                }
                if (count == word.getWord().length()) {  // check bottom cell only on last letter
                    if (!this.isCellEmpty(col, row + 1)) {
                        return 0;
                    }
                }
            } else { // else horizontal
                // check surroundings
                if (activeCell != letter) {  // don't check surroundings if cross point
                    if (!this.isCellEmpty(col, row - 1)) // check top cell
                        return 0;

                    if (!this.isCellEmpty(col, row + 1)) // check bottom cell
                        return 0;
                }
                if (count == 1) { // check left cell only on first letter
                    if (!this.isCellEmpty(col - 1, row))
                        return 0;
                }
                if (count == word.getWord().length()) {  // check right cell only on last letter
                    if (!this.isCellEmpty(col + 1, row))
                        return 0;
                }
            }
            if (vertical == 1) // progress to next letter and position
                row++;
            else // else horizontal
                col++;

            count++;
        }
        return score;
    }

    private boolean isCellEmpty(int col, int row) {
        try {
            char cell = this.getCell(col, row);
            if (cell == EMPTY_CHAR)
                return true;
        } catch (ArrayIndexOutOfBoundsException ignored) {
            // might happen, pass
        }
        return false;
    }

    private char getCell(int col, int row) {
        if (col < 1)
            col = this.cols - 1;
        if (row < 1)
            row = this.rows - 1;
        return this.grid[row - 1][col - 1];
    }

    private void fitAndAdd(Word word) {
        // doesn't really check fit except for the first word; otherwise just adds if score is good
        boolean fit = false;
        int count = 0;
        List<Coordinate> coordinateList = this.suggestCoordinate(word);
        while (!fit && count < this.maxLoops) {
            if (this.currentWordList.size() == 0) {  // this is the first word: the seed
                // top left seed of longest word yields best results (maybe override)
                int vertical = (ThreadLocalRandom.current().nextDouble() <= 0.5) ? 1 : 0, col = 1, row = 1;
                if (this.getFitScore(col, row, vertical, word) != 0) {
                    fit = true;
                    this.setWord(col, row, vertical, word);
                }
            } else { // a subsequent words have scores calculated
                try {
                    Coordinate coordinate = coordinateList.get(count);
                    int col = coordinate.col, row = coordinate.row, vertical = coordinate.vertical;
                    if (coordinate.score != 0) { // already filtered these out, but double check
                        fit = true;
                        this.setWord(col, row, vertical, word);
                    }
                } catch (IndexOutOfBoundsException ignored) {
                }
            }
            count++;
        }
    }

    private void setWord(int col, int row, int vertical, Word word) {
        word.setCol(col);
        word.setRow(row);
        word.setVertical(vertical);
        this.currentWordList.add(word);

        for (char letter : word.getWord().toCharArray()) {
            this.setCell(col, row, letter);
            if (vertical == 1)
                row++;
            else
                col++;
        }
    }

    private void setCell(int col, int row, char value) {
        this.grid[row - 1][col - 1] = value;
    }

}