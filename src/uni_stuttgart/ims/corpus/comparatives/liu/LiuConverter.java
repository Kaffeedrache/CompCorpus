// (c) Wiltrud Kessler
// 20.01.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.comparatives.liu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni_stuttgart.ims.corpus.util.Options;
import de.uni_stuttgart.ims.corpus.compannotation.SentenceAnnotation;
import de.uni_stuttgart.ims.nlpbase.tools.Tokenizer;
import de.uni_stuttgart.ims.nlpbase.tools.TokenizerStanford;
import de.uni_stuttgart.ims.util.Fileutils;



/**
 * Converts an annotaiton in the Jindal and Liu format to the IMS format.
 *
 * @author kesslewd
 *
 */
public class LiuConverter {


   // include annotations of type 4 (no entities or predicate annotated)
   //private static boolean useType4 = true;
   private static boolean useType4 = false;
   //private static boolean debugoutput = false;
   private static boolean debugoutput = false;

   /**
    * @param args
    * @throws IOException
   */
   public static void main(String[] args) {

      // ===== GET USER-DEFINED OPTIONS =====

      try {

         if (args.length < 1) {
            System.err.println("Usage: LiuConverter <config file>");
            System.exit(1);
         }
         Options.parseOptionsFile(args[0]);

      } catch (IOException e) {
         System.err.println("ERROR !!! while parsing options file: " + e);
         System.exit(1);
      }

      String filename = Options.getOption("inputCorpusFilename");
      String annotationsOutputFilename = Options.getOption("plaintextAnnotationsFilename");
      String sentencesOutputFilename = Options.getOption("plaintextSentencesFilename");


      // ===== INITIALIZATION =====

      // Things for I/O that we need later
      BufferedReader br = null;
      BufferedWriter outSentences = null;
      BufferedWriter outAnnotations = null;
      BufferedWriter outDebug = null;
      Tokenizer tokenizer = null;

      try {

         LiuAnnotation.setUseType4(useType4);

         // Initialize tokenizer (Stanford)
         tokenizer = new TokenizerStanford();
         LiuAnnotation.setTokenizer(tokenizer);

         // open input file
         DataInputStream in = new DataInputStream(new FileInputStream(filename));
         br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
         LiuAnnotation.setFilename(filename);

         // open output file (sentences)
         FileWriter fstream1 = new FileWriter(sentencesOutputFilename);
         outSentences = new BufferedWriter(fstream1);

         // open output file (annotations)
         FileWriter fstream2 = new FileWriter(annotationsOutputFilename);
         outAnnotations = new BufferedWriter(fstream2);

         // open output file (debug)
         if (debugoutput) {
            FileWriter fstream3 = new FileWriter(sentencesOutputFilename + ".debug");
            outDebug = new BufferedWriter(fstream3);
         }

      } catch (Exception e) {
         System.err.println("ERROR !!! in initialization: " + e);
         e.printStackTrace();
         System.exit(1);
      }



      // ===== PROCESSING =====


      // Bookkeeping
      String strLine = "asdf";
      boolean inComparativeSentence = false;
      boolean afterStars = false;
      int comparatives = 0;
      int comparativesFound = 0;
      int ignoreLines = 0;
      int lineno = 0;
      int totalsentences = 0;
      int errorNo = 0;
      int[] types = new int[5];
      LiuAnnotation thisLineAnnotation = null;

      // TEST !!!
      // todo: adapt to repaired file!
      // reviews -> 0 .. 5366
      // forums -> 5405 .. 6896
      // forums -> 8522 .. 10397
      // news -> 5367 .. 5405
      // news -> 6897 .. 8521
      // tokenized roughly until 8524
      // total 10403
      int min = 0;
      int max = 10403;


      while (strLine != null) {

         try {
            strLine = br.readLine();
         } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }
         if (strLine == null)
            break;

         strLine = strLine.trim();
         lineno++;

         // TEST !!!
         if (lineno < min) {
            continue;
         }
         if (lineno == min) {
            //System.out.println(strLine);
         }
         if (lineno > max) {
            //System.out.println(strLine);
            break;
         }

         // Skip empty lines
         if (strLine.equals("")) {
            continue;
         }

         // Recognize begin/end of heading (= line consisting only of stars)
         // -> ignore
         if (isStarLine(strLine)) {
            afterStars = !afterStars; // Toggle 'afterStars'
            continue;
         }

         // Recognize heading -> ignore
         if (afterStars) {
            continue;
         }

         // ===== ANNOTATIONS =====

         // Ignore a number of lines after an annotation tag
         // that contain the actual annotation.
         // The number of lines is defined in recognition
         // of the start of an annotation.
         // Format:
         // 1_this player 2_ipod 3_sound ()
         if (ignoreLines > 0 && !inComparativeSentence) {
            //System.out.println (lineno + ": " + "ignore (after annotation): " + strLine);

            errorNo += thisLineAnnotation.addEntities(strLine, thisLineAnnotation.linesToIgnore - ignoreLines);
            ignoreLines--;

            continue;
         }


         // Write completed annotation to files
         if (!inComparativeSentence && thisLineAnnotation != null && thisLineAnnotation.toWrite) {

            if (thisLineAnnotation.types.size() > 0) {

               SentenceAnnotation thisSentenceAnnotation = thisLineAnnotation.convertToSentenceAnnotation();

               if (thisSentenceAnnotation != null) {
                  comparativesFound++;

                  try {
                     outSentences.write(thisSentenceAnnotation.getSentence());
                     outSentences.newLine();
                     outSentences.flush();
                  } catch (IOException e) {
                     System.err.println("ERROR !!! while writing sentence to file: " + e);
                     System.err.println("in sentence " + thisSentenceAnnotation.getSentence());
                     errorNo++;
                     continue;
                     // TODO what is the best error handling here?
                  }

                  // Write annotation to annotation file
                  try {
                     outAnnotations.write(thisSentenceAnnotation.toString());
                     outAnnotations.newLine();
                     outAnnotations.flush();
                  } catch (IOException e) {
                     System.err.println("ERROR !!! while writing annotation to file: " + e);
                     System.err.println("annotation " + thisLineAnnotation);
                     System.err.println("in sentence " + thisSentenceAnnotation.getSentence());
                     errorNo++;
                     continue;
                     // TODO what is the best error handling here?
                  }
               }
            }

            thisLineAnnotation.toWrite = false;
         }

         // Recognize start of annotation for comparative sentence
         if (!inComparativeSentence) {
            thisLineAnnotation = getAnnotation(strLine);
            if (thisLineAnnotation.isAnnotation) {
               //System.out.println ("lineno + ": " + ignore (annotation start): " + strLine + " " + ignoreLines);
               ignoreLines = thisLineAnnotation.linesToIgnore;
               thisLineAnnotation.toWrite = true;
               for (Integer type: thisLineAnnotation.types) {
                  types[type]++;
               }
               inComparativeSentence = true;
               types[0]++;
               continue;
            }

         }

         // Recognize end of annotation for comparative sentence
         if (inComparativeSentence) {
            if (recognizeAnnotationEnd(strLine)) {
               inComparativeSentence = false;
               continue;
            }
         }



         // ===== CONTENT SENTENCES =====

         // Count comparative sentences and write to file
         if (inComparativeSentence) {
            // Add sentence to annotation
            thisLineAnnotation.sentence = strLine;
            // sanity check & debug

            if (thisLineAnnotation.types.size() > 0) {
               comparatives++;
            }
         }
         totalsentences++;

         if (debugoutput) {
            try {
               if (thisLineAnnotation.types.size() > 0) {
                  outDebug.write("1\t" + strLine);
               } else {
                  outDebug.write("0\t" + strLine);
               }
               outDebug.newLine();

            } catch (IOException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
         }

      }


      // ===== CLEANUP =====

      // The important statistics
      System.out.println("number of errors (see System.err): " + errorNo);
      System.out.println("number of predicate annotations not found: " + LiuAnnotation.getPredicatesNotFound());
      System.out.println("number of argument annotations not found: " + LiuAnnotation.getArgumentsNotFound());

      System.out.println("total sentences: " + totalsentences);
      System.out.println("total non-comparatives: " + (totalsentences - comparatives));
      System.out.println("total comparatives to be found: " + comparatives);
      System.out.println("total comparatives found: " + comparativesFound);
      System.out.println("types: " + Arrays.toString(types));

      // Do some cleanup
      Fileutils.closeSilently(br);
      Fileutils.closeSilently(outSentences);
      Fileutils.closeSilently(outAnnotations);
      Fileutils.closeSilently(outDebug);
      System.out.println("done.");

   }




   private static LiuAnnotation getAnnotation(String line) {

      String patternString = "";
      patternString = "(<cs-1>)|(<cs-2>)|(<cs-3>)|(<cs-4>)";
      Pattern pattern = Pattern.compile(patternString);

      Matcher matcher = pattern.matcher(line);

      int count = 0;
      int found = 0;
      List<Integer> types = new ArrayList<Integer>();
      while (matcher.find()) {
         Integer annoType = LiuAnnotation.findType(matcher.group());
         if (annoType != 4 || useType4) {
            types.add(annoType);
         }
         found++;
         if (annoType == 1 || annoType == 2 || annoType == 3) {
            count++;
         }
      }

      if (found == 0) {
         return new LiuAnnotation(false);
      } else {
         return new LiuAnnotation(true, types, count);
      }
   }


   private static boolean recognizeAnnotationEnd(String line) {
      // 1 error, line 1338: /cs-2>
      Pattern pattern = Pattern.compile("(</cs-.>)|(/cs-2>)");
      Matcher  matcher = pattern.matcher(line);
      return matcher.find();
   }


   private static boolean isStarLine(String line) {
      for (Character c : line.toCharArray()) {
         if (c != '*') {
            return false;
         }
      }
      return true;
   }

   static class SortByValueComparator implements Comparator<Map.Entry<String, Integer>> {
      public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
          return e1.getValue().compareTo(e2.getValue());
      }
   }


}
