package com.ie.services;

import com.ie.models.Word;

import java.util.List;

public interface WordBankService {
    List<Word> getRandomWords(int level);

    String getClue(String query);
}
