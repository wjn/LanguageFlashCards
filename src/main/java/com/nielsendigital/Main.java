package com.nielsendigital;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Main {

    public static void main(String[] args)  {
        try {
        UI.run();

            /*
                test cases:
                    testA
                        colWidth : 16
                        content : 7
                    testB
                        colWidth : 16
                        content : 8
             */

//            ArrayList<ArrayList<String>> results = new ArrayList<>();
//            boolean printIteratively = false;
//            results.add(doTest("16x7", 16, 7, printIteratively));
//            results.add(doTest("16x8", 16, 8, printIteratively));
//            results.add(doTest("15x7", 15, 7, printIteratively));
//            results.add(doTest("15x8", 15, 8, printIteratively));
//            results.add(doTest("15x17", 15, 17, printIteratively));
//            results.add(doTest("15x15", 15, 15, printIteratively));
//
//            for(ArrayList<String> al : results) {
//            int count = 0;
//                for(String alEntry : al) {
//                    System.out.println(alEntry);
//                }
//            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.getCause();
            e.printStackTrace();
        }
    }

    private static ArrayList<String> doTest(String testName, int colWidth, int contentWidth, boolean shouldPrintIteratively) {
        StringBuilder heading = new StringBuilder();
        StringBuilder rows = new StringBuilder();

        String test = (testName == null || testName.isEmpty()) ? "UNLABELED" : testName;
        int testColWidth = colWidth;
        int testContentWidth = contentWidth;
        String testRuler = "#".repeat(testColWidth);
        String testContent = "X".repeat(testContentWidth);

        heading.append(testRuler).append(" | ");
        rows.append(UI.Dialogs.WordBankUI.wrapTermWithSpace(testColWidth, testContent, 0)).append(" | ");

        if(shouldPrintIteratively) {
            System.out.println("TEST " + test + ": (" + testColWidth + " : " + testContentWidth + ")");
            System.out.println(testContent);
            System.out.println(testRuler);
            System.out.println(UI.Dialogs.WordBankUI.wrapTermWithSpace(testColWidth, testContent, 0).replace(" ", "1"));
        }
        ArrayList<String> out = new ArrayList<String>();
        out.add(heading.toString());
        out.add(rows.toString());
        return out;
    }
}
