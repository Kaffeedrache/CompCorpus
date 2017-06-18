// (c) Wiltrud Kessler
// 27.1.2015
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.comparatives.usage;

import java.io.BufferedWriter;
import java.io.IOException;

import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotation;
import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotationToken;
import de.uni_stuttgart.ims.corpus.compannotation.SentenceAnnotation;
import de.uni_stuttgart.ims.corpus.util.PredicateNotFoundException;
import de.uni_stuttgart.ims.nlpbase.nlp.PredicateType;
import de.uni_stuttgart.ims.nlpbase.tools.TextSpan;
import de.uni_stuttgart.ims.nlpbase.tools.Tokenizer;



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
public class USAGEAnnotationConverter {

   /**
    * File to write tokenized sentences to (for later parsing).
    */
   private BufferedWriter outSentences;

   /**
    * File to write sentence annotations to.
    */
   private BufferedWriter outAnnotations;

   /**
    * Tokenizer
    */
   private Tokenizer tokenizer;

   /**
    * Global counter of predicates not found.
    */
   private static int predicatesNotFound = 0;

   /**
    * Global counter of arguments not found.
    */
   private static int argumentsNotFound = 0;


   /**
    * Per-document. Stores document name
    * (recorded as part of annotation).
    */
   private String id;

   /**
    * Per-document. Stores plain text of document.
    */
   private String text;

   /**
    * Per-document. Stores found sentence boundaries.
    */
   private TextSpan[] sentenceSpans;

   /**
    * Per-document. Stores all found annotations by sentence.
    * Zero or one annotation per sentence.
    * Length: sentenceSpans.length
    */
   private SentenceAnnotation[] sentenceAnnotations;

   /**
    * Per-document. Stores found token boundaries
    * of the sentences that have been identified in sentenceSpans.
    * Length: sentenceSpans.length
    */
   private TextSpan[][] sentenceTokenizations;

   /**
    * Use subjective phrase as head instead of aspect.
    */
   public boolean subjHead = false;


   static class USAGEPhraseAnnotation {
      public int leftOffset;
      public int rightOffset;
      public String tokens;
      public String type;
      public String phraseID;

      public USAGEPhraseAnnotation (String leftOffset, String rightOffset, String tokens, String type, String phraseID) {
         this.leftOffset = Integer.parseInt(leftOffset);
         this.rightOffset = Integer.parseInt(rightOffset);
         this.tokens = tokens;
         this.type = type;
         this.phraseID = phraseID;
      }
   }



   static class USAGERelationAnnotation {
      public String PhraseID1;
      public String PhraseID2;
      public String tokensPhrase1;
      public String tokensPhrase2;
      public String type;

      public USAGERelationAnnotation (String PhraseID1, String PhraseID2, String tokensPhrase1, String tokensPhrase2, String type) {
         this.PhraseID1 = PhraseID1;
         this.PhraseID2 = PhraseID2;
         this.tokensPhrase1 = tokensPhrase1;
         this.tokensPhrase2 = tokensPhrase2;
         this.type = type;
      }
   }


   // ===== SETTER =====


   /**
    * File to write tokenized sentences to (for later parsing)..
    */
   public void setOutputFileSentences(BufferedWriter outSentences) {
      this.outSentences = outSentences;
   }

   /**
    * File to write sentence annotations to.
    */
   public void setOutputFileAnnotations(BufferedWriter outAnnotations) {
      this.outAnnotations = outAnnotations;
   }

   /**
    * Tokenizer of sentences.
    */
   public void setTokenizer(Tokenizer tokenizer) {
      this.tokenizer = tokenizer;
   }


   // ===== GETTER =====

   /**
    * Returns number of sentences identified by sentence splitter.
    */
   public int getNumberOfSentences() {
      return sentenceSpans.length;
   }

   /**
    * Returns number of comparative sentences, i.e., number of
    * identified sentences where at least one annotation was found.
    */
   public int getNumberOfComparativeSentences() {
      int comparativeSentences = 0;
      for (int i=0; i<sentenceAnnotations.length; i++) {
         if (sentenceAnnotations[i] != null) {
            comparativeSentences++;
         }
      }
      return comparativeSentences;
   }

   /**
    * Returns number of predicates that could not be found,
    * to know the reason, see log.
    * Equivalent to cought PredicateNotFoundExceptions.
    */
   public int getPredicatesNotFound() {
      return predicatesNotFound;
   }

   /**
    * Returns number of arguments that could not be found,
    * to know the reason, see log.
    * Equivalent to cought ArgumentNotFoundExceptions.
    */
   public int getArgumentsNotFound() {
      return argumentsNotFound;
   }



   // ===== DOCUMENT LEVEL =====


   /**
    * Reset all per-document variables to begin a new document.
    *
    * @param text Document plain-text
    * @param filename Document name (= source stored as part of annotation)
    */
   public void startNewDocument (String text, String id, TextSpan[] sentenceSpans) {
      // Reset all per-document variables
      this.id = id;
      this.text = text;
      this.sentenceSpans = sentenceSpans;

      // Prepare arrays to store annotations and tokenizations for each sentence
      sentenceAnnotations = new SentenceAnnotation[sentenceSpans.length];
      sentenceTokenizations = new TextSpan[sentenceSpans.length][];

   }


   // ===== ANNOTATION LEVEL =====


   public void addAnnotation (USAGERelationAnnotation relation,
         USAGEPhraseAnnotation phrase1, USAGEPhraseAnnotation phrase2) {

      if (phrase1 == null) {
         printError("Phrase 1 is null for relation: " + relation, 0);
         return;
      }
      if (phrase2 == null) {
         printError("Phrase 2 is null for relation: " + relation, 0);
         return;
      }

      // Find the sentence this annotation occurrs in.
      // We look at the covering span, not all spans, because there should
      // usually only be one span.
      int sentenceNo1 = this.findCorrespondingSentence(phrase1.leftOffset, phrase1.rightOffset);
      if (sentenceNo1 == -1) {
         printError("The annotation is outside of the text: offsets are "
                 + phrase1.leftOffset + "-" + phrase1.rightOffset + " but textlength is " + text.length() + " (" + phrase1.phraseID + ")", -1);
         return;
      }
      int sentenceNo2 = this.findCorrespondingSentence(phrase2.leftOffset, phrase2.rightOffset);
      if (sentenceNo2 == -1) {
         printError("The annotation is outside of the text: offsets are "
                 + phrase2.leftOffset + "-" + phrase2.rightOffset + " but textlength is " + text.length() + " (" + phrase2.phraseID + ")", -1);
         return;
      }
      if (sentenceNo1 != sentenceNo2) {
         printError("The annotations are not in the same sentence: " + phrase1.tokens + " (" + phrase1.phraseID + ")"
               + " is in " +  sentenceNo1 + " vs. " + phrase2.tokens + " (" + phrase2.phraseID + ") in "+ sentenceNo2, -1);
         return;
      }
      TextSpan sentenceSpan = sentenceSpans[sentenceNo1];
      String thisSentence = (String) sentenceSpan.getCoveredText(text);



      USAGEPhraseAnnotation predicateAnnotation = phrase1; // aspect
      USAGEPhraseAnnotation argumentAnnotation = phrase2; // subjective phrase

      if (subjHead ) { // switch predicate and argument
          predicateAnnotation = phrase2; // subjective phrase
          argumentAnnotation = phrase1; // aspect
      }


      // If there is no annotation for this sentence yet, ...
      if (sentenceAnnotations[sentenceNo1] == null) {

         // Create a new annotation
         sentenceAnnotations[sentenceNo1] = new SentenceAnnotation();
         sentenceAnnotations[sentenceNo1].setComparative(true);
         sentenceAnnotations[sentenceNo1].setSource(id);

         // Tokenize sentence
         thisSentence = thisSentence.replaceAll("\\n", " ");
         sentenceTokenizations[sentenceNo1] = tokenizer.getTokenizationSpans(thisSentence);

      }
      SentenceAnnotation thisSentenceAnnotation = sentenceAnnotations[sentenceNo1];

      ComparisonAnnotation newAnnotation = new ComparisonAnnotation();

      newAnnotation.setFineType(PredicateType.undefined);

      try {

         ComparisonAnnotationToken[] tokens = checkAndMakeTokens (predicateAnnotation, "Predicate", sentenceNo1);
         newAnnotation.setPredicate(tokens);
      } catch (PredicateNotFoundException e) {
         this.printError("Predicate not found: " + e.getMessage(), sentenceNo1);
         predicatesNotFound++;
         thisSentenceAnnotation.addAnnotationError(e.getMessage());
         return;
      }

      // Entity 1
      try {

         ComparisonAnnotationToken[] tokens = checkAndMakeTokens (argumentAnnotation, "Argument", sentenceNo1);
         newAnnotation.addEntity1(tokens);

      } catch (PredicateNotFoundException e) {
         this.printError("Argument not found: " + e.getMessage(), sentenceNo1);
         argumentsNotFound++;
         thisSentenceAnnotation.addAnnotationError(e.getMessage());
      }

      thisSentenceAnnotation.addComparisonAnnotation(newAnnotation);

   }







   private int findCorrespondingSentence(int spanBegin, int spanEnd) {
      for (int i=0; i<sentenceSpans.length; i++) {
         TextSpan sentenceSpan = sentenceSpans[i];
         if (sentenceSpan.contains(spanBegin) & sentenceSpan.contains(spanEnd-1)) {
            return i;
         }
      }
      return -1;
   }



   private ComparisonAnnotationToken[] mapToTokens (int spanBegin, int spanEnd, int sentenceNo) {

      TextSpan[] tokenSpans = sentenceTokenizations[sentenceNo];
      TextSpan sentenceSpan = sentenceSpans[sentenceNo];


      int first = -1;
      int last = -1;

      for (int i=0; i<tokenSpans.length; i++) {
         TextSpan thisTokenSpan = tokenSpans[i];
         //System.out.println("check this token + " + i + ": " + thisTokenSpan + " [" + thisTokenSpan.getCoveredText(sentenceText) + "] = " + (sentenceSpan.begin + thisTokenSpan.begin) + " " + (sentenceSpan.begin + thisTokenSpan.end));
         if ((sentenceSpan.begin + thisTokenSpan.begin) == spanBegin) {
            //System.out.println("found first");
            first = i;
         }
         if ((sentenceSpan.begin + thisTokenSpan.end) == spanEnd) {
            last = i;
         }
      }

      // Not a complete token
      if (first == -1 | last == -1) {
         return null;
      }

      ComparisonAnnotationToken[] tokens = new ComparisonAnnotationToken[last-first+1];

      for (int j=0; j<(last-first+1); j++) {
         tokens[j] = new ComparisonAnnotationToken(tokenSpans[first+j].coveredText, first+1+j);
      }

      return tokens;
   }


   private ComparisonAnnotationToken[] checkAndMakeTokens  (
         USAGEPhraseAnnotation annotation, String type, int sentenceNo1) throws PredicateNotFoundException {

      // Abort if spanned text is empty
      if (annotation.tokens == null || annotation.tokens.equals("")) {
         throw new PredicateNotFoundException(type + " is empty (" + annotation.phraseID + ")");
      }

      // Abort if spanned text does not match annotation string
      String textSpan = text.substring(annotation.leftOffset, annotation.rightOffset);
      //System.out.println("Ann: '" + annotation.tokens + "' / span '" + text.substring(annotation.leftOffset, annotation.rightOffset) + "'");
      if (!annotation.tokens.equalsIgnoreCase(textSpan) ) {
         throw new PredicateNotFoundException(type + " does not match annotation span: found '" + textSpan
               + "', expected '" + annotation.tokens + "' (" + annotation.phraseID + ")");
      }

      // Try mapping to tokens
      ComparisonAnnotationToken[] tokens = mapToTokens(annotation.leftOffset, annotation.rightOffset, sentenceNo1);
      // Throw error if this fails
      return tokens;

   }




   // ===== PRINTOUT =====



   public void writeAnnotations() throws IOException {

      // --- Write sentences and annotations to file ---
      // All annotations from document have been collected.
      // Write them to files.
      for (int j=0; j< sentenceAnnotations.length; j++) {

         // Skip sentences without annotation
         if (sentenceAnnotations[j] == null) {
            continue;
         }

         SentenceAnnotation thisAnnotation = sentenceAnnotations[j];
         TextSpan[] tokenSpans = sentenceTokenizations[j];

         // Set the sentence as the tokenized sentence in the annotation
         String out = "";
         for (int i=0; i<tokenSpans.length; i++) {
            String token = tokenSpans[i].coveredText;
            token = token.replaceAll("=", "eq");
            out = out + " " + token;
         }
         out = out.trim();

         sentenceAnnotations[j].setSentence(out);
         sentenceAnnotations[j].setId(id + "-" + j);

         // Write sentence to sentence file
         outSentences.write(thisAnnotation.getSentence());
         outSentences.newLine();
         outSentences.flush();

         // Write complete annotation to annotation file
         outAnnotations.write(thisAnnotation.toString());
         outAnnotations.newLine();
         outAnnotations.flush();


      }
   }


   public void printError(String message, int sentenceNo) {
      String sentText = "";
      if (sentenceNo >= 0 && sentenceNo < sentenceSpans.length) {
         sentText = " in sentence " + this.sentenceSpans[sentenceNo].getCoveredText(text).replaceAll("\n", " ");
      }
      System.out.println("USAGE ANNOTATION WARNING !!! " + message
               + sentText
               + " in review " + id);
   }



}
