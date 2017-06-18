// (c) Wiltrud Kessler
// 10.4.2014
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.comparatives.ims;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotation;
import de.uni_stuttgart.ims.corpus.compannotation.SentenceAnnotation;
import de.uni_stuttgart.ims.nlpbase.nlp.PredicateType;
import de.uni_stuttgart.ims.nlpbase.nlp.PredicateDirection;
import de.uni_stuttgart.ims.corpus.util.Options;
import de.uni_stuttgart.ims.util.Fileutils;


/**
 * Filter comparisons from a file in the IMS format.
 *
 * @author kesslewd
 *
 */
public class IMSExtractor {

   /**
    * Do NOT use comparisons that are
    * - type difference
    * - type ranked/superlative and direction negative
    * - how to treat equative -> see useEquative
    */
   private static boolean onlyPositive = false;

   /**
    * In combination with onlyPositive:
    * - if true : include equative
    * - if false: exclude equative
    */
   private static boolean useEquative = true;



   /**
    * Read all sentences.
    * Go through annotation, try to find sentence.
    * If successful, check if this is a comparison.
    * Check other requirements.
    * Then write to sentence/annotation files.
    * If not, ignore.
    *
    *
    * @param args Config file
    */
   public static void main(String[] args) {

      // ===== GET USER-DEFINED OPTIONS =====

      try {

         if (args.length < 1) {
            System.err.println("Usage: IMSExtractor <config file>");
            System.exit(1);
         }
         Options.parseOptionsFile(args[0]);

      } catch (IOException e) {
         System.err.println("ERROR !!! while parsing options file: " + e);
         System.exit(1);
      }

      String sentencesInputFilename = Options.getOption("inputSentencesFilename");
      String[] annotationsInputFilename = Options.getListOption("inputAnnotationsFilename");
      String annotationsOutputFilename = Options.getOption("plaintextAnnotationsFilename");
      String sentencesOutputFilename = Options.getOption("plaintextSentencesFilename");


      // ===== INITIALIZATION =====

      // Things for I/O that we need later
      BufferedReader inSentences = null;
      BufferedReader inAnnotations = null;
      BufferedWriter outSentences = null;
      BufferedWriter outAnnotations = null;

      try {

         // open input file (sentences)
         inSentences = Fileutils.getReadFile(sentencesInputFilename);

         // open output file (sentences)
         outSentences = Fileutils.getWriteFile(sentencesOutputFilename);

         // open output file (annotations)
         outAnnotations = Fileutils.getWriteFile(annotationsOutputFilename);


      } catch (Exception e) {
         System.err.println("ERROR !!! in initialization: " + e);
         e.printStackTrace();
         System.exit(1);
      }



      // ===== PROCESSING =====

      // Read sentences
      // Format: id \t sentence

      String strLine;
      HashMap<String, String> sentences = new HashMap<String, String>();

      try {
         while ((strLine = inSentences.readLine()) != null) {

            String[] parts = strLine.split("\t");

            sentences.put(parts[0].trim(), parts[1].trim());

         }
      } catch (Exception e) {
         e.printStackTrace();
      }


      // Read annotations
      // Format: id \t [annotations, sentence, etc.] (don't care)

      int errorNo = 0;
      int totalsentences = 0;
      int comparatives = 0;
      int ignored = 0;

      for (int i=0; i<annotationsInputFilename.length; i++) {

         // open input file (annotations)
         try {
            inAnnotations = Fileutils.getReadFile(annotationsInputFilename[i]);
         } catch (Exception e) {
            System.err.println("ERROR !!! reading annotation file: " + annotationsInputFilename[i] + e);
            e.printStackTrace();
            System.exit(1);
         }


         try {
            while ((strLine = inAnnotations.readLine()) != null) {
               totalsentences+=1;

               String[] parts = strLine.split("\t");
               if (parts[1].equals("0")) { // non-comp
                  continue;
               }

               String thisSentence = sentences.get(parts[0]);

               if (thisSentence != null) { // have found sentence

                  // Check annotation
                  SentenceAnnotation result = wantThisAnnotation(strLine);
                  if (result != null) {
                     outSentences.write(thisSentence + "\n");
                     outAnnotations.write(result + "\n");
                     comparatives += 1;
                  } else {
                     ignored++;
                  }
               } else {
                  System.out.println("error, could not find sentence for annotation id " + parts[0]);
                  errorNo+= 1;
               }


            }
            outAnnotations.flush();
            outSentences.flush();
         } catch (IOException e) {
            e.printStackTrace();
         }

      }

      // The important statistics
      System.out.println("total sentences: " + totalsentences);
      System.out.println("total non-comparatives: " + (totalsentences - comparatives));
      System.out.println("total comparatives: " + comparatives);
      System.out.println("total ignored: " + ignored);
      System.out.println("number of errors (see System.err): " + errorNo);

      // Do some cleanup
      Fileutils.closeSilently(inSentences);
      Fileutils.closeSilently(inAnnotations);
      Fileutils.closeSilently(outSentences);
      Fileutils.closeSilently(outAnnotations);
      System.out.println("done.");


   }

   /**
    * Get sentence annotation & all the comparison annotations
    * only IF we want them.
    */
   private static SentenceAnnotation wantThisAnnotation(String strLine) {

      try {
         SentenceAnnotation annie = new SentenceAnnotation(strLine);

         Iterator<ComparisonAnnotation> itty = annie.getComparisonAnnotationsIterator();

         ArrayList<ComparisonAnnotation> listy = new ArrayList<ComparisonAnnotation>();

         while (itty.hasNext()) {
            ComparisonAnnotation compie = itty.next();

            // Check direction
            if (onlyPositive) {
               PredicateType type = compie.getFineType();
               PredicateDirection dir = compie.getDirection();

               if (type == PredicateType.equative) {
                  if (!useEquative)
                     listy.add(compie);
                  // everything fine
               } else if (type == PredicateType.ranked | type == PredicateType.superlative) {
                  if (dir == PredicateDirection.SUPERIOR | dir == PredicateDirection.UNDEFINED) {
                     // everything fine
                  } else { // INFERIOR
                     listy.add(compie);
                  }

               } else { // DIFFERENCE or UNKNOWN
                  listy.add(compie);
               }

            }


         }

         for (ComparisonAnnotation compie : listy) {
            annie.removeComparisonAnnotation(compie);
         }

         if (annie.getNumberOfComparisons() > 0) {
            return annie;
         } else {
            return null;
         }
      } catch (Exception e) {
         System.out.println("ERROR in annotation, ignore: " + strLine);
         return null;
      }
   }

}
