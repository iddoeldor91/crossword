#### Hebrew crossword generator with images as clues for Android app

### Using SprintBoot, [Caffeine](https://github.com/ben-manes/caffeine) for caching

`$ curl localhost:8080/get`

Will respond with Crossword object:
* l = level (default is one, the level the end user, the higher the number the hardest the crossword
** words difficultly algorithm is based on occurrences in wikipedia articles (high occurrences means easy word)
* t = time for level
* d contains array of Word, each word contains the position in the crossword grid, the actual word & clue, which is base64 image taken from the first picture on google images


Example:


```
{
    "l": 1,
    "t": 367,
    "d": [{
        "r": 1,
        "c": 1,
        "v": 1,
        "w": "אינו",
        "h": "/9j/4AAQSkZJRgA.....Sg0WA//9k="
    }, {
        "r": 1,
        "c": 1,
        "v": 0,
        "w": "אותו",
        "h": "/9j/4AAQSkZJRg......W0YH//2Q=="
    }, {
        "r": 3,
        "c": 1,
        "v": 0,
        "w": "ניתן",
        "h": "/9j/4AAQSkZJRgAB.......AQEAYABgAAD/2=="
    }]
}
```
