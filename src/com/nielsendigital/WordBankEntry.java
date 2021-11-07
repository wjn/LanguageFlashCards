package com.nielsendigital;

import java.sql.Timestamp;

public class WordBankEntry implements LanguageTuple {
    private final String foreignLanguage;
    private final String nativeLanguage;
    private final String grammar;
    private final String answer;
    private Timestamp lastSeen;
    private int countSeen;
    private int countIncorrect;

    public WordBankEntry(String foreignLanguage,
                         String nativeLanguage,
                         String grammar,
                         String answer,
                         Timestamp lastSeen,
                         int countSeen,
                         int countIncorrect) {
        this.foreignLanguage = foreignLanguage;
        this.nativeLanguage = nativeLanguage;
        this.grammar = grammar;
        this.answer = answer;
        this.lastSeen = lastSeen;
        this.countSeen = countSeen;
        this.countIncorrect = countIncorrect;
    }

    @Override
    public boolean updateLastSeen() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        if (ts.getTime() > this.lastSeen.getTime()) {
            this.lastSeen = ts;
            return true;
        }
        return false;
    }

    @Override
    public boolean incrementCountSeen() {
        int countBeforeIncrement = this.countSeen;
        this.countSeen++;
        return (this.countSeen - countBeforeIncrement) == 1;
    }

    @Override
    public boolean incrementCountIncorrect() {
        int countBeforeIncrement = this.countIncorrect;
        this.countIncorrect++;
        return (this.countIncorrect - countBeforeIncrement) == 1;
    }

    public String getForeignLanguage() {
        return foreignLanguage;
    }

    public String getNativeLanguage() {
        return nativeLanguage;
    }

    public String getGrammar() {
        return grammar;
    }

    public String getAnswer() {
        return answer;
    }

    public Timestamp getLastSeen() {
        return lastSeen;
    }

    public int getCountSeen() {
        return countSeen;
    }

    public int getCountIncorrect() {
        return countIncorrect;
    }

    public String toCsvRow() {
        return "\"" + this.getForeignLanguage().replace("\"", "") + "\"," +
                "\"" + this.getNativeLanguage().replace("\"", "") + "\"," +
                "\"" + this.getGrammar().replace("\"", "").replace("\n", "\u2028") + "\"," +
                "\"" + this.getAnswer().replace("\"", "").replace("\n", "\u2028") + "\"," +
                "\"" + this.getLastSeen() + "\"," +
                "\"" + this.getCountSeen() + "\"," +
                "\"" + this.getCountIncorrect() + "\"" +
                "\n";
    }

    public String[] getAllValues() {
        return new String[]{
                this.getForeignLanguage(),
                this.getNativeLanguage(),
                this.getGrammar(),
                this.getAnswer(),
                this.getLastSeen().toLocalDateTime().toString(),
                Integer.toString(this.getCountSeen()),
                Integer.toString(this.getCountIncorrect())
        };
    }

}
