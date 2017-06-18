// (c) Wiltrud Kessler
// 27.01.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/

package de.uni_stuttgart.ims.corpus.srlannotation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import de.uni_stuttgart.ims.util.Fileutils;
import de.uni_stuttgart.ims.nlpbase.io.ParseReaderCoNLL;
import de.uni_stuttgart.ims.nlpbase.io.ParseWriterCoNLL;
import de.uni_stuttgart.ims.nlpbase.nlp.ArgumentType;
import de.uni_stuttgart.ims.nlpbase.nlp.PredicateDirection;
import de.uni_stuttgart.ims.nlpbase.nlp.PredicateType;
import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotation;
import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotationToken;
import de.uni_stuttgart.ims.corpus.compannotation.SentenceAnnotation;
import de.uni_stuttgart.ims.corpus.util.ArgumentNotFoundException;
import de.uni_stuttgart.ims.corpus.util.Options;
import de.uni_stuttgart.ims.corpus.util.PredicateNotFoundException;
import de.uni_stuttgart.ims.nlpbase.nlp.SRLSentence;
import de.uni_stuttgart.ims.nlpbase.nlp.Word;


/**
 * Annotate the parsed file with comparison annotations.
 */
public class CreateSRLTrainingData {


   static int predicatesNotFound = 0;
   static int argumentsNotFound = 0;
   static private int reorder = 0;



   /**
    * @param args
    * @throws IOException
   */
   public static void main(String[] args) {



      // ===== GET USER-DEFINED OPTIONS =====

      try {
         if (args.length < 1) {
            System.err.println("Usage: CreateSRLTrainingData <config file> ");
            System.exit(1);
         }

         String optionsFilename = args[0];

         System.out.println("Read options file " + optionsFilename);
         Options.parseOptionsFile(optionsFilename);

      } catch (IOException e) {
         System.err.println("ERROR !!! while parsing options file: " + e);
         System.exit(1);
      }

      String sentencesInputFilename = Options.getOption("parsedSentencesFilename");
      String annotationsInputFilename = Options.getOption("plaintextAnnotationsNormFilename");
      String outputFilename = Options.getOption("srlAnnotatedSentencesFilename");

      String markerA = Options.getOption("useArgumentMarker");
      ArgumentType.setArgumentMarker(markerA);

      boolean orderEntitiesByPreferred = false;
      boolean orderEntitiesBySurface = false;
      String annOption = Options.getOption("annOption");
      if (annOption.equals("pref"))
         orderEntitiesByPreferred = true;
      if (annOption.equals("surf"))
         orderEntitiesBySurface = true;

      boolean printNonComp = false;

      // ===== INITIALIZATION =====

      BufferedReader brAnnotations = null;
      ParseWriterCoNLL out = null;
      ParseReaderCoNLL parseReader = null;

      try{
         // Open input parsed sentences file
         parseReader = new ParseReaderCoNLL(sentencesInputFilename);
         parseReader.openFile();

         // open input file with annotations
         brAnnotations = Fileutils.getReadFile(annotationsInputFilename);

         // open output file for parsed sentences
         out = new ParseWriterCoNLL(outputFilename);

      } catch (Exception e) {
         System.err.println("ERROR in initialization: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }


      // ===== PROCESSING =====

      SRLSentence sentence;
      int lineno = 0;
      int linesprintedno = 0;
      int errorNo = 0;
      int comparativePredsNo = 0;

      while (!(sentence = parseReader.readParseSRL()).isEmpty()) {

         String nextLine = null;
         try {
            nextLine = brAnnotations.readLine();
         } catch (IOException e) {
            System.err.println("Error while reading annotations file at line " + lineno + "!");
            e.printStackTrace();
            break;
         }

         if (nextLine == null) {
            System.err.println("Error, annotations file ended unexpectedly at line " + lineno + "!");
            break;
         }

         lineno++;

         // Get annotation
         SentenceAnnotation thisLineAnnotation = new SentenceAnnotation(nextLine);

         // Add annotations to parse
         Iterator<ComparisonAnnotation> iter = thisLineAnnotation.getComparisonAnnotationsIterator();
         String id = thisLineAnnotation.getId();
         while (iter.hasNext()) {
            ComparisonAnnotation comparison = iter.next();


            // Change order of entities if necessary
            // either by preferred entity [call this for IMS corpus]
            if (orderEntitiesByPreferred) {
               reorderEntitiesByPreferred(comparison);
            }
            // or by surface order  [call this for JDPA corpus]
            if (orderEntitiesBySurface) {
               reorderEntitiesByID(comparison);
            }


            try {

               // Predicate
               Word pred = SRLHelper.identifyPredicate(sentence, comparison.getPredicate());
               sentence.addPredicate(pred, comparison.getFineType(), comparison.getDirection());
               comparativePredsNo++;

               // Arguments
               identifyArguments(id, sentence, pred, comparison.getEntity1(), ArgumentType.entity1);
               identifyArguments(id, sentence, pred, comparison.getEntity2(), ArgumentType.entity2);
               identifyArguments(id, sentence, pred, comparison.getAspect(), ArgumentType.aspect);
               identifyArguments(id, sentence, pred, comparison.getSentiment(), ArgumentType.sentiment);

            } catch (PredicateNotFoundException e) {
               System.out.println(e.getMessage() + "\n"
                     + " in sentence " + id + ": " + sentence.toString());
               predicatesNotFound++;
            }

         }

         // Write new parse with SRL annotation to file (if there is at least 1 predicate)
         if (printNonComp | sentence.getPredicates().size() > 0) {
            try {
               out.writeParse(sentence);
               linesprintedno++;
            } catch (Exception e) {
               e.printStackTrace(); // TODO do something useful
               errorNo++;
            }
         }

      }


      // ===== STATISTICS, CLEANUP =====


      System.out.println("Read " + lineno + " sentences.");
      System.out.println("Printed " + linesprintedno + " sentences.");
      System.out.println("reordered: " + reorder + " entities\n");
      System.out.println("Found " + errorNo + " sentences with errors.");
      System.out.println("Caused by " + predicatesNotFound + " predicates that could not be found.");
      System.out.println("Caused by " + argumentsNotFound + " arguments that could not be found.");
      System.out.println("total comparative predicates found: " + comparativePredsNo);

      //Close files and clean up
      Fileutils.closeSilently(parseReader);
      Fileutils.closeSilently(out);
      Fileutils.closeSilently(brAnnotations);
      System.out.println("... done.");

   }



   /**
    * Find the arguments in tree.
    */
   public static void identifyArguments
         (String id, SRLSentence sentence, Word pred, List<ComparisonAnnotationToken[]> arguments, ArgumentType type)  {
      for (ComparisonAnnotationToken[] argument : arguments) {
         if (argument != null && !(argument.length == 0)) {
            try {
               Word argumentWord = SRLHelper.identifyArgument(sentence, argument);
               sentence.addArgument(pred, argumentWord, type);
            } catch (ArgumentNotFoundException e) {
               System.out.println(e.getMessage()
                     + " in sentence " + id + ": " + sentence.toString());
               argumentsNotFound++;
            }
         }
      }
   }


   /**
    * Change order of entities
    * if we want to order by ID, reorder if necessary
    */
   public static void reorderEntitiesByID(ComparisonAnnotation annotation) {

      List<ComparisonAnnotationToken[]> entity1listNew = annotation.getEntity1();
      List<ComparisonAnnotationToken[]> entity2listNew = annotation.getEntity2();

      if (entity1listNew != null & entity2listNew != null) {

         if (!entity1listNew.isEmpty() & !entity2listNew.isEmpty()) {
            int firstIndex1 = entity1listNew.get(0)[0].tokenNumber;
            int firstIndex2 = entity2listNew.get(0)[0].tokenNumber;
            if (firstIndex1 > firstIndex2) {// swap
               System.out.println("reorder: " + entity1listNew + " / " + entity2listNew);
               reorder++;
               annotation.setEntity1(entity2listNew);
               annotation.setEntity2(entity1listNew);
            }
         }
      }

   }



   /**
    * Change order of entities
    * if we want to order by preferred, reorder if necessary
    * call this only on IMS corpus
    */
   public static void reorderEntitiesByPreferred(ComparisonAnnotation annotation) {

      List<ComparisonAnnotationToken[]> entity1listNew = annotation.getEntity1();
      List<ComparisonAnnotationToken[]> entity2listNew = annotation.getEntity2();

      if (entity1listNew != null & entity2listNew != null) {

         if (!entity1listNew.isEmpty() & !entity2listNew.isEmpty()) {

            if ((annotation.getFineType() == PredicateType.ranked && annotation.getDirection() == PredicateDirection.INFERIOR)
              || (annotation.getFineType() == PredicateType.superlative && annotation.getDirection() == PredicateDirection.INFERIOR) ){

               // swap
               System.out.println("reorder: " + entity1listNew + " / " + entity2listNew);
               reorder++;
               annotation.setEntity1(entity2listNew);
               annotation.setEntity2(entity1listNew);
            }
         }
      }

   }



}
