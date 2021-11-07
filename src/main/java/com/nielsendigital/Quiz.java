package com.nielsendigital;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Should take a subset of the WordBank and test on that. User should be given feedback on whether their answer was
 * correct or not and the correct answer should be shown in either case.
 */
public class Quiz {
    private final int numWordsToTest;
    private final QuizType quizType;
    private final WordBank testBank;
    private final String wordBankPath;
    private final QuizDirection testDirection;

    public Quiz(int numWordsToTest, QuizType quizType, WordBank testBank, QuizDirection testDirection) throws Exception {
        this.wordBankPath = "german-english.csv";
        this.numWordsToTest = numWordsToTest;
        this.quizType = quizType;
        this.testDirection = testDirection;
        if(testBank == null) {
            this.testBank = this.loadWords();
        } else {
            this.testBank = testBank;
        }

        if(numWordsToTest > this.testBank.getWordList().size()) {
            System.out.println("ERROR: more words requested than in Word Bank:" +
                    "\n- words requested: " + this.numWordsToTest +
                    "\n- word bank words: " + this.testBank.getWordList().size()
            );
            return;
        }
        this.run();
    }

    public Quiz(int numWordsToTest, WordBank testBank) throws Exception {
        this( numWordsToTest,
                QuizType.RANDOM, // default to random type
                testBank,
                QuizDirection.RANDOM); // random test direction
    }

    public Quiz(int numWordsToTest, QuizType quizType) throws Exception {
        this( numWordsToTest,
                quizType,
                null, // will use the default word bank
                QuizDirection.RANDOM // random test direction
        );
    }

    public Quiz(QuizType quizType) throws Exception {
        this(20, // defaults to 20 word
                quizType,
                null, // will use the default word bank
                QuizDirection.RANDOM // random test direction
        );
    }

    public Quiz(QuizType quizType, QuizDirection testDirection) throws Exception {
        this(20, // defaults to 20 word
                quizType,
                null, // will use the default word bank
                testDirection
        );
    }

    public Quiz(int numWords, QuizType testType, QuizDirection testDirection) throws Exception {
        this(numWords,
                testType,
                null, // will use default word bank
                testDirection
        );
    }

    private WordBank loadWords(boolean shouldPrintDetails) throws Exception {
        return new WordBank(this.wordBankPath, shouldPrintDetails);
    }

    private WordBank loadWords() throws Exception {
        return new WordBank(this.wordBankPath);
    }

    private void run() throws Exception {
        this.printQuizInfo();
        List<WordBankEntry> currentTestList;
        switch(this.quizType) {
            case MOST_INCORRECT:
                currentTestList = getMostIncorrectWordList();

                // restart building the word test if there was a problem.
                if(currentTestList == null) {
                    return;
                }
                break;
            case LEAST_RECENTLY_SEEN:
                currentTestList = getLeastRecentlySeenWordList();
                // restart building the word test if there was a problem.
                if(currentTestList == null) {
                    return;
                }
                break;
            case RANDOM :
            default:
                currentTestList = getRandomWordList();
                break;
        }

        gradeQuiz(startWordQuiz(currentTestList));

        printCurrentQuizList(currentTestList);
    }

    private List<WordBankEntry> getLeastRecentlySeenWordList() throws Exception{
        List<WordBankEntry> wordList = this.testBank.getWordList();
        List<WordBankEntry> quizList = new LinkedList<WordBankEntry>();

        // sort list by least recently seen descending
        wordList.sort(new Comparator<WordBankEntry>() {
            @Override
            public int compare(WordBankEntry o1, WordBankEntry o2) {
                return o1.getLastSeen().compareTo(o2.getLastSeen());
            }
        });

        // fetch all the entries that have been answered incorrectly at least 1x
        for (int i = 0; i < this.numWordsToTest; i++) {
            quizList.add(wordList.get(i));
        }

        if( !this.areThereEnoughWords(quizList.size()) ) {
            return null;
        }

        Collections.shuffle(quizList);
        return quizList;
    }

    private List<WordBankEntry> getMostIncorrectWordList() throws Exception{
        List<WordBankEntry> quizList = new LinkedList<WordBankEntry>();

        // fetch all the entries that have been answered incorrectly at least 1x
        for (WordBankEntry wbe : this.testBank.getWordList()) {
            if (wbe.getCountIncorrect() > 0) {
                quizList.add(wbe);
            }
        }

        if( !this.areThereEnoughWords(quizList.size()) ) {
            return null;
        }
        return getRandomWordList(quizList);
    }

    private List<WordBankEntry> getRandomWordList(List<WordBankEntry> wordlist) throws Exception {
        if(wordlist == null || wordlist.size() < 1) {
            throw new FatalQuizException("ERROR: there are no values in the word list.");
        }
        List<WordBankEntry> currentTestList = new LinkedList<WordBankEntry>();
        Random random = new Random();

        for(int i = 0; i < this.numWordsToTest; i++ ) {
            currentTestList.add( wordlist.get( random.nextInt(0, wordlist.size()) ) );
        }

        return currentTestList;
    }

    private List<WordBankEntry> getRandomWordList() throws Exception {
        return getRandomWordList(this.testBank.getWordList());
    }

    private boolean areThereEnoughWords(int quizListSize) {
        return this.areThereEnoughWords(
                quizListSize,
                "you have asked to quiz more words than are available in the word bank!"
        );
    }

    private boolean areThereEnoughWords(int quizListSize, String message) {
        if( quizListSize < this.numWordsToTest) {
            System.out.println("ERROR: " + message +
                    "\n- total words available: " + quizListSize +
                    "\n- word requested for quiz: " + this.numWordsToTest
            );
            return false;
        }

        return true;
    }

    private void gradeQuiz(List<QuizEvaluationResult> results) throws Exception {
        int count = 0;
        int failureCount = 0;
        int numCorrect = 0;
        double score;

        // tabulate score and update the testBank so that it can be saved to file.
        for(QuizEvaluationResult result : results) {
            count++;
            if(result.isCorrect) {
                numCorrect++;
            }
            String grammar = "Successfully updated";
            /*
              At this point, updateTestBankEntry will update this.testBank with the testing data (e.g., last_seen).
              After, this loop the data would be ready to write from this.testBank to the WordBank file.
             */
            if(!this.updateQuizBankEntry(result.getEntry())) {
                failureCount++;
                grammar = "FAILED to update";
            }
            if(this.testBank.isShouldPrintDetails() || (failureCount > 0)) {
                System.out.println(grammar + " " + result.wordTested + " : " + result.answerExpected + " pair in the word bank.");
            }
        }

        if(this.testBank.isShouldPrintDetails()) {
            System.out.println("Writing entries to csv");
        }

        // eventually may return true/false depending on how the PrintWriter works
        this.testBank.writeEntriesToFile();

        System.out.println("\n" + UI.Draw.hr_squig);
        System.out.println("\n                      Congratulations!");
        System.out.println("\n" + UI.Draw.hr_squig + "\n");
        score = (((double) numCorrect / (double) count)*100);
        System.out.println("You scored " + String.format("%.2f", score) + "%");
    }

    private boolean updateQuizBankEntry(WordBankEntry entry) {
        LinkedList<WordBankEntry> testList = this.testBank.getWordList();
        boolean wasSuccess = false;
        int i = 0;
        // this seems like an expensive way to do this, searching the entire wordbank for each entry to be updated.
        for(WordBankEntry wbe : testList) {
            if( Objects.equals(wbe.getForeignLanguage(), entry.getForeignLanguage()) &&
                    Objects.equals(wbe.getNativeLanguage(), entry.getNativeLanguage())
            ) {
                testList.set(i, entry);
                wasSuccess = true;
                break;
            }
            i++;
        }
        return wasSuccess;
    }

    private List<QuizEvaluationResult> startWordQuiz(List<WordBankEntry> wordlist ) {
        List<QuizEvaluationResult> results = new LinkedList<QuizEvaluationResult>();
        System.out.println("---------------------- Start Test ---------------------");

        for(WordBankEntry entry : wordlist) {
            results.add(new QuizEvaluationResult(entry, this.testDirection));
        }

         return results;
    }

    private void printTestOfflineMessage() {
        System.out.println("Currently, '" + this.quizType.getQuizType() + "' testing type is offline. " +
                "A '" + QuizType.RANDOM.getQuizType() + "' test of " +
                this.numWordsToTest + " words will be used instead.");
    }

    private void printCurrentQuizList(List<WordBankEntry> wordlist ) {
        System.out.println();
        System.out.println("---------------------- Quizzed Word List ---------------------");

        int i = 0;
        for(WordBankEntry wbe : wordlist) {
            i++;
            System.out.println(i + ": " + wbe.getForeignLanguage() + " : " + wbe.getNativeLanguage() +
                    "\n\t(numSeen: " + wbe.getCountSeen() +
                        " numIncorrect: " + wbe.getCountIncorrect() +
                        " lastSeen: " + timeToRelativeTime(wbe.getLastSeen()) + ")");
        }
        System.out.println("--------------------------------------------------------------\n\n");

    }

    private String timeToRelativeTime(Timestamp ts) {

        Duration duration = Duration.between(ts.toLocalDateTime(), LocalDateTime.now());

        return String.format("%d days, %d hours ago",
                duration.toDaysPart(), duration.toHoursPart());
    }

    private void printQuizInfo() {
        System.out.println();
        System.out.println("---------------------- Test Info ----------------------");
        System.out.println("type: \t\t\t" + this.quizType.getQuizType() +
                "\ndirection: \t\t" + this.testDirection.getDirectionType() +
                "\nword count: \t" + this.numWordsToTest + " out of " + this.testBank.getWordList().size() +
                "\nword bank: \t\t" + this.testBank.getPathName());
        System.out.println("-------------------------------------------------------\n");
    }

    private class QuizEvaluationResult {
        private Scanner scanner;
        private final WordBankEntry entry;
        private final String wordTested;
        private String answerGiven;
        private String answerExpected;
        private boolean isCorrect;

        public QuizEvaluationResult(WordBankEntry entry, QuizDirection testDirection) {
            this.scanner = new Scanner(System.in);
            switch (testDirection) {
                case RANDOM:
                    Random rand = new Random();
                    int zeroOrOne = rand.nextInt(0,2);
                    if( zeroOrOne == 1) {
                        this.wordTested = entry.getNativeLanguage();
                        this.answerExpected = entry.getForeignLanguage();
                    } else {
                        this.wordTested = entry.getForeignLanguage();
                        this.answerExpected = entry.getNativeLanguage();
                    }
                    break;

                case NATIVE_TO_FOREIGN:
                    this.wordTested = entry.getNativeLanguage();
                    this.answerExpected = entry.getForeignLanguage();
                    break;

                case FOREIGN_TO_NATIVE:
                default:
                    this.wordTested = entry.getForeignLanguage();
                    this.answerExpected = entry.getNativeLanguage();
                    break;
            }

            this.entry = entry;
            this.answerGiven = "";
            this.isCorrect = false;

            this.evaluate();

        }

        public void printReport() {
            System.out.println("/nYou're answer was " + ((this.isCorrect) ? "correct" : "incorrect") + "." +
                    "\n\nWord Quizzed: " + this.wordTested +
                    "\nAnswer Given: " + this.getAnswerGiven() +
                    "\nAnswer Expected: " + this.getAnswerExpected() +
                    "\nDetails:\n" +
                    this.entry.getAnswer()
            );
        }

        private void evaluate() {
            System.out.println(this.wordTested + " (" + this.entry.getGrammar() + ") : ");
            if(this.entry.getCountSeen() > 0) {
                System.out.println("- you've seen this word " + this.entry.getCountSeen() + " times");
                System.out.println("- you have " + ((this.entry.getCountIncorrect() == 0) ? "not missed this word before." : "missed this word " + this.entry.getCountIncorrect() + " time(s).") );
                System.out.println("- last seen on " + timeToRelativeTime(this.entry.getLastSeen()) );
            }
            this.answerGiven = this.scanner.nextLine();

            String given = sanitize(this.answerGiven);
            String expected = sanitize(this.answerExpected);
            String message;
            if(given.equals(expected)){
                this.isCorrect = true;
                message = "Correct!";
            } else {
                this.isCorrect = false;
                this.entry.incrementCountIncorrect();
                message = "Incorrect. Looking for : " + expected + " instead of " + given;
            }
            this.entry.incrementCountSeen();
            this.entry.updateLastSeen();
            System.out.println(message + "\n");
        }

        private String sanitize(String term) {
            return term.toLowerCase(Locale.ROOT).trim();
        }

        public String getAnswerGiven() {
            return this.answerGiven;
        }

        public String getAnswerExpected() {
            return answerExpected;
        }

        public WordBankEntry getEntry() {
            return entry;
        }
    }

    public enum QuizDirection {
        FOREIGN_TO_NATIVE("Foreign > Native"),
        NATIVE_TO_FOREIGN("Native > Foreign"),
        RANDOM("Random bi-directional");

        private final String direction;

        private QuizDirection(String direction) {
            this.direction = direction;
        }

        public String getDirectionType() {
            return this.direction;
        }
    }

    public enum QuizType {
        RANDOM("random"),
        LEAST_RECENTLY_SEEN("least recently seen"),
        MOST_INCORRECT("most times answered incorrectly");

        private final String quizType;

        private QuizType(String quizType) {
            this.quizType = quizType;
        }

        public String getQuizType() {
            return quizType;
        }
    }

    public static class FatalQuizException extends Exception {
        public FatalQuizException(String message) {
            super("FATAL > " +message);
        }
    }

}
