package com.nielsendigital;

import com.diogonunes.jcolor.Attribute;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

import static com.diogonunes.jcolor.Ansi.colorize;

public class UI {

    private static final Scanner scanner = new Scanner(System.in);

    public static void run() {
        try {
            while(true) {
                switch (Dialogs.run()) {
                    case 1 -> Dialogs.Quiz.quickStart();
                    case 2 -> Dialogs.Quiz.Configure.run();
                    case 3 -> Dialogs.WordBankUI.run();
                    case 0 -> {
                        System.out.println(Write.thanksForPracticing());
                        System.exit(0);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

    }

    public static class Dialogs {
        private static final String initialMessage = Write.appName + " Main Menu";

        private static int run() {
            System.out.println("\n" + Draw.hr_squig);
            System.out.println(Dialogs.initialMessage);
            System.out.println(Draw.hr_squig + "\n");

            System.out.println("""
                    1. Quick quiz
                    2. Configure quiz
                    3. Manage WordBanks
                    --------------------
                    0. Exit
                    """);
            System.out.print(colorize(Write.enterMenuNumberPrompt(), Attribute.BLUE_TEXT()));

            if (scanner.hasNextInt()) {
                int i = scanner.nextInt();
                scanner.nextLine();
                return i;
            }
    
            return 0;
        }

        private static boolean askYesNoQuestion(String questionToAsk) {
            String answer = null;
            String pattern = "[YyNn]";

            System.out.println(questionToAsk);
            System.out.print(colorize("Enter [y/n] : ", Attribute.BRIGHT_BLUE_TEXT()));
            if(scanner.hasNext(pattern)) {
                answer = scanner.next(pattern);
                scanner.nextLine();
            }
            if(answer != null && answer.matches(pattern)) {
                return answer.equalsIgnoreCase("y");
            }
            return false;
        }

        public static class WordBankUI {

            public static String menuName = "Word Bank";

            public enum MenuItem implements WithAbbreviations {
                SEARCH("s", "Search Word Bank"),
                ADD_ENTRY("a", "Add New Word"),
                UPDATE_ENTRY("u", "Edit New Word"),
                REMOVE_ENTRY("r", "Remove New Word"),
                EXIT("e", "Return to Main Menu")
                ;

                private final String abbreviation;
                private final String text;

                MenuItem(String abbreviation, String text) {
                    this.abbreviation = abbreviation;
                    this.text = text;
                }

                public String getText() {
                    return text;
                }

                @Override
                public String getAbbreviation() {
                    return this.abbreviation;
                }
            }

            public enum SearchMenuItem implements WithAbbreviations {
                FOREIGN("f", "by Foreign Term", WordBank.EntryHeading.FOREIGN_LANGUAGE),
                NATIVE("n", "by Native Term", WordBank.EntryHeading.NATIVE_LANGUAGE),
                GRAMMAR("g", "by Grammatical Term", WordBank.EntryHeading.GRAMMAR)
                ;

                private final String abbreviation;
                private final String text;
                private final WordBank.EntryHeading entryHeading;

                SearchMenuItem(String abbreviation, String text, WordBank.EntryHeading entryHeading) {
                    this.abbreviation = abbreviation;
                    this.text = text;
                    this.entryHeading = entryHeading;
                }

                public WordBank.EntryHeading getEntryHeading() {
                    return entryHeading;
                }

                public String getText() {
                    return text;
                }

                @Override
                public String getAbbreviation() {
                    return this.abbreviation;
                }
            }

            private static void run() throws Exception {
                System.out.println(Draw.hr);
                System.out.println("\t\t\t" + WordBankUI.menuName + " Manager");
                System.out.println(Draw.hr + "\n");

                // Build Menu
                StringBuilder wordBankMenuItems = new StringBuilder();
                for(MenuItem menuItem : MenuItem.values()) {
                    if(!menuItem.getAbbreviation().equals(MenuItem.EXIT.getAbbreviation())) {
                        wordBankMenuItems.append("\t").append(menuItem.getAbbreviation())
                                .append(" : ").append(menuItem.getText()).append("\n");
                    }
                }
                // create exit option last and below dotted line
                wordBankMenuItems.append(Draw.hr_thin_quarter).append("\n");
                wordBankMenuItems.append("\t").append(MenuItem.EXIT.getAbbreviation())
                        .append(" : ").append(MenuItem.EXIT.getText()).append("\n");

                System.out.println(wordBankMenuItems);
                System.out.print(Write.enterAbbreviationPrompt());

                if(scanner.hasNext()) {
                    MenuItem selected = (MenuItem) getEnumValueFromAbbreviation(scanner.next(), MenuItem.values());
                    scanner.nextLine();

                    switch(Objects.requireNonNull(selected)) {
                        case SEARCH -> {
                            searchWordBank(getWordBank());
                        }
                        case ADD_ENTRY -> addEntry(getWordBank());
                        case UPDATE_ENTRY -> editEntry(getWordBank());
                        case REMOVE_ENTRY -> removeEntry(getWordBank());
                        case EXIT -> UI.run();
                        default -> {
                            System.out.println(
                                    Write.enterWordBankMenuItemAbbreviations() +"\n");
                            WordBankUI.run();
                        }
                    }
                } else {
                    System.out.println(
                            Draw.hr_thin_quarter +
                                    Write.enterWordBankMenuItemAbbreviations() +
                                    Draw.hr_thin_quarter +"\n");
                }

            }

            private static void searchWordBank(WordBank wordBank) {
                StringBuilder sb = new StringBuilder();
                sb.append(Draw.hr_thin).append("\n");
                sb.append("\t\t WORD BANK SEARCH\n");
                sb.append(Draw.hr_thin).append("\n\n");

                // build search menu
                int wordListSize = wordBank.getWordListSize();
                String entryEntries = (wordListSize == 1) ? "entry" : "entries";
                sb.append("There are ").append(wordListSize).append(" ").append(entryEntries)
                        .append(" in the Word Bank.").append("\n");

                sb.append("Search type:\n");

                sb.append(getEnumMenuList(SearchMenuItem.values())).append("\n");
                sb.append(Write.enterAbbreviationPrompt());
                System.out.print(sb);

                if(scanner.hasNext()) {
                    SearchMenuItem searchCategory = (SearchMenuItem) getEnumValueFromAbbreviation(scanner.next(), SearchMenuItem.values());
                    scanner.nextLine();

                    if(searchCategory == null) {
                        System.out.println(colorize("ERROR: couldn't determine search type.", Attribute.BRIGHT_RED_TEXT()));
                        searchWordBank(wordBank);
                    }

                    String searchTerm = enterSearchTerm();

                    assert searchTerm != null;
                    assert searchCategory != null;

                    LinkedList<WordBankEntry> foundEntries =
                            wordBank.findWordBankEntries(searchTerm, searchCategory.getEntryHeading());

                    System.out.println("RESULTS for : " +
                            colorize(searchTerm, Attribute.BRIGHT_BLUE_TEXT()) +
                            " in " + colorize(searchCategory.getText(), Attribute.BRIGHT_BLUE_TEXT()) + "\n");
                    if(foundEntries == null && foundEntries.size() == 0) {
                        System.out.println("No entries found.");
                    } else {
                        StringBuilder results = new StringBuilder();
                        int colSizeCount = 6;
                        int colSizeForeign = colSizeCount * 4;
                        int colSizeNative = colSizeForeign;
                        int colSizeGrammar = colSizeForeign * 2;
                        int colSizeLastSeen = colSizeForeign;
                        int justification = 0;

                        // heading row
                        results.append(wrapTermWithSpace(colSizeCount, "#", justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeForeign, WordBank.EntryHeading.FOREIGN_LANGUAGE.getText(), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeNative, WordBank.EntryHeading.NATIVE_LANGUAGE.getText(), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeGrammar, WordBank.EntryHeading.GRAMMAR.getText(), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeCount, WordBank.EntryHeading.COUNT_SEEN.getText(), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeCount, WordBank.EntryHeading.COUNT_INCORRECT.getText(), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeLastSeen, WordBank.EntryHeading.LAST_SEEN.getText(), justification)).append(" | ");
                        results.append("\n");

                        // separator between heading row and table body
                        results.append(wrapTermWithSpace(colSizeCount, "≈".repeat(colSizeCount), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeForeign, "≈".repeat(colSizeForeign), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeNative, "≈".repeat(colSizeNative), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeGrammar, "≈".repeat(colSizeGrammar), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeCount, "≈".repeat(colSizeCount), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeCount, "≈".repeat(colSizeCount), justification)).append(" | ");
                        results.append(wrapTermWithSpace(colSizeLastSeen, "≈".repeat(colSizeLastSeen), justification)).append(" | ");
                        results.append("\n");



                        // table body
                        int count = 0;
                        Collections.sort(foundEntries, (entry1, entry2) -> entry1.getForeignLanguage().compareTo(entry2.getForeignLanguage()));

                        for(WordBankEntry wbe : foundEntries) {
                            count ++;
                            String[] col = wbe.getAllValues();
                            results.append(wrapTermWithSpace(colSizeCount, Integer.toString(count), justification)).append(" | ");
                            results.append(wrapTermWithSpace(colSizeForeign, col[WordBank.EntryHeading.FOREIGN_LANGUAGE.getIndex()], justification)).append(" | ");
                            results.append(wrapTermWithSpace(colSizeNative, col[WordBank.EntryHeading.NATIVE_LANGUAGE.getIndex()], justification)).append(" | ");
                            results.append(wrapTermWithSpace(colSizeGrammar, col[WordBank.EntryHeading.GRAMMAR.getIndex()], justification)).append(" | ");
                            results.append(wrapTermWithSpace(colSizeCount, col[WordBank.EntryHeading.COUNT_SEEN.getIndex()], justification)).append(" | ");
                            results.append(wrapTermWithSpace(colSizeCount, col[WordBank.EntryHeading.COUNT_INCORRECT.getIndex()], justification)).append(" | ");
                            results.append(wrapTermWithSpace(colSizeLastSeen, col[WordBank.EntryHeading.LAST_SEEN.getIndex()], justification)).append(" | ");
                            results.append("\n");
                        }
                        System.out.println(results);
                    }
                }

            }

            public static void printSpacingFathoms(String heading, int columnSize, String content, int spaceNeeded, int leftPadding, int rightPadding, String result) {
                String formattedResult = "resultSize   : " + Integer.toString(result.length());
                if(heading.toUpperCase(Locale.ROOT).equals("RESULT")) {
                    if (result.length() != columnSize) {
                        formattedResult = colorize(formattedResult, Attribute.BRIGHT_RED_BACK(), Attribute.BRIGHT_WHITE_TEXT());
                    }
                    formattedResult = colorize(formattedResult, Attribute.BRIGHT_GREEN_BACK(), Attribute.BRIGHT_WHITE_TEXT());
                }

                System.out.println("\n" + heading.toUpperCase(Locale.ROOT) + " : " + content +
                        "\n" + Draw.hr_thin_half +
                        "\ncolumnSize   : " + columnSize +
                        "\ncontentSize  : " + content.length() +
                        "\nspaceNeeded  : " + spaceNeeded +
                        "\nleftPadding  : " + leftPadding +
                        "\nrightPadding : " + rightPadding +
                        "\n" + formattedResult
                );
            }

            public static String wrapTermWithSpace(int columnSize, String content, int justification) {

                // Justification
                // 0: left
                // 1: center
                // 2: right

                boolean isReduced = false;
                // ensure there's is enough space in the column for the content
                if(content.length() > columnSize ) {
                    isReduced = true;
                    // abbreviate content that is too long.
                    // take an extra one extra space for the elipsis
                    int spacesTooMany = content.length() - columnSize + 1;
                    int expectedContentLength = content.length() - spacesTooMany;
                    String elipsis = new String(Character.toChars(0x2026));
                    content = content.substring(0, (expectedContentLength) ) + elipsis;
                }

                // calculate spaced needed
                int spaceNeeded = columnSize - content.length();
                int leftPadding = 0;
                int rightPadding = 0;


                if(spaceNeeded < 0) {
                    spaceNeeded = 0;
                }
                // odd space needed will have an extra space at the end of the result string.
                if(spaceNeeded % 2 != 0) {
                    leftPadding = spaceNeeded / 2;
                    rightPadding = leftPadding + 1;
                } else {
                    leftPadding = rightPadding = spaceNeeded / 2;
                }

                switch(justification) {
                    case 0 -> {
                        rightPadding = leftPadding + rightPadding;
                        leftPadding = 0;
                    }
                    case 1 -> {
                        // leave padding as is.
                    }
                    case 2 -> {
                        leftPadding = leftPadding + rightPadding;
                        rightPadding = 0;
                    }

                }


                // build padded column
                StringBuilder result = new StringBuilder();

                // prep results
                if(isReduced) {
                    content = colorize(content, Attribute.YELLOW_TEXT());
                }
                result.append(" ".repeat(leftPadding)).append(content).append(" ".repeat(rightPadding));

                return result.toString();
            }

            public static String enterSearchTerm() {
                System.out.print(Write.userEntryPrompt("\nEnter Search term"));
                if(!scanner.hasNextLine()) {
                    System.out.println("Please provide a search term.\n");
                    enterSearchTerm();
                }

                String searchTerm = scanner.nextLine();
                if(searchTerm == null) {
                    System.out.println("Please provide a search term.\n");
                    enterSearchTerm();
                }
                assert searchTerm != null;
                return searchTerm;
            }

            public static WordBank getWordBank() throws Exception {
                File wordListFile = selectWordListFile();
                assert wordListFile != null;
                return new WordBank(wordListFile.getCanonicalPath());
            }

            // eventually could accept a File with the path to the data files directory 
            public static File selectWordListFile() throws Exception {
                System.out.println("Please select the Word Bank file you wish to use:");
                
                // gather array of files from the data/ directory
                File dataDir = new File("data");
                String[] files = dataDir.list();
                
                if(files == null || files.length < 1) {
                    throw new Exception("ERROR: there are no files in the data directory.");
                }
                
                // display array of data files
                int count = 1;
                for(String file : files) {
                    System.out.println("\t" + count + ": " + file);
                    count++;
                }
                System.out.print("\n" + Write.enterMenuNumberPrompt());
                
                // store user's selection of file
                File wordListFile = null;
                if(scanner.hasNextInt()){
                    int index = scanner.nextInt() - 1;
                    scanner.nextLine();

                    if(index >= 0 && index < files.length) {
                        String pathname = dataDir.getPath() + "/" + files[index];
                        wordListFile = new File(pathname);

                        System.out.println("Working with data file:\n" + wordListFile.getCanonicalPath());
                    }
                }

                if(wordListFile == null){
                    System.out.println("ERROR: we weren't able to select a file. Let's try again.");
                    selectWordListFile();
                }
                return wordListFile;

            }

            private static void removeEntry(WordBank wordBank) {
                Write.WordBankEntryEditorMenuHeading(menuName,"Removing", wordBank);
            }

            private static void editEntry(WordBank wordBank) {
                Write.WordBankEntryEditorMenuHeading(menuName,"Editing", wordBank);
            }

            private static void addEntry(WordBank wordBank) throws Exception {
                Write.WordBankEntryEditorMenuHeading(menuName,"Adding", wordBank);

                LinkedList<WordBankEntry> newEntries = new LinkedList<>();
                do {
                    newEntries.add(buildSingleEntry(wordBank));
                } while(askYesNoQuestion("Would you like to add another entry?"));

                // after user indicates there are no more entries to add display number of entries
                // entered that will be added.
                System.out.println("You have " + newEntries.size() + " ready to add:\n");

                // display entries
                System.out.println(Draw.hr_squig_half);
                int count = 0;
                for(WordBankEntry wbe : newEntries) {
                    count ++;
                    for(int i = 0; i < WordBank.EntryHeading.values().length; i++ ) {
                        System.out.println(colorize(WordBank.EntryHeading.values()[i].getText(), Attribute.BOLD(), Attribute.YELLOW_TEXT()) +
                                " : " + colorize(wbe.getAllValues()[i].replace("\n\n", "\n").replace("\n", ", "), Attribute.BRIGHT_BLUE_TEXT()));
                    }
                    if(count < newEntries.size()) {
                        System.out.println(Draw.hr_thin_half + "\n");
                    }
                }
                System.out.println(Draw.hr_squig_half);



                // confirm user wants to save
                String entryEntries = (count == 1) ? "Save this entry" : "Save these entries";
                if(!askYesNoQuestion(colorize(entryEntries + " to the Word Bank file?",
                        Attribute.BRIGHT_BLUE_BACK(), Attribute.BLACK_TEXT()))){
                    System.out.println("Bummer. We'll need to start over at this point.");
                    addEntry(wordBank);
                }

                // write to wordbank file
                entryEntries = (count == 1) ? "entry was" : "entries were";
                // add the entries to the end of the list in memory
                if(wordBank.appendWordBankEntriesList(newEntries) && wordBank.writeEntriesToFile()) {
                        System.out.println("SUCCESS: the Word Bank file was saved.");
                        // Success messaging will be printed out by WordBank class
                        // return to the main menu
                        Dialogs.run();
                } else {
                    throw new Exception("Failed to append Word Bank list with the " + entryEntries +
                            " or there was a problem writing the Word Bank file to disk." +
                        "\nPathname: " + wordBank.getPathName());
                }
            }

            private static WordBankEntry buildSingleEntry(WordBank wordbank) {
                // Foreign term
                String foreignTerm = getForeignTerm(wordbank);

                // ---------- Part of Speech ----------
                PartOfSpeech partOfSpeech = getPartOfSpeech();

                // ---------- POS Change Type ----------
                // i.e., declension or conjugation
                assert partOfSpeech != null;
                ArrayList<? extends PosChange> posChanges = getPosChangeMenu(partOfSpeech);

                // ---------- Grammar ----------
                // Grammar: Part of speech + posChanges
                // multi-line string is compiled
                String grammar = getGrammar(partOfSpeech, posChanges);

                // ---------- Native language ----------
                // redisplay the foreign term information
                System.out.println("\n" + Draw.hr_thin_half);
                System.out.println("Foreign Term: " +
                        "\n" + colorize(foreignTerm, Attribute.BRIGHT_BLUE_TEXT(), Attribute.BOLD()) +
                        "\n\n" + colorize(grammar, Attribute.BLUE_TEXT()) );
                System.out.println(Draw.hr_thin_half+"\n");
                // prompt user to enter native term
                String nativeTerm = getNativeTerm(partOfSpeech, posChanges);

                // Assemble components into a Word Bank Entry
                return new WordBankEntry(
                        foreignTerm,
                        nativeTerm,
                        grammar,
                        getAnswer(nativeTerm, grammar),
                        getCurrentTimestamp(),
                        0,
                        0
                );

            }

            // Only Native Entries will have the change type. In the case of
            // Declensions we can provide the pronouns for consistency.
            private static String getNativeTerm(PartOfSpeech partOfSpeech,
                                                ArrayList<? extends PosChange> posChanges) {
                StringBuilder term = new StringBuilder();
                System.out.println(Write.enterNativeTerm() + "\n");

                // ≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈ PosChange Prefixing ≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈
                //
                // CONJUGATED VERBS
                // -----------------
                // compile the grammatical prefix that will go with the word for example:
                //      (lebe)   : I live
                //      (fallen) : we/they fall
                //      (hört)   : y'all/he/she/it hear(s)
                //
                // Conjugations follow the pattern: {pronoun/pronoun...} nativeVerb(s)
                //
                // Note: the `(s)` in cases where the pronoun/pronoun... complex requires
                // either a singular or plural form of the verb.
                //
                //
                // ≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈

                // Prepend posChanges to the term anticipating
                // the user entry below. Currently, only Verbs will receive pronoun prefixing.
                int count = 0;
                for(PosChange change : posChanges) {
                    count ++;

                    if(partOfSpeech == PartOfSpeech.VERB) {
                        term.append(change.getNativeTermTranslationInfo());
                        // insure there are no "/" at the end of the string.
                        if(count < posChanges.size()) {
                            term.append("/");
                        }
                    }
                }

                // In the case of Verbs, inform that the pronouns will be prefixed to the Native Term
                if(partOfSpeech == PartOfSpeech.VERB) {
                    System.out.println("\nWe'll add pronouns to the " +
                            WordBank.EntryHeading.NATIVE_LANGUAGE.getText() + " definition you " +
                            "\nprovide below so don't also type it.\n");
                }

                // Prompt for the Native Term
                System.out.print(colorize("Enter Native Term : " + term + " ", Attribute.BRIGHT_BLUE_TEXT()));


                // capture the Native Term entered by the user
                if(scanner.hasNextLine()) {
                    // if the term has a prefix add space before user entered Native Term.
                    if(term.length() > 0) {
                        term.append(" ");
                    }
                    term.append(scanner.nextLine());
                } else {
                    System.out.println(Write.termEntryTryAgain() + "\n");
                    getNativeTerm(partOfSpeech, posChanges);
                }

                if(!askYesNoQuestion("Does "+
                        colorize(String.valueOf(term),Attribute.BRIGHT_BLUE_TEXT()) + " look correct?")) {
                    term = new StringBuilder();
                    System.out.println("No worries, we'll try again.");
                    getNativeTerm(partOfSpeech, posChanges);
                } else {
                    System.out.println("Great! we'll make an entry for " +
                            colorize(String.valueOf(term),Attribute.BRIGHT_BLUE_TEXT()) + ".\n");
                }

                return term.toString();
            }

            private static String getForeignTerm(WordBank wordbank) {
                String term = null;
                System.out.println(Write.enterForeignTerm() + "\n");
                System.out.print(colorize("Enter Foreign Term : ", Attribute.BRIGHT_BLUE_TEXT()));

                if (scanner.hasNextLine()) {
                    term = scanner.nextLine();
                }

                if (term == null) {
                    System.out.println(Write.termEntryTryAgain() + "\n");
                    getForeignTerm(wordbank);
                }

                if(wordbank.isDuplicateEntry(term)) {
                    System.out.println(colorize("DUPLICATE: " + term + " already exists in the Word Bank.",
                            Attribute.RED_BACK(), Attribute.BRIGHT_WHITE_TEXT()) + "\n");
                    getForeignTerm(wordbank);
                }

                System.out.println("\nGreat! Let's focus on the term \"" + term + "\".");
                return term;
            }

            private static String getGrammar(PartOfSpeech pos, ArrayList<? extends PosChange> posChangeList) {
                ArrayList<String> grammarArrayList = new ArrayList<>();

                /* PART OF SPEECH
                   ≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈ */
                grammarArrayList.add(pos.getText());

                /* declension AND CONJUGATION
                   ≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈ */
                for(PosChange posChange : posChangeList) {
                    grammarArrayList.add(posChange.toGrammarString());
                }

                // assemble grammarArray list into single multi lined string
                return String.join("\n",grammarArrayList);
            }

            private static PartOfSpeech getPartOfSpeech() {
                System.out.println("Select the part of speech to which your term belongs:");
                for(PartOfSpeech pos : PartOfSpeech.values()) {
                    System.out.println("\t" + colorize(pos.getAbbreviation(), Attribute.BRIGHT_BLUE_TEXT()) +
                            ": " + pos.getText());
                }

                System.out.print(Write.enterAbbreviationPrompt());

                // look for abbr entry
                if(scanner.hasNext()) {
                    String selectedAbbreviation = scanner.next();
                    scanner.nextLine();

                    return (PartOfSpeech) getEnumValueFromAbbreviation(selectedAbbreviation, PartOfSpeech.values());
                }

                //If we haven't returned yet print an error message and
                // restart if we've not already returned the pos
                System.out.println(Write.enterPosAbbreviations() + "\n");

                getPartOfSpeech();
                
                return null;
            }

            private static ArrayList<? extends PosChange> getPosChangeMenu(PartOfSpeech pos) {
                System.out.println("\nTell us a little bit more about this " + pos.getText() + ".");
                if(pos.getChangeType() == PosChangeType.CONJUGATION) {
                    System.out.println("Sometimes " +
                            Write.thePluralOf(Write.Language.EN, pos.getText()) +
                            " can have several " + pos.getChangeType().getText() + " depending on the " +
                            "\ncontext. You can add one or more " + pos.getChangeType().getText() +
                            " for this " + pos.getText() + ".");
                }
                
                return getPosChangeList(pos);
            }

            private static ArrayList<PosChange> getPosChangeList(PartOfSpeech pos) {
                ArrayList<PosChange> posChangesArrayList = new ArrayList<>();
                PosChangeType posChangeType = pos.getChangeType();
                String thereIsAre = "There are";
                String changeChanges = Write.thePluralOf(Write.Language.EN, posChangeType.getText());


                while(true) {
                    PosChange posChange = getSinglePosChange(pos);
                    // Insure posChange isn't null and provide message if it is.
                    if(posChange == null) {
                        System.out.println("ERROR: " + posChangeType + " was null.");
                        break;
                    }
                    // add the selected posChange to the list
                    posChangesArrayList.add(posChange);

                    // format grammar for menu language
                    if(posChangesArrayList.size() == 1) {
                        thereIsAre = "There is";
                        changeChanges = posChangeType.getText();
                    }

                    // confirm with user their last entry and the total number of posChanges in the list.
                    System.out.println("Ok. We'll add " + posChange.toGrammarString() + " as a " +
                            posChangeType.getText() + ".");

                    // for POS or POS changeTypes that only support one posChange break out of the loop
                    if(pos == PartOfSpeech.NOUN) {
                        break;
                    }

                    System.out.println(thereIsAre + " currently " + posChangesArrayList.size() + " " +
                            changeChanges + " in your " + posChangeType.getText() + " list.");

                    // Repeat if user has more posChanges to enter else break the loop.
                    if(!askYesNoQuestion("\nWould you like to enter another " +
                            posChangeType.getText() + " for " + "this " + pos.getText() + "?")) {
                        break;
                    }
                }

                // send the list back.
                return posChangesArrayList;
            }

            private static PosChange getSinglePosChange(PartOfSpeech pos) {
                PosChange selectedPosChange = null;
                PosChangeType posChangeType = pos.getChangeType();
                ArrayList<String> errorList = new ArrayList<>();
                PosChange[] posChangeArray;

                if(posChangeType == PosChangeType.CONJUGATION) {
                    posChangeArray = Conjugation.values();
                    for(Conjugation conjugation : Conjugation.values()) {
                        System.out.println("\t" +conjugation.getAbbreviation() + ": " + conjugation.getText() +
                                " (" + conjugation.getPronouns() + ").");
                    }
                } else { // declensions
                    posChangeArray = Declension.values();
                    for(Declension declension : Declension.values()) {
                        System.out.println("\t" + declension.getAbbreviation() + ": " + declension.toGrammarString());
                    }
                }
                System.out.print(Write.enterAbbreviationPrompt());

                if(scanner.hasNext()) {
                    String userEntry = scanner.next();
                    scanner.nextLine();

                    return getPosChangeFromAbbreviation(userEntry, posChangeType);
                } else {
                    errorList.add(Write.enterNumberBetween(0, (posChangeArray.length - 1)));
                }

                if(errorList.size() > 0) {
                    for(String e : errorList) {
                        System.out.println(e);
                    }
                    selectedPosChange = null;
                }

                return selectedPosChange;
            }

            private static String getEnumMenuList(WithAbbreviations[] menuList) {
                StringBuilder sb = new StringBuilder();
                for(WithAbbreviations searchItem : menuList) {
                    sb.append("\t").append(searchItem.getAbbreviation()).append(" : ").append(searchItem.getText()).append("\n");
                }
                return sb.toString();
            }

            private static WithAbbreviations getEnumValueFromAbbreviation(String userEntry, WithAbbreviations[] enumWithAbbreviations) {
                for(WithAbbreviations enumValue : enumWithAbbreviations) {
                    if(enumValue.getAbbreviation().equalsIgnoreCase(userEntry)) {
                        return enumValue;
                    }
                }
                return null;
            }

            private static PosChange getPosChangeFromAbbreviation(String userEntry, PosChangeType posChangeType) {
                if(posChangeType.equals(PosChangeType.CONJUGATION)) {
                    return (PosChange) getEnumValueFromAbbreviation(userEntry, Conjugation.values());
                } else {
                    return (PosChange) getEnumValueFromAbbreviation(userEntry, Declension.values());
                }
            }

            private static String getAnswer(String nativeTerm, String grammar) {
                return nativeTerm + "\n\n" + grammar;
            }

            private static Timestamp getCurrentTimestamp() {
                return new Timestamp(System.currentTimeMillis());
            }

            public enum PosChangeType implements WithAbbreviations {
                CONJUGATION("c", "conjugation"),
                DECLENSION("d","declension");

                private final String abbreviation;
                private final String text;

                PosChangeType(String abbreviation, String text) {
                    this.abbreviation = abbreviation;
                    this.text = text;
                }

                public String getText() {
                    return text;
                }

                @Override
                public String getAbbreviation() {
                    return this.abbreviation;
                }
            }

            public enum PartOfSpeech implements WithAbbreviations {
                NOUN("Noun", "N", PosChangeType.DECLENSION),
                VERB("Verb", "V", PosChangeType.CONJUGATION),
                DETERMINER("Determiner", "D", PosChangeType.DECLENSION),
                PRONOUN("Pronoun", "PRO", PosChangeType.DECLENSION),
                ADJECTIVE("Adjective", "ADJ", PosChangeType.DECLENSION),
                ADVERB("Adverb", "ADV", null),
                PHRASE("Phrase", "PHR", null),
                INTERJECTION("Interjection", "INT",  null);

                private final String text;
                private final String abbreviation;
                private final PosChangeType changeType;

                PartOfSpeech(String text, String abbreviation, PosChangeType changeType) {
                    this.text = text;
                    this.abbreviation = abbreviation;
                    this.changeType = changeType;
                }

                public PosChangeType getChangeType() {
                    return changeType;
                }

                public String getText() {
                    return text;
                }

                @Override
                public String getAbbreviation() {
                    return this.abbreviation;
                }
            }

            public enum GrammaticalCase implements WithAbbreviations {
                NOMINATIVE("nom", "nominative", "subject", "takes action"),
                ACCUSATIVE("acc","accusative", "direct object", "receives action"),
                DATIVE("dat", "dative", "indirect object", "to/for whom action is taken"),
                GENITIVE("gen", "genitive", "possessive", "indicates owner of something/someone"),
                VOCATIVE("voc", "vocative", null, null),
                ABLATIVE("abl", "ablative", null, null);

                private final String abbreviation;
                private final String text;
                private final String role;
                private final String description;

                GrammaticalCase(String abbreviation, String text, String role, String description) {
                    this.abbreviation = abbreviation;
                    this.text = text;
                    this.role = role;
                    this.description = description;
                }

                public String getText() {
                    return text;
                }

                public String getRole() {
                    return role;
                }

                public String getDescription() {
                    return description;
                }


                @Override
                public String getAbbreviation() {
                    return this.abbreviation;
                }
            }

            public enum Declension implements PosChange, WithAbbreviations {
                MASCULINE_SINGULAR("ms", "masculine", "singular"),
                FEMININE_SINGULAR("fs", "feminine","singular"),
                NEUTER_SINGULAR("ns", "neuter","singular"),
                MASCULINE_PLURAL("mp", "masculine", "plural"),
                FEMININE_PLURAL("fp", "feminine","plural"),
                NEUTER_PLURAL("np", "neuter","plural");

                private final String abbreviation;
                private final String gender;
                private final String number;

                Declension(String abbreviation, String gender, String number) {
                    this.abbreviation = abbreviation;
                    this.gender = gender;
                    this.number = number;
                }

                @Override
                public String getNativeTermTranslationInfo() {
                    return toGrammarString();
                }

                @Override
                public String toGrammarString() {
                    return getGender() + " " + getNumber();
                }



                public String getGender() {
                    return gender;
                }

                public String getNumber() {
                    return number;
                }

                @Override
                public String getAbbreviation() {
                    return abbreviation;
                }

                @Override
                public String getText() {
                    return toGrammarString();
                }

            }

            public enum Conjugation implements PosChange, WithAbbreviations {
                FIRST_PER_SINGULAR("1ps","1st person singular", "I"),
                FIRST_PER_PLURAL("1pp", "1st person plural", "we"),
                SECOND_PER_SINGULAR("2ps","2nd person singular", "you"),
                SECOND_PER_PLURAL("2pp", "2nd person plural", "y'all"),
                THIRD_PER_SINGULAR("3ps", "3rd person singular", "he/she/it"),
                THIRD_PER_PLURAL("3pp", "3rd person plural", "they");

                private final String abbreviation;
                private final String text;
                private final String pronouns;

                Conjugation(String abbreviation, String text, String pronouns) {
                    this.abbreviation = abbreviation;
                    this.text = text;
                    this.pronouns = pronouns;
                }

                public String getText() {
                    return text;
                }

                public String getPronouns() {
                    return pronouns;
                }

                @Override
                public String getNativeTermTranslationInfo() {
                    return pronouns;
                }

                @Override
                public String toGrammarString() {
                    return getText();
                }

                @Override
                public String getAbbreviation() {
                    return abbreviation;
                }
            }

            public interface PosChange {
                // unifying interface for Conjugations and declensions
                String getNativeTermTranslationInfo();
                String toGrammarString();
            }

            public interface WithAbbreviations{
                String abbreviation = "";
                String getAbbreviation();
                String getText();
            }
        }

        public static class Quiz {

            private static void quickStart() {
                try {
                    new com.nielsendigital.Quiz(Configure.numberOfWords(), com.nielsendigital.Quiz.QuizType.RANDOM);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }

            public static class Configure {
                public static String menuName = "Configure a Quiz";

                private static void run() {
                    try {
                        System.out.println(Dialogs.Quiz.Configure.menuName + "\n");
                        int numWords = numberOfWords();
                        com.nielsendigital.Quiz.QuizType testType = type();
                        com.nielsendigital.Quiz.QuizDirection testDirection = direction();
                        if(confirm(numWords, testType, testDirection)) {
                            new com.nielsendigital.Quiz(numWords, testType, testDirection);
                        } else {
                            run();
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    }
                }

                private static boolean confirm(int numWords, com.nielsendigital.Quiz.QuizType testType, com.nielsendigital.Quiz.QuizDirection testDirection) {
                    System.out.println(Draw.hr_squig);
                    System.out.println("Please confirm the following config for your quiz:");
                    System.out.println("\tNumber of Words: " + numWords);
                    System.out.println("\tQuiz Type: " + testType.getQuizType());
                    System.out.println("\tDirectional Focus: " + testDirection.getDirectionType());
                    System.out.println(Draw.hr_squig+"\n");
                    return askYesNoQuestion("Does this look right?");
                }

                private static com.nielsendigital.Quiz.QuizType type() {
                    com.nielsendigital.Quiz.QuizType[] types = com.nielsendigital.Quiz.QuizType.values();
                    int i = 0;
                    int selection = 0;

                    System.out.println("What type of quiz do you have in mind?");
                    for(com.nielsendigital.Quiz.QuizType type : types){
                        i++;
                        System.out.println(i + ". " + type.getQuizType());
                    }
                    if(scanner.hasNextInt()) {
                        selection = scanner.nextInt();
                        scanner.nextLine();
                    }

                    // validate
                    if(selection > types.length || selection < 1 ) {
                        System.out.println(Write.enterNumberBetween(1,types.length));
                        direction();
                    }
                    System.out.println("Cool! We'll set up a " + types[selection-1].getQuizType() + " quiz.");

                    return types[selection - 1];
                }

                private static com.nielsendigital.Quiz.QuizDirection direction() {
                    com.nielsendigital.Quiz.QuizDirection[] testDirs = com.nielsendigital.Quiz.QuizDirection.values();
                    int i = 0;
                    int selection = 0;

                    System.out.println("What type of directional focus are you looking for?");
                    for(com.nielsendigital.Quiz.QuizDirection dir : testDirs) {
                        i++;
                        System.out.println(i + ". " + dir.getDirectionType());
                    }

                    if(scanner.hasNextInt()) {
                        selection = scanner.nextInt();
                        scanner.nextLine();
                    }
                    // validate
                    if(selection > testDirs.length || selection < 1 ) {
                        System.out.println(Write.enterNumberBetween(1,testDirs.length));
                        direction();
                    }
                    System.out.println("Super! We'll set up a quiz as: " + testDirs[selection-1].getDirectionType());
                    return testDirs[selection - 1];
                }

                private static int numberOfWords() {
                    System.out.println("How many words would you like to be quizzed on?\n");
                    int numWords;
                    if(scanner.hasNextInt()) {
                        numWords = scanner.nextInt();
                        scanner.nextLine();

                        System.out.println("Toll! We'll set up a quiz with " + numWords + " word(s)\n");
                        return numWords;
                    } else {
                        System.out.println(Write.enterAnInt());
                        numberOfWords();
                    }
                    return -1;
                }
            }
        }

    }

    public static class Write {
        public final static String appName = "Language Flash Cards";

        private static String enterNumberBetween(int first, int last) {
            return "Please enter a number between " +
                            first + " and " + last +".\n";
        }

        private static String enterPosAbbreviations() {
            StringBuilder result = new StringBuilder();
            result.append("Please enter one of the abbreviations for the menu selections:");
            for (Dialogs.WordBankUI.PartOfSpeech pos : Dialogs.WordBankUI.PartOfSpeech.values()) {
                result.append("\n\t - ").append(pos.getAbbreviation());
            }
            return result.toString();
        }

        private static String enterWordBankMenuItemAbbreviations() {
            StringBuilder result = new StringBuilder();
            result.append("Please enter one of the abbreviations for the menu selections:");
            for (Dialogs.WordBankUI.MenuItem item : Dialogs.WordBankUI.MenuItem.values()) {
                result.append("\n\t - ").append(item.getAbbreviation());
            }
            return result.toString();
        }

        private static String thanksForPracticing() {
            return "Thanks for practicing. See you next time.";
        }

        public static String enterAnInt() {
            return "Please enter a whole number (e.g., 1,2,3...).";
        }

        public static String enterTerm(WordBank.EntryHeading termType) {
            return "\n" + termType.getText().toUpperCase() +
                    "\n" + Draw.hr_thin_half +
                    "\nEnter the " + termType.getText() + " term exactly as you'd want to " +
                    "\nhave to answer it on a quiz.";
        }

        public static String enterNativeTerm() {
            return enterTerm(WordBank.EntryHeading.NATIVE_LANGUAGE) +
                    "\n\nFor example: " +
                    "\n\t- be consistent when entering different types of grammar." +
                    "\n\t- number agreement example: y'all he/she/it drink(s) " +
                    "\n\t    to represent both singular and plural.";

        }

        public static String enterForeignTerm() {
            return enterTerm(WordBank.EntryHeading.FOREIGN_LANGUAGE) +
                    "\n\nFor example: " +
                    "\n\t- use the definite article with nouns" +
                    "\n\t- do not include pronouns with verbs";
        }

        public static String termEntryTryAgain() {
            return "The term you entered wasn't quite right. Please try again.";
        }

        public static String thePluralOf(Language language, String word) {
            if(language == Language.EN) {
                //test cases: tart, horse, cup, alfalfa, polarity
                String[] letters = word.split("");
                String lastLetter = letters[(letters.length -1)];
                if(lastLetter.matches("[IiOoHh]") ) {
                    return word + "es";
                } else if(lastLetter.matches("[Yy]")) {
                    return word.substring(0, (word.length() - 1)) + "ies";
                } else {
                    return word + "s";
                }
            } else {
                System.out.println("thePluralOf() currently only supports " + Language.EN.getText());
            }
            return null;
        }

        public static String WordBankEntryEditorMenuHeading(String menuName, String actionType, WordBank wordBank) {
            return "\n" + actionType + " " + menuName + " entry" +
                "\n\t- WordBank: " + wordBank.getPathName() +
                "\n\t- Number Entries: " + wordBank.getWordList().size() +
                "\n" + Draw.hr_thin + "\n";
        }

        public static String enterMenuNumberPrompt() {
            return colorize("Enter menu number : ", Attribute.BRIGHT_BLUE_TEXT());
        }

        public static String enterAbbreviationPrompt() {
            return colorize("Enter the abbreviation : ", Attribute.BRIGHT_BLUE_TEXT());
        }

        public static String userEntryPrompt(String prompt) {
            return colorize(prompt + " : ", Attribute.BRIGHT_BLUE_TEXT());
        }

        public enum Language {
            EN("English", ".", ","),
            ES("Spanish", ".", ","),
            DE("Deutsch", ",", "."),
            RU("Deutsch", ",", ".");

            private final String text;
            private final String decimalChar;
            private final String thousandsSeparator;

            Language(String text, String decimalChar, String thousandsSeparator) {
                this.text = text;
                this.decimalChar = decimalChar;
                this.thousandsSeparator = thousandsSeparator;
            }

            public String getText() {
                return text;
            }

            public String getDecimalChar() {
                return decimalChar;
            }

            public String getThousandsSeparator() {
                return thousandsSeparator;
            }
        }
    }
    
    public static class Draw {
        public static final String hr                  = "========================================";
        public static final String hr_half             = "====================";
        public static final String hr_quarter          = "==========";
        public static final String hr_thin             = "----------------------------------------";
        public static final String hr_thin_half        = "--------------------";
        public static final String hr_thin_quarter     = "----------";
        public static final String hr_squig            = "≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈";
        public static final String hr_squig_half       = "≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈≈";
        public static final String hr_squig_quarter    = "≈≈≈≈≈≈≈≈≈≈";
    }
}
