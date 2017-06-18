// (c) Wiltrud Kessler
// 29.04.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/

package de.uni_stuttgart.ims.corpus.folds;

import de.uni_stuttgart.ims.nlpbase.io.ParseReaderCoNLL;
import de.uni_stuttgart.ims.nlpbase.io.ParseWriterCoNLL;
import de.uni_stuttgart.ims.nlpbase.nlp.SRLSentence;
import de.uni_stuttgart.ims.util.Fileutils;

/**
 * Create crossvalidation folds from the input file.
 * The folds are split linearly.
 *
 * @author kesslewd
 */
public class CreateFolds {



   public static void main(String[] args) {


      // ===== GET USER-DEFINED OPTIONS =====

      String inputFilename = null;
      int numberOfSentences = 0;
      int numberOfFolds = 0;

      try {

         if (args.length < 3) {
            System.err.println("Usage: CreateFolds <inputFilename> <numberOfSentences> <numberOfFolds>");
            System.exit(1);
         }

         inputFilename = args[0];

         numberOfSentences = Integer.parseInt(args[1]);
         if (numberOfSentences == 0) {
            System.err.println("ERROR !!! numberOfSentences may not be 0");
            System.exit(1);
         }

         numberOfFolds = Integer.parseInt(args[2]);
         if (numberOfFolds == 0) {
            System.err.println("ERROR !!! numberOfFolds may not be 0");
            System.exit(1);
         }

      } catch (Exception e) {
         System.err.println("ERROR !!! while parsing options: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }


      // ===== INITIALIZATION =====

      ParseWriterCoNLL out = null;
      ParseReaderCoNLL parseReader = null;

      try{
         // Open input parsed sentences file
         parseReader = new ParseReaderCoNLL(inputFilename);
         parseReader.openFile();
         //System.out.println();

         // open output file for parsed sentences
         out = new ParseWriterCoNLL(inputFilename + "." + 1);

      } catch (Exception e) {
         System.err.println("ERROR in initialization: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }


      // ===== PROCESSING =====

      SRLSentence sentence;
      int fold = 1;
      int lineno = 0;
      int foldsize = (int) java.lang.Math.ceil(numberOfSentences/numberOfFolds);

      while (!(sentence = parseReader.readParseSRL()).isEmpty()) {

         // Create folds for output parses
         if (lineno % foldsize == 0) {
            if (fold <= numberOfFolds) {
               // last few sentences are just added to last fold
               if (fold != 1) {
                  Fileutils.closeSilently(out);
               }
               //System.out.println("Start fold: " + fold);
               out= new ParseWriterCoNLL(inputFilename + "." + fold);
               fold++;
            }
         }

         // TODO
         if (lineno > numberOfSentences) {
            break;
         }

         try {
            // Write new parse with SRL annotation
            out.writeParse(sentence);

         } catch (Exception e) {
            e.printStackTrace();
            continue;
         }

         lineno++;

      }


      // ===== STATISTICS, CLEANUP =====


      //Close files and clean up
      Fileutils.closeSilently(parseReader);
      Fileutils.closeSilently(out);

   }


}
