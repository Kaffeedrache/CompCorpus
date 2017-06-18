// (c) Wiltrud Kessler
// 07.04.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.normalize;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotation;
import de.uni_stuttgart.ims.corpus.compannotation.SentenceAnnotation;
import de.uni_stuttgart.ims.corpus.util.Options;
import de.uni_stuttgart.ims.util.Fileutils;


/**
 * Do normalizations of sentences and annotations.
 * Which normalizations are performed, depends on the parameters.
 *
 * @author kesslewd
 *
 */
public class NormalizeSentence {

   /**
    * Recognize Named Entities.
    */
   private static boolean doNER = false;

   /**
    * In a regular multiword-predicate, annotate the adjective
    * as the predicate, otherwise annotate the predicate.
    */
   private static boolean annotateAdjective = false;

   /**
    * The predicates to be considered for regular multiword-predicates.
    */
   static List<String> exchangePreds = Arrays.asList(
         new String[] {"more", "less", "as", "most", "least"}); //, "compare", "like"});

   /**
    * Do splitting of multiword predicates (all of them)
    */
   private static boolean splitPred = false;





   /**
    * @param args
    * @throws Exception
    */
   public static void main(String[] args) throws Exception {

      // ===== GET USER-DEFINED OPTIONS =====

      try {

         if (args.length < 1) {
            System.err.println("Usage: NormalizeSentence <config file>");
            //System.err.println("Usage: NormalizeSentence <sentences file> [<annotations file>] [-idsorteq] [-idsortall] [-ner] [-annAdj] [-splitPred]");
            System.exit(1);
         }
         Options.parseOptionsFile(args[0]);

      } catch (IOException e) {
         System.err.println("ERROR !!! while parsing options file: " + e);
         System.exit(1);
      }

      String sentencesIn = Options.getOption("plaintextSentencesFilename");
      String annotationsIn = Options.getOption("plaintextAnnotationsFilename");
      String sentencesOut = Options.getOption("plaintextSentencesNormFilename");
      String annotationsOut = Options.getOption("plaintextAnnotationsNormFilename");
      String[] nerOptions = Options.getListOption("normOptions");
      System.out.println(Arrays.asList(nerOptions));
      boolean doAnnotations = false;

      if (annotationsIn != null && !annotationsIn.isEmpty()) {
         doAnnotations = true;
      }
      for (String opt : nerOptions) {
         if (opt.equals("-ner"))
            doNER = true;
         if (opt.equals("-adj"))
            annotateAdjective = true; // for those from list
         if (opt.equals("-splitPred"))
            splitPred = true;
      }
      System.out.println(
            "doNER " + doNER + "; "
            + "annotateAdjective " + annotateAdjective + "; "
            + "splitPred " + splitPred + "; "
            );


      // ========== INITIALIZE ==========


      BufferedReader brAnnotations = null;
      BufferedReader brNERSentences = null;
      BufferedWriter outSentences = null;
      BufferedWriter outAnnotations = null;

      try{

         // open input file with sentences
         brNERSentences = Fileutils.getReadFile(sentencesIn);
         System.out.println("Read sentences from: " + sentencesIn);

         // open input file with annotations
         if (doAnnotations) {
            brAnnotations = Fileutils.getReadFile(annotationsIn);
            System.out.println("Read annotations from: " + annotationsIn);
         }

         // open output file (sentences)
         outSentences = Fileutils.getWriteFile(sentencesOut);
         System.out.println("Write sentences to: " + sentencesOut);

         // open output file (annotations)
         if (doAnnotations) {
            outAnnotations = Fileutils.getWriteFile(annotationsOut);
            System.out.println("Write annotations to: " + annotationsOut);
         }

      } catch (Exception e) {
         System.err.println("ERROR in initialization: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }

      NERNormalizer ner = null;
      if (doNER) {
         ner = new NERNormalizer();
      }
      AnnotationNormalizer anny = new AnnotationNormalizer();




      // ========== PROCESSING ==========

      int longSentences = 0;

      // REad first sentence
      String thisLineAnn = null;
      String thisLineSentence = null;
      SentenceAnnotation thisLineAnnotation = null;
      int lineno = 0;
      try {
         lineno += 1;
         if (doAnnotations) {
         thisLineAnn = brAnnotations.readLine();
         }
         thisLineSentence = brNERSentences.readLine();
      } catch (IOException e) {
         System.err.println("Error while reading sentence/annotations file at line " + lineno + "!");
         e.printStackTrace();
      }


      // Go through all...

      while ((thisLineAnn != null | !doAnnotations) && thisLineSentence != null) {

         if (doAnnotations)
            try {
               thisLineAnnotation = new SentenceAnnotation(thisLineAnn);
            } catch (Exception e) {
             System.err.println("Error when creating sentence annotation from sentence " + lineno + ": " + thisLineAnn);
             e.printStackTrace();
             System.exit(1);
            }

         // Initialize tokens
         String[] tokens = thisLineSentence.split(" ");

         // Initialize indices, token indices as given by parser/annotation start at 1
         // -> leave 0 empty
         Integer[] newIndices = new Integer[tokens.length+1];
         for (int i=0; i<newIndices.length; i++) {
            newIndices[i] = i;
         }

         // Initialize replacements (null)
         // Enter only things to be replaced, will be checked for isNull
         String[] replacements = new String[tokens.length+1];



         /// ==================

         // Do NER on sentence (if wanted)

         if (doNER) {
            ner.doNormalize(tokens, newIndices, replacements);
         }

         /// ==================

         // Treat things the parser cannot treat, e.g., =

         TokenLevelReplacements.globalReplaceTokens(tokens, newIndices, replacements);


         /// ==================

         // Length cut off after 150 tokens

         int[] array = LengthNormalizer.getStartEnd(thisLineAnnotation, tokens.length);
         if (tokens.length > LengthNormalizer.tokenlimit) {
            printError("Sentence is too long (" + tokens.length + " tokens), print from " + array[0] + " to " + array[1] + " = " + (array[1] - array[0]), thisLineSentence);
            longSentences++;
         }
         LengthNormalizer.adjustReplacementIndices(array[0], array[1], newIndices, replacements);


         /// ==================


         String newSentence = TokenLevelReplacements.getSentenceString(tokens, newIndices, replacements);


         /// ==================


         if (doAnnotations) {

            thisLineAnnotation.setSentence(newSentence);

            try {

               // Iterate through annotations
               Iterator<ComparisonAnnotation> itty2 = thisLineAnnotation.getComparisonAnnotationsIterator();
               while (itty2.hasNext()) {
                  ComparisonAnnotation annotation = itty2.next();

                  // Change all token indices of all annotations
                  TokenLevelReplacements.convertAnnotation(annotation, newIndices, replacements);

                  // Normalize predicate if necessary
                  if (splitPred) {
                     anny.splitPredicate(annotation, tokens);
                  }

                  // Annotate adjective (sentiment) instead of 'more'
                  if (annotateAdjective & exchangePreds.contains(annotation.getPredicateString())) {
                     anny.changePredicate(annotation);
                  }


               }

            } catch (Exception e) {
               e.printStackTrace();
               System.err.println(newSentence);
               System.err.println(Arrays.toString(newIndices));
               System.err.println(Arrays.toString(replacements));
               System.exit(1);
            }


         }


         /// ==================


         // Write sentence to sentence file
         outSentences.write(newSentence);
         outSentences.newLine();
         outSentences.flush();

         if (doAnnotations) {

            // Write complete annotation to annotation file
            outAnnotations.write(thisLineAnnotation.toString());
            outAnnotations.newLine();
            outAnnotations.flush();

         }



         try {
            lineno += 1;

            if (doAnnotations) {
               thisLineAnn = brAnnotations.readLine();
            }
            thisLineSentence = brNERSentences.readLine();
         } catch (IOException e) {
            System.err.println("Error while reading annotations file at line " + lineno + "!");
            e.printStackTrace();
            break;
         }

      }


      // Statistics
      anny.getStatistics();
      System.out.println("LenghtNormalizer: " + longSentences + " sentences");

      if (doNER) {
         ner.writeDebug();
      }




      // Do some cleanup
      if (doAnnotations) {
      Fileutils.closeSilently(brAnnotations);
      Fileutils.closeSilently(outAnnotations);
      }
      Fileutils.closeSilently(brNERSentences);
      Fileutils.closeSilently(outSentences);
      System.out.println("done.");

   }




   public static void printError(String message, String sentence) {
     System.out.println("NORMALIZATION WARNING !!! " + message
              + " in sentence " + sentence.replaceAll("\n", " "));
   }


}
