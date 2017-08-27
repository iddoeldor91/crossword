package com.ie.service;

import com.ie.model.Word;

import java.util.List;

public interface WordBankService {
    List<Word> getRandomWords(int level);

    void getCluesInParallel(List<Word> currentWordList);

    void initClues();
}
