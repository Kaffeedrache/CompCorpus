// (c) Wiltrud Kessler
// 27.1.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.comparatives.usage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;

import de.uni_stuttgart.ims.corpus.comparatives.usage.USAGEAnnotationConverter.USAGEPhraseAnnotation;
import de.uni_stuttgart.ims.corpus.comparatives.usage.USAGEAnnotationConverter.USAGERelationAnnotation;
import de.uni_stuttgart.ims.corpus.util.Options;
import de.uni_stuttgart.ims.nlpbase.tools.SentenceSplitter;
import de.uni_stuttgart.ims.nlpbase.tools.SentenceSplitterStanford;
import de.uni_stuttgart.ims.nlpbase.tools.TextSpan;
import de.uni_stuttgart.ims.nlpbase.tools.Tokenizer;
import de.uni_stuttgart.ims.nlpbase.tools.TokenizerStanford;
import de.uni_stuttgart.ims.util.Fileutils;



/**
 *
 * Converts a document with JDPA Knowtator annotations into
 * annotations that we can use for SRL later.
 *
 * Writes out all comparative sentences and their annotations.
 * Discards annotations that do not correspond to a complete token
 * or are not in the same sentence.
 *
 * @author kesslewd
 *
 */
public class USAGEExtractor {

   /**
    * @param args
    */
   public static void main(String[] args) {

      // ===== GET USER-DEFINED OPTIONS =====

      try {

         if (args.length < 1) {
            System.err.println("Usage: USAGEExtractor <config file>");
            System.exit(1);
         }
         Options.parseOptionsFile(args[0]);

      } catch (IOException e) {
         System.err.println("ERROR !!! while parsing options file: " + e);
         System.exit(1);
      }

      String sentencesInputFilename = Options.getOption("inputCorpusFilename");
      String annotationsInputFilenameSpans = Options.getOption("inputAnnotationsFilenameSpans");
      String annotationsInputFilenameRelations = Options.getOption("inputAnnotationsFilenameRelations");
      String annotationsOutputFilename = Options.getOption("plaintextAnnotationsFilename");
      String sentencesOutputFilename = Options.getOption("plaintextSentencesFilename");


      // ===== INITIALIZATION =====

      // Things for I/O that we need later
      BufferedReader inSentences = null;
      BufferedReader inAnnotationSpans = null;
      BufferedReader inAnnotationRelations = null;
      BufferedWriter outSentences = null;
      BufferedWriter outAnnotations = null;
      SentenceSplitter sentenceSplitter = null;
      Tokenizer tokenizer = null;
      USAGEAnnotationConverter converter = null;

      try {

         // open input file (sentences)
         DataInputStream in1 = new DataInputStream(new FileInputStream(sentencesInputFilename));
         inSentences = new BufferedReader(new InputStreamReader(in1, Charset.forName("UTF-8")));

         // open input file (annotation spans)
         DataInputStream in2 = new DataInputStream(new FileInputStream(annotationsInputFilenameSpans));
         inAnnotationSpans = new BufferedReader(new InputStreamReader(in2, Charset.forName("UTF-8")));

         // open input file (annotation relations)
         DataInputStream in3 = new DataInputStream(new FileInputStream(annotationsInputFilenameRelations));
         inAnnotationRelations = new BufferedReader(new InputStreamReader(in3, Charset.forName("UTF-8")));

         // open output file (sentences)
         FileWriter fstream1 = new FileWriter(sentencesOutputFilename);
         outSentences = new BufferedWriter(fstream1);

         // open output file (annotations)
         FileWriter fstream2 = new FileWriter(annotationsOutputFilename);
         outAnnotations = new BufferedWriter(fstream2);


         // Initialize sentence splitter (Stanford)
         sentenceSplitter = new SentenceSplitterStanford();

         // Initialize tokenizer (Stanford)
         tokenizer = new TokenizerStanford();

         // Converts USAGE annotations to our format
         converter = new USAGEAnnotationConverter();
         converter.setOutputFileSentences(outSentences);
         converter.setOutputFileAnnotations(outAnnotations);
         converter.setTokenizer(tokenizer);
         converter.subjHead = false;

      } catch (Exception e) {
         System.err.println("ERROR !!! in initialization: " + e);
         e.printStackTrace();
         System.exit(1);
      }



      // ===== PROCESSING =====

      int errorNo = 0;


      // --- COLLECT TEXT AND ANNOTATIONS ---

      // Collect all sentences from all reviews
      // Format: review-id \t Amazon-Product-ID \t Amazon-Review-ID \t title \t text

      // Mapping  review id -> review text
      HashMap<String, String> sentences = new HashMap<String, String>();
      String strLine;
      int lineno = 0;

      try {
         while ((strLine = inSentences.readLine()) != null) {
            lineno++;

            String[] parts = strLine.split("\t");
            if (parts.length != 6) {
               System.out.println("USAGE ANNOTATION WARNING !!! Line " + lineno + " doesn't have correct number of parts: " + strLine);
            }


            // Offsets are for title+text, so merge them together (separated by one char)
            sentences.put(parts[0], parts[parts.length-2] + " " + parts[parts.length-1]);
               // TODO in case line has different number of parts, take 4+5 or last two?

         }
      } catch (IOException e) {
         e.printStackTrace();
         errorNo++;
      }


      // Collect all annotations for spans from .csv
      // Format: class  \t review-id \t offset left \t offset right \t string \t phrase-ID \t polarity \t relation
      // Class is aspect ["aspect"] or subjective phrase ["subjective"]
      //     -> subjective = pred, aspect = arg
      //

      // Mapping  review id -> (phrase id -> attributes)
      HashMap<String, HashMap<String, USAGEPhraseAnnotation>> phraseAnnotations = new HashMap<String, HashMap<String, USAGEPhraseAnnotation>>();
      lineno = 0;

      try {
         while ((strLine = inAnnotationSpans.readLine()) != null) {
            lineno++;

            String[] parts = strLine.split("\t");
            if (parts.length != 8) {
               System.out.println("USAGE ANNOTATION WARNING !!! Line " + lineno + " doesn't have correct number of parts: " + strLine);
            }

            HashMap <String, USAGEPhraseAnnotation> reviewAnnotations = phraseAnnotations.get(parts[1]);
            if (reviewAnnotations == null) {
               reviewAnnotations = new HashMap <String, USAGEPhraseAnnotation>();
               phraseAnnotations.put(parts[1], reviewAnnotations);
            }
            if (reviewAnnotations.get(parts[5]) != null) {
               System.err.println("Error, overwriting annotation for phrase " + parts[5]);
            }
            reviewAnnotations.put(parts[5], new USAGEPhraseAnnotation(parts[2], parts[3], parts[4], parts[0], parts[5]));

         }
      } catch (IOException e) {
         e.printStackTrace();
         errorNo++;
      }


      // Collect all relations from .rel
      // Format: Relation-Type \t review id \t Phrase-ID1 \t Phrase-ID2 \t string1 \t string2

      // Class is aspect ["aspect"] or subjective phrase ["subjective"]
      //     -> subjective = pred, aspect = arg

      // Mapping  review id -> (phrase1 id_phrase2 id -> attributes)
      HashMap<String, HashMap<String, USAGERelationAnnotation>> relationAnnotations = new HashMap<String, HashMap<String, USAGERelationAnnotation>>();
      lineno = 0;

      try {
         while ((strLine = inAnnotationRelations.readLine()) != null) {
            lineno++;

            String[] parts = strLine.split("\t");
            if (parts.length != 6) {
               System.out.println("USAGE ANNOTATION WARNING !!! Line " + lineno + " doesn't have correct number of parts: " + strLine);
            }

            // Only extract target-subjphrase relations
            if (parts[0].equalsIgnoreCase("TARG-SUBJ")) {
               HashMap <String, USAGERelationAnnotation> reviewAnnotations = relationAnnotations.get(parts[1]);
               if (reviewAnnotations == null) {
                  reviewAnnotations = new HashMap <String, USAGERelationAnnotation>();
                  relationAnnotations.put(parts[1], reviewAnnotations);
               }
               if (reviewAnnotations.get(parts[2] + "_" + parts[3]) != null) {
                  System.err.println("Error, overwriting relation annotation for phrases " + parts[2] + "_" + parts[3]);
               }
               reviewAnnotations.put(parts[2] + "_" + parts[3], new USAGERelationAnnotation(parts[2], parts[3], parts[4], parts[5], parts[0]));
            }

         }
      } catch (IOException e) {
         e.printStackTrace();
         errorNo++;
      }





      // --- MERGE AND CREATE COMPLETE ANNOTATIONS ---

      // Iterate through all reviews and collect annotations
      int documentNo = 0;
      int totalsentencesNo = 0;
      int comparativesNo = 0;
      for (String reviewID : sentences.keySet()) {
         documentNo++;

         String text = sentences.get(reviewID);

         // Sentence splitting
         TextSpan[] sentenceSpans = sentenceSplitter.split(text);

         //System.out.println(reviewID + " : " + text);
         //System.out.println(reviewID);

         // Start new document in converter
         converter.startNewDocument(text, reviewID, sentenceSpans);

         totalsentencesNo += converter.getNumberOfSentences();
         comparativesNo += converter.getNumberOfComparativeSentences();

         HashMap <String, USAGERelationAnnotation> thisReviewRelationAnnotations = relationAnnotations.get(reviewID);
         if (thisReviewRelationAnnotations == null)
            continue;

         HashMap <String, USAGEPhraseAnnotation> thisReviewPhraseAnnotations = phraseAnnotations.get(reviewID);

         //for (USAGEPhraseAnnotation phr : thisReviewPhraseAnnotations.values()) {
        //    System.out.println(phr.phraseID + " \"" + phr.tokens + "\" [" + phr.leftOffset + "-" + phr.rightOffset + "] " + phr.type);
         //}


         for (USAGERelationAnnotation relation : thisReviewRelationAnnotations.values()) {
           // System.out.println("Add " + relation.PhraseID1 + " " + relation.tokensPhrase1 + " / " + relation.PhraseID2 + " " + relation.tokensPhrase2);
            converter.addAnnotation(relation, thisReviewPhraseAnnotations.get(relation.PhraseID1), thisReviewPhraseAnnotations.get(relation.PhraseID2));

         }


         // Error handling / logging?
         //          errorNo++;


      }



      // --- WRITE ANNOTATIONS TO FILE(S)---

      try {
         converter.writeAnnotations();
      } catch (IOException e) {
         System.err.println("ERROR !!! while writing sentence/annotaion to file: " + e);
         errorNo++;
      }




      // ===== STATISTICS, CLEANUP =====

      // The important statistics
      System.out.println("number of errors (see System.err): " + errorNo);

      // Some statistics
      System.out.println("number of predicate annotations not found: " + converter.getPredicatesNotFound());
      System.out.println("number of argument annotations not found: " + converter.getArgumentsNotFound());
      System.out.println("total documents: " + documentNo);
      System.out.println("total sentences: " + totalsentencesNo);
      System.out.println("total non-comparatives: " + (totalsentencesNo - comparativesNo));
      System.out.println("total comparatives: " + comparativesNo);

      // Do some cleanup
      Fileutils.closeSilently(sentenceSplitter);
      Fileutils.closeSilently(tokenizer);
      Fileutils.closeSilently(inSentences);
      Fileutils.closeSilently(inAnnotationSpans);
      Fileutils.closeSilently(inAnnotationRelations);
      Fileutils.closeSilently(outSentences);
      Fileutils.closeSilently(outAnnotations);
      System.out.println("done.");


   }

}
