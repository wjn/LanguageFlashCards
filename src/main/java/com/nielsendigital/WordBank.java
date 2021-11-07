package com.nielsendigital;

import java.io.*;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

/*
WordBank manages a specific set of words (WordBankEntry (WBE) instances). Each WBE has fields which
correspond to the csv that stores the persistent record and tracks progress.

WordBank must be instantiated with a path to a csv that follows the tuple format of :
    [ForeignLanguage,NativeLanguage,Grammar,Answer,LastSeen,CountSeen,CountIncorrect]
 */
public class WordBank {
    private File wordBankFile;
    private final String delimiter;
    private LinkedList<WordBankEntry> wordList; // i.e., the word bank
    private final boolean shouldPrintDetails;
    private final int numberEntriesOnLoad;

    public WordBank(String pathname, boolean shouldPrintDetails) throws Exception {
        this.shouldPrintDetails = shouldPrintDetails;
        this.delimiter = ",";
        this.wordList = new LinkedList<>();
        if(!getFile(pathname)) {
            throw new Exception("ERROR: csv file was not read at " + pathname);
        }
        if(!readFileToEntries()) {
            throw new Exception("ERROR: populating the word bank.");
        }
        this.numberEntriesOnLoad = this.wordList.size();
    }

    public WordBank(String pathname) throws Exception {
        this(pathname, false);
    }

    public boolean appendNewWordBankEntry(WordBankEntry wbe) {
        return this.wordList.add(wbe);
    }

    public boolean appendWordBankEntriesList(List<WordBankEntry> wordBankEntries) {
        return this.wordList.addAll(wordBankEntries);
    }

    public LinkedList<WordBankEntry> getWordList() {
        return this.wordList;
    }

    public void setWordList(LinkedList<WordBankEntry> wordList) {
        this.wordList = wordList;
    }

    public String getPathName() {
        return this.wordBankFile.getName();
    }

    private boolean getFile(String pathname) throws NullPointerException {
        try {
            File file = new File(pathname);
            if(file.isFile()) {
                this.wordBankFile = file;
                return true;
            }
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public LinkedList<WordBankEntry> findWordBankEntries(String term, EntryHeading heading) {
        LinkedList<WordBankEntry> resultsList = new LinkedList<>();
        String sanitizedTerm = term.trim().toLowerCase();
        for(WordBankEntry wbe : this.wordList) {
            switch(heading) {
                case FOREIGN_LANGUAGE -> {
                    if(sanitizedTerm.equalsIgnoreCase(wbe.getForeignLanguage())) resultsList.add(wbe);
                }
                case NATIVE_LANGUAGE -> {
                    if(sanitizedTerm.equalsIgnoreCase(wbe.getNativeLanguage())) resultsList.add(wbe);
                }
                case GRAMMAR -> {
                    if(wbe.getGrammar().toLowerCase().contains(sanitizedTerm)) resultsList.add(wbe);
                }
            }
        }
        return resultsList;
    }

    public boolean isDuplicateEntry(String term) {
        for(WordBankEntry wbe : this.wordList) {
            if(term.equalsIgnoreCase(wbe.getForeignLanguage())) {
                return true;
            }
        }

        return false;
    }

    private boolean clearWordBankList() {
        this.wordList.clear();
        return this.getWordList().size() == 0;
    }

    private boolean readFileToEntries() {
        LinkedList<WordBankEntry> wordList = readFileToEntries(this.wordBankFile,
                this.wordList,
                this.delimiter,
                this.shouldPrintDetails);
        return wordList != null;
    }

    public static LinkedList<WordBankEntry> readFileToEntries(File wordBankFile,
                                                        LinkedList<WordBankEntry> wordList,
                                                        String delimiter, boolean shouldPrintDetails) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(wordBankFile));
            String line;
            int prevNumEntries = wordList.size();
            LinkedList<WordBankEntry> copyWordList = new LinkedList<>(wordList);

            int count = -1; // once in loop immediately goes to 0, then checks to see if 1st row is a header row
            boolean hasHeaderRow = true;

            while((line = br.readLine()) != null) {
                count++;

                // skip heading row
                if(hasHeaderRow && count == 0) {
                    continue;
                }

                // use limit: -1 to include empty columns from csv
                String[] cols = line.split(delimiter, -1);

                if(cols.length != EntryHeading.values().length) {
                    System.out.println("ERROR: some lines in the CSV do not have the correct number of " +
                            EntryHeading.values().length + " columns." +
                            "\nCheck entry (" + count + ") " + cols[EntryHeading.FOREIGN_LANGUAGE.getIndex()] +
                            ":" + cols[EntryHeading.NATIVE_LANGUAGE.getIndex()] +
                            "\ncols currently has a length of " + cols.length);
                }

                // new WordBankEntry(ForeignLanguage,NativeLanguage,Grammar,Answer) as Strings
                wordList.add(new WordBankEntry(
                        cols[EntryHeading.FOREIGN_LANGUAGE.getIndex()].replace("\"", ""),
                        cols[EntryHeading.NATIVE_LANGUAGE.getIndex()].replace("\"", ""),
                        cols[EntryHeading.GRAMMAR.getIndex()].replace("\"", ""),
                        cols[EntryHeading.ANSWER.getIndex()].replace("\"", ""),
                        sanitizeStringToTimeStamp(cols[EntryHeading.LAST_SEEN.getIndex()].replace("\"", "")),
                        sanitizeStringToInt(cols[EntryHeading.COUNT_SEEN.getIndex()].replace("\"", "")),
                        sanitizeStringToInt(cols[EntryHeading.COUNT_INCORRECT.getIndex()].replace("\"", ""))
                ));

                if(shouldPrintDetails) {
                    System.out.println("Entry " + count + " : " +
                            cols[EntryHeading.FOREIGN_LANGUAGE.getIndex()] + " , " +
                            cols[EntryHeading.NATIVE_LANGUAGE.getIndex()]
                    );
                }

            } // end while((line = br.readLine()) != null)

            int expectedWordBankSize = prevNumEntries + count;
            if(!hasHeaderRow) expectedWordBankSize++;

            if(expectedWordBankSize != wordList.size()) {
                System.out.println("ERROR: Current size of the word bank is different than it should be\n" +
                        prevNumEntries + " : prev size\n" +
                        expectedWordBankSize + " : expected size\n" +
                        wordList.size() + " : current word bank size\n" +
                        (expectedWordBankSize - wordList.size()) + " : difference");
                System.out.println("Restored prior list.");
                return copyWordList;
            }

            return wordList;

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static int sanitizeStringToInt (String stringInt) {
        if(stringInt.isEmpty()) {
            stringInt = "0";
        }
        return Integer.parseInt(stringInt);
    }

    public static Timestamp sanitizeStringToTimeStamp(String stringTs) {
        if(stringTs.isEmpty()) {
            stringTs = "0001-01-01 00:00:00";
        }
        return Timestamp.valueOf(stringTs);
    }

    public boolean writeEntriesToFile()  {
        BufferedWriter bw = null;
        // create a string from the wordlist for writing to file
        StringBuilder sb = new StringBuilder();
        // include heading row
        sb.append(getCvsHeadingsRow());
        // csv body of wb entries
        for(WordBankEntry wbe : this.wordList) {
            sb.append(wbe.toCsvRow());
        }

        try {
            Writer writer = new FileWriter(this.wordBankFile);
            bw = new BufferedWriter(writer);
            // write all lines to test.csv
            bw.append(sb.toString());

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if(bw != null) {
                    bw.close();
                }
            } catch (Exception e ) {
                System.out.println("Error in closing the BufferedWriter"+e);
            }
        }

        // the list size when the word bank was first created
        int originalListSize = this.numberEntriesOnLoad;
        // expects the current wordlist to have grown since load as words have been added by user
        int rowsExpectedToAdd = this.getWordList().size() - originalListSize;

        try {
            // validate the previous file write
            if(this.clearWordBankList() && this.readFileToEntries()) {
                // the current word list should be the sum of the original size + entries added.
                int entriesCountDiff = this.getWordList().size() - (originalListSize + rowsExpectedToAdd);

                if( entriesCountDiff != 0) {
                    // different sized lists
                    System.out.println("ERROR: The old list and the new list are different sizes:" +
                            "\noriginal entries count : " + originalListSize + " entries" +
                            "\nexpected entries added : " + rowsExpectedToAdd + " new entries" +
                            "\nnew entries count : " + this.getWordList().size() + " entries" +
                            "\n\nDesired Outcome: original + expected = new");
                } else {
                    return true;
                }
            } else {
                System.out.println("ERROR: could not read csv file after update.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public String getCvsHeadingsRow() {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for(EntryHeading heading : EntryHeading.values()) {
            count++;
            sb.append(heading.getCamelCase());

            // add comma unless at last column
            if(count < EntryHeading.values().length) {
                sb.append(",");
            }
        }
        sb.append("\n");

        return sb.toString();
    }

    public boolean isShouldPrintDetails() {
        return shouldPrintDetails;
    }

    public int getWordListSize() {
        return this.wordList.size();
    }

    public enum EntryHeading {
        FOREIGN_LANGUAGE(0, "ForeignLanguage", "Foreign Language"),
        NATIVE_LANGUAGE(1, "NativeLanguage", "Native Language"),
        GRAMMAR(2, "Grammar", "Grammar"),
        ANSWER(3, "Answer", "Answer"),
        LAST_SEEN(4, "LastSeen", "Last Seen"),
        COUNT_SEEN(5, "CountSeen", "Count Seen"),
        COUNT_INCORRECT(6, "CountIncorrect", "Count Incorrect");

        private final int index;
        private final String camelCase;
        private final String text;

        EntryHeading(int index, String camelCase, String text) {
            this.index = index;
            this.camelCase = camelCase;
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public String getCamelCase() {
            return camelCase;
        }

        public int getIndex() {
            return this.index;
        }

    }

}

