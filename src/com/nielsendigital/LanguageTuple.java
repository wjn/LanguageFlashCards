package com.nielsendigital;

import java.sql.Timestamp;

public interface LanguageTuple {
    // CSV headings: ForeignLanguage,NativeLanguage,Grammar,Answer,LastSeen,CountSeen,CountIncorrect
    public String foreignLanguage = null;
    public String nativeLanguage = null;
    public String grammar = null;
    public String answer = null;
    public Timestamp lastSeen = null;
    public int countSeen = 0;
    public int countIncorrect = 0;

    public boolean updateLastSeen();

    public boolean incrementCountSeen();

    public boolean incrementCountIncorrect();
}
