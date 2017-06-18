// (c) Wiltrud Kessler
// 29.04.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/

package de.uni_stuttgart.ims.corpus.folds;


import de.uni_stuttgart.ims.util.Fileutils;
import de.uni_stuttgart.ims.nlpbase.io.ParseReaderCoNLL;
import de.uni_stuttgart.ims.nlpbase.io.ParseWriterCoNLL;
import de.uni_stuttgart.ims.nlpbase.nlp.SRLSentence;


/**
 * Create crossvalidation folds from the input file.
 * The folds are mixed, meaning 5 folds = 5 files,
 *  1st sentence in 1st fold
 *  2nd sentence in 2nd fold
 *  ...
 *  6th sentence in 1st fold
 *  ...
 *
 * @author kesslewd
 */
public class CreateMixedFolds {



   public static void main(String[] args) {

      // ===== GET USER-DEFINED OPTIONS =====

      String inputFilename = null;
      int numberOfFolds = 0;

      try {

         if (args.length < 2) {
            System.err.println("Usage: CreateFolds <inputFilename> <numberOfFolds>");
            System.exit(1);
         }

         inputFilename = args[0];

         numberOfFolds = Integer.parseInt(args[1]);
         if (numberOfFolds == 0) {
            System.err.println("ERROR !!! numberOfFolds may not be 0");
            System.exit(1);
         }

      } catch (Exception e) {
         System.err.println("ERROR !!! while parsing options: " + e.getMessage());
         System.exit(1);
      }


      // ===== INITIALIZATION =====

      ParseWriterCoNLL[] out = null;
      ParseReaderCoNLL parseReader = null;

      try{
         // Open input parsed sentences file
         parseReader = new ParseReaderCoNLL(inputFilename);
         parseReader.openFile();
         //System.out.println();

         // open output files for parsed sentences
         out = new ParseWriterCoNLL[numberOfFolds];
         for (int i=0; i<numberOfFolds; i++) {
            out[i] = new ParseWriterCoNLL(inputFilename + "." + (i+1));
         }

      } catch (Exception e) {
         System.err.println("ERROR in initialization: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }


      // ===== PROCESSING =====

      SRLSentence sentence;
      int fold = 1;
      int lineno = 0;

      while (!(sentence = parseReader.readParseSRL()).isEmpty()) {

         // Create folds for output parses
         fold = lineno % numberOfFolds;

         try {
            // Write new parse with SRL annotation
            out[fold].writeParse(sentence);

         } catch (Exception e) {
            e.printStackTrace();
            continue;
         }

         lineno++;

      }


      // ===== STATISTICS, CLEANUP =====

      //Close files and clean up
      Fileutils.closeSilently(parseReader);
      for (int i=0; i<numberOfFolds; i++) {
         Fileutils.closeSilently(out[i]);
      }

   }


}
