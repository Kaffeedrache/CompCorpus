// (c) Wiltrud Kessler
// 19.02.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.comparatives.jdpa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jdom2.JDOMException;

import com.google.common.io.Files;

import de.uni_stuttgart.ims.util.Fileutils;
import de.uni_stuttgart.ims.corpus.util.Options;
import de.uni_stuttgart.ims.nlpbase.tools.SentenceSplitter;
import de.uni_stuttgart.ims.nlpbase.tools.SentenceSplitterStanford;
import de.uni_stuttgart.ims.nlpbase.tools.TextSpan;
import de.uni_stuttgart.ims.nlpbase.tools.Tokenizer;
import de.uni_stuttgart.ims.nlpbase.tools.TokenizerStanford;


/**
* Imports the JDPA corpus in Knowtator format and outputs a format
* that is used to create SRL annotated parses.
*
* The JDPA corpus can be found here:
* https://verbs.colorado.edu/jdpacorpus/
*
* There are two (optional 3) files created:
* - sentences file:
*   Contains all sentences that contain a comparison annotation.
*   Format: One sentence per line, tokenized with OpenNLP tokenizer.
* - annotations file:
*   Contains all annotation for a sentence.
* - optional HTML debug output
*
*
*/
public class JDPAConverter {


   private static boolean debug = false;

   /**
   * Expects a list of directories. The directories should have two subdirectories,
   * one for the plain text and one for the annotations.
   *    Example text file loc.:        <given path>/txt/camera-002-001.txt
   *    Example annotation file loc.:  <given path>/annotation/camera-002-001.txt.knowtator.xml
   */
   public static void main(String[] args) {


      // ===== GET USER-DEFINED OPTIONS =====

      try {

         if (args.length < 1) {
            System.err.println("Usage: JDPAConverter <config file>");
            System.exit(1);
         }
         Options.parseOptionsFile(args[0]);

      } catch (IOException e) {
         System.err.println("ERROR !!! while parsing options file: " + e);
         System.exit(1);
      }

      String[] paths = Options.getListOption("jdpaCorpusPaths");
      String sentencesOutputFilename = Options.getOption("plaintextSentencesFilename");
      String annotationsOutputFilename = Options.getOption("plaintextAnnotationsFilename");
      String htmlOutputFilename = Options.getOption("htmlMentionsOutputFilename");
      boolean writeHTMLOutput = (htmlOutputFilename != null);


      // ===== INITIALIZATION =====

      // Add names of JDPA annotation types to extract
      ArrayList<String> comparisonTypes = new ArrayList<String>();
      comparisonTypes.add("Comparison");
      ArrayList<String> sentimentTypes = new ArrayList<String>();
      sentimentTypes.add("SentimentBearingExpression");
      if (debug) System.out.println("Extract annotation of types " + comparisonTypes);

      // Things for I/O that we need later
      SentenceSplitter sentenceSplitter = null;
      Tokenizer tokenizer = null;
      KnowtatorXMLParser parser = null;
      BufferedWriter outSentences = null;
      BufferedWriter outAnnotations = null;
      HTMLDebugOutputJDPA outHTML = null;
      JDPAAnnotationConverter converter = null;

      try {

         // Initialize sentence splitter (Stanford)
         sentenceSplitter = new SentenceSplitterStanford();
         //sentenceSplitter.initializeOpenNLP();

         // Initialize tokenizer (Stanford)
         tokenizer = new TokenizerStanford();

         // Initialize Knowtator XML parser
         parser = new KnowtatorXMLParser();

         // open output file (sentences)
         FileWriter fstream1 = new FileWriter(sentencesOutputFilename);
         outSentences = new BufferedWriter(fstream1);

         // open output file (annotations)
         FileWriter fstream2 = new FileWriter(annotationsOutputFilename);
         outAnnotations = new BufferedWriter(fstream2);

         // HTML Debug output
         if (writeHTMLOutput) {
            outHTML = new HTMLDebugOutputJDPA(htmlOutputFilename);
            outHTML.initialize();
         }

         // Converts JDPA annotations to our format
         converter = new JDPAAnnotationConverter();
         converter.setOutputFileSentences(outSentences);
         converter.setOutputFileAnnotations(outAnnotations);
         converter.setTokenizer(tokenizer);


      } catch (Exception e) {
         System.err.println("ERROR !!! in initialization: " + e);
         e.printStackTrace();
         System.exit(1);
      }



      // ===== PROCESSING =====

      // Bookkeeping variables
      int documentNo = 0;
      int comparativesNo = 0;
      int totalsentencesNo = 0;
      int errorNo = 0;

      for (String knowtatorTextDirectoryPath : paths) {
         if (debug) System.out.println();
         if (debug) System.out.println("Process directory " + knowtatorTextDirectoryPath);
         File knowtatorTextDirectory = new File(knowtatorTextDirectoryPath + "txt");
         File knowtatorAnnotationsDirectory = new File(knowtatorTextDirectoryPath + "annotation");

         // Loop through all text files
         File[] files = knowtatorTextDirectory.listFiles();
         Arrays.sort(files);
         for (File textFile : files) {
            //if (debug) System.out.println();
            if (debug) System.out.println("Process file " + textFile.getAbsolutePath());

            // Try to find corresponding annotation file, abort if not found
            File anntationFile = new File(knowtatorAnnotationsDirectory + File.separator + textFile.getName()+".knowtator.xml");
            URI knowtatorURI = anntationFile.toURI();
            if (!anntationFile.exists()) {
               System.err.println("ERROR !!! Annotation file not found: " + knowtatorURI);
               errorNo++;
               continue;
            }

            // DEBUG count
            documentNo++;

            // Get plain text and normalize (replace windows line breaks)
            String text = "";
            try {
               text = Files.toString(textFile, Charset.forName("ASCII"));
            } catch (IOException e) {
               System.err.println("ERROR !!! while reading text file: " + e);
               errorNo++;
               continue;
            }
            text = text.replaceAll("\\r\\n", " ");
            parser.setPlaintext(text);


            // Sentence splitting
            TextSpan[] sentenceSpans = sentenceSplitter.split(text);

            // Start new document in converter
            converter.startNewDocument(text, anntationFile.getAbsolutePath(), sentenceSpans);

            // HTML Output DEBUG
            if (writeHTMLOutput) {
               outHTML.startNewDocument(text, sentenceSpans);
            }


            // --- PARSE XML ---

            // Parse the JDPA xml files to get the annotations
            // Parse the Knowtator XML file into annotation objects
            Collection<KnowtatorAnnotation> jdpaAnnotations = null;
            try {
               jdpaAnnotations = parser.parse(knowtatorURI);
            } catch (JDOMException e) {
               System.err.println("ERROR !!! while parsing file " + knowtatorURI + " : " + e);
               errorNo++;
               continue;
            } catch (IOException e) {
               System.err.println("ERROR !!! while parsing file " + knowtatorURI + " : " + e);
               errorNo++;
               continue;
            } // processed document


            // --- COLLECT ANNOTATIONS ---

            // Iterate through all JDPA annotations,
            // and collect the ones we are interested in.
            Iterator<KnowtatorAnnotation> iter = jdpaAnnotations.iterator();
            HashMap<Integer, List<KnowtatorAnnotation>> sentimentAnnotations = new HashMap<Integer, List<KnowtatorAnnotation>>();
            List<KnowtatorAnnotation> comparisonAnnotations = new ArrayList<KnowtatorAnnotation>();
            while (iter.hasNext()) {
               KnowtatorAnnotation current = iter.next();

               // HTML Output DEBUG
               if (writeHTMLOutput) {
                  outHTML.addHTMLDebugOutput(current);
               }

               // Collect annotation types we are interested in
               if (comparisonTypes.contains(current.type)) {
                  comparisonAnnotations.add(current);
               }
               if (sentimentTypes.contains(current.type)) {
                  Integer begin = current.getCoveringSpan().begin;
                  List<KnowtatorAnnotation> thislist = sentimentAnnotations.get(begin);
                  if (thislist == null) {
                     thislist = new ArrayList<KnowtatorAnnotation>();
                  }
                  thislist.add(current);
                  sentimentAnnotations.put(begin, thislist);
               }


            } // while (iter.hasNext()) {


            // Create comparison annotation objects
            // and add them to the corresponding sentences.
            for (KnowtatorAnnotation current : comparisonAnnotations) {
               //System.out.println("Annotation: " + current.type + " / "+ current.spannedText +
               //      " (" + current.spans + ")");
               //if (debug) System.out.println(current);
               converter.addAnnotation(current, sentimentAnnotations);

            }


            // --- WRITE ANNOTATIONS TO FILE(S)---

            try {
               converter.writeAnnotations();
            } catch (IOException e) {
               System.err.println("ERROR !!! while writing sentence/annotaion to file: " + e);
               System.err.println("in file " + textFile.getAbsolutePath());
               errorNo++;
               continue;
               // TODO what is the best error handling here?
            }

            // HTML Output (DEBUG)
            if (writeHTMLOutput) {
               outHTML.writeHTMLToFile(textFile.getAbsolutePath() + "<br/>");
            }


            // DEBUG counts
            totalsentencesNo += converter.getNumberOfSentences();
            comparativesNo += converter.getNumberOfComparativeSentences();

            // DEBUG
            //if (documentNo > 50)  break;

         } // file

         // DEBUG counts
         //System.out.println("Processed " + documentNo + " annotated documents.");
         //System.out.println("total comparatives: " + comparativesNo);
      }


      // ===== STATISTICS, CLEANUP =====

      // Some statistics
      System.out.println("number of errors (see System.err): " + errorNo);
      System.out.println("number of predicate annotations not found: " + converter.getPredicatesNotFound());
      System.out.println("number of argument annotations not found: " + converter.getArgumentsNotFound());
      System.out.println("total documents: " + documentNo);
      System.out.println("total sentences: " + totalsentencesNo);
      System.out.println("total non-comparatives: " + (totalsentencesNo - comparativesNo));
      System.out.println("total comparatives: " + comparativesNo);

      // Do some cleanup
      Fileutils.closeSilently(sentenceSplitter);
      Fileutils.closeSilently(tokenizer);
      Fileutils.closeSilently(outSentences);
      Fileutils.closeSilently(outAnnotations);
      Fileutils.closeSilently(outHTML);
      System.out.println("done.");

   }



   public static void printoutText(String text) {
      // DEBUG
      System.out.print("Text is (" + text.length() + ")");
      for (int i1=0; i1<text.length(); i1++) {
         if (i1 % 100 == 0) {
            System.out.println();
         }
         if (i1 % 10 == 0) {
            System.out.print("[" + i1 + "]");
         }
         if (text.charAt(i1) == '\r') {
            System.out.print("\\r");
         } else if (text.charAt(i1) == '\n') {
            System.out.print("\\n");
         } else {
            System.out.print(text.charAt(i1));
         }
      }
      System.out.println();
   }




}
