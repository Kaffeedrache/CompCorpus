// (c) Wiltrud Kessler
// 07.03.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license 
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.comparatives.jdpa;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotation;
import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotationToken;
import de.uni_stuttgart.ims.corpus.compannotation.SentenceAnnotation;
import de.uni_stuttgart.ims.corpus.util.ArgumentNotFoundException;
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
public class JDPAAnnotationConverter {
   
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
    * If an annotation is not found in the sentence,
    * whether to try follow the coreference chain to find an
    * coreferent annotation in the same sentence.
    */
   private static boolean useCoref = true;

   
   /**
    * For equative comparisons, set entities 1 and 2 in the 
    * order of appearance in the sentence.
    */
   private static boolean doEquativesReordering = true;
   
   /**
    * Do some complicated stuff for aspects.
    */
   private boolean doAspectFinding = false;

   
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
   private String filename;

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
   public void startNewDocument (String text, String filename, TextSpan[] sentenceSpans) {
      // Reset all per-document variables
      int last = filename.lastIndexOf("/");
      this.filename = filename.substring(last+1, filename.indexOf(".", last));
      this.text = text;
      this.sentenceSpans = sentenceSpans;

      // Sentence splitting
      //this.sentenceSpans = sentenceSplitter.split(text);
      
      // Prepare arrays to store annotations and tokenizations for each sentence
      sentenceAnnotations = new SentenceAnnotation[sentenceSpans.length];
      sentenceTokenizations = new TextSpan[sentenceSpans.length][];

   }
   

   // ===== ANNOTATION LEVEL =====


   public void addAnnotation (KnowtatorAnnotation current, 
            HashMap<Integer, List<KnowtatorAnnotation>> sentimentAnnotations) {
      
      // Find the sentence this annotation occurrs in.
      // We look at the covering span, not all spans, because there should
      // usually only be one span.
      KnowtatorAnnotation.Span annotationSpan = current.getCoveringSpan();
      int sentenceNo = this.findCorrespondingSentence(annotationSpan);
      TextSpan sentenceSpan = sentenceSpans[sentenceNo];
      String thisSentence = (String) sentenceSpan.getCoveredText(text);
      
      
      // If there is no annotation for this sentence yet ...
      if (sentenceAnnotations[sentenceNo] == null) {
         
         // Create a new annotation
         sentenceAnnotations[sentenceNo] = new SentenceAnnotation();
         sentenceAnnotations[sentenceNo].setComparative(true);
         sentenceAnnotations[sentenceNo].setSource(filename);

         // Tokenize sentence
         thisSentence = thisSentence.replaceAll("\\n", " ");
         sentenceTokenizations[sentenceNo] = tokenizer.getTokenizationSpans(thisSentence);

      }
      SentenceAnnotation thisSentenceAnnotation = sentenceAnnotations[sentenceNo];
      ///System.out.println("Found it here: " + sentenceSpans[sentenceNo]);
      //System.out.println(sentenceSpans[sentenceNo].getCoveredText(text));
      
      ComparisonAnnotation newAnnotation = new ComparisonAnnotation();
      
      newAnnotation.setFineType(getType(current));
      
      try {
         newAnnotation.setPredicate(makePredicate(current, sentenceNo));
      } catch (PredicateNotFoundException e) {
         this.printError("Predicate not found: " + e.getMessage(), sentenceNo);
         predicatesNotFound++;
         thisSentenceAnnotation.addAnnotationError(e.getMessage());
         return;
      }

      // Entity 1 = more
      ComparisonAnnotationToken[] argMore = null;
      ComparisonAnnotationToken[] argLess = null;
      KnowtatorAnnotation mention1 = null;
      try {
         mention1 = getMention(current, "More", sentenceNo);
         argMore = makeArgument(current, mention1, sentenceNo, "More");
      } catch (ArgumentNotFoundException e) {
         this.printError("Argument 'More' not found: " + e.getMessage(), sentenceNo);
         argumentsNotFound++;
         thisSentenceAnnotation.addAnnotationError(e.getMessage());
      }
      // Entity 2 = less
      KnowtatorAnnotation mention2 = null;
      try {
         mention2 = getMention(current, "Less", sentenceNo);
         argLess = makeArgument(current, mention2, sentenceNo, "Less");
      } catch (ArgumentNotFoundException e) {
         this.printError("Argument 'Less' not found: " + e.getMessage(), sentenceNo);
         argumentsNotFound++;
         thisSentenceAnnotation.addAnnotationError(e.getMessage());
      }
   
      
      // If we have an equative comparison and we want to re-order these, do that
      if (doEquativesReordering & (newAnnotation.getFineType() ==  PredicateType.equative)) {
         if (argMore != null & argLess != null) {
            int firstIndexMore = argMore[0].tokenNumber;
            int firstIndexLess = argLess[0].tokenNumber;
            if (firstIndexLess < firstIndexMore) { // 'more' is added as entity1
               // swap
               ComparisonAnnotationToken[] tmp = argMore;
               argMore = argLess;
               argLess = tmp;
            }
         }
      }

      // Actually add them ;)
      newAnnotation.addEntity1(argMore); // More = entity 1
      newAnnotation.addEntity2(argLess); // Less = entity 2

      
      // Dimension = Aspect or Sentiment
      // A sentiment mention might be annotated at the same token as the "dimension".
      // If there is no sentiment mention, the "dimension" is the aspect.
      // If there is a sentiment mention, get the target of the sentiment mention.
      // TODO ?? - if this is equal to the predicate, ignore it.
      // 
      try {
         KnowtatorAnnotation aspect = getMention(current, "Dimension", sentenceNo);
         if (aspect != null) { 
            if (doAspectFinding) { // do the complicated stuff described above
               // WARNING! this doesn't really work
               KnowtatorAnnotation sentiment = null;
               Integer beginspan = aspect.getCoveringSpan().begin;
               List<KnowtatorAnnotation> otherMentions = sentimentAnnotations.get(beginspan);
               if (otherMentions != null) {
                  for (KnowtatorAnnotation other : otherMentions) {
                     if (other.getCoveringSpan().equals(aspect.getCoveringSpan())) {
                        KnowtatorAnnotation target = other.annotationSlots.get("Target");
                        if (target != null) 
                           if ((mention1 == null || !target.getCoveringSpan().equals(mention1.getCoveringSpan()))
                              && (mention2 == null || target.getCoveringSpan().equals(mention2.getCoveringSpan()))) {
                           sentiment = other;
                           aspect = target;
                           System.out.println("Have sentiment: " + aspect + " [A] " + sentiment + " [S]");
                           thisSentenceAnnotation.addAnnotationError("Have sentiment: " + aspect + " [A] " + sentiment + " [S]");
                        }
                     }
                  }
               }
               newAnnotation.addAspect(makeArgument(current, aspect, sentenceNo, "aspect"));
               newAnnotation.addSentiment(makeArgument(current, sentiment, sentenceNo, "sentiment"));
            } else { // just use Dimension, don't care about the rest
               newAnnotation.addAspect(makeArgument(current, aspect, sentenceNo, "aspect"));
            }
         }
         
      } catch (ArgumentNotFoundException e) {
         this.printError("Argument 'Dimension' not found: " + e.getMessage(), sentenceNo);
         argumentsNotFound++;
         thisSentenceAnnotation.addAnnotationError(e.getMessage());
      }
      
      thisSentenceAnnotation.addComparisonAnnotation(newAnnotation);

   }



   private PredicateType getType(KnowtatorAnnotation current) {
      PredicateType type = PredicateType.ranked;
      for (String key: current.stringSlots.keySet()) {
         //System.out.println(key + ": " + current.stringSlots.get(key));
         if (key.equals("Same")) {
            String value = current.stringSlots.get(key);
            if (value.equals("true")) {
               type = PredicateType.equative;
            } else {
               type = PredicateType.ranked;
            }
         }
      }
      return type;
   }
   

   
   

   private ComparisonAnnotationToken[] makePredicate (KnowtatorAnnotation current, int sentenceNo) 
         throws PredicateNotFoundException {

      // Abort if spanned text is empty
      if (current.spannedText == null || current.spannedText.equals("")) {
         throw new PredicateNotFoundException("Predicate is empty: " + current.id);
      }

      // Try mapping to tokens
      ComparisonAnnotationToken[] tokens = mapToTokens(current.getCoveringSpan(), sentenceNo);   
      // Throw error if this fails
      if (tokens == null)
         throw new PredicateNotFoundException("Predicate annotation could not be mapped to tokens: " + current.spannedText + " (" + current.id + ")");
      
      return tokens;
   }
   
   
   
   private KnowtatorAnnotation getMention 
         (KnowtatorAnnotation current, String key, int sentenceNo) 
         throws ArgumentNotFoundException {
      
      if (current == null)
         return null;

      // Get referred Knowtator mention
      KnowtatorAnnotation mention = current.annotationSlots.get(key);
      
      // Abort if this argument is not annotated
      if (mention == null) {
         return null;
      }

      TextSpan sentenceSpan = sentenceSpans[sentenceNo];

      // Check if this mention is in the same sentence as the predicate.
      // If it is - no problem.
      // If it isn't - search for a coreferent mention that is in the
      // same sentence.
      // If one is found, set 'mention' to this one.
      // TODO search not only directly referred, but whole coref chain
      if (!sentenceSpan.contains(mention.getCoveringSpan().begin, mention.getCoveringSpan().end)) {
         //System.out.println(key + " : " + mention); 

         //System.out.println("ERROR -- Mention not in span: " + mention.getCoveringSpan() + " in " + sentence); 
         ArrayList<KnowtatorAnnotation> corefChain = new ArrayList<KnowtatorAnnotation>();

         if (useCoref) getAllCoreferent(mention,  corefChain);
         
         for (KnowtatorAnnotation thingy : corefChain) {
            //System.err.println("has " + mentionKey + ": " + referredMention);
            if (sentenceSpan.contains(thingy.getCoveringSpan().begin, thingy.getCoveringSpan().end)) {
               //System.out.println("here it is!" + thingy.spannedText);
               mention = thingy;
               break;
            }
         }
         
         if (mention == current.annotationSlots.get(key)) {
            throw new ArgumentNotFoundException("Argument " + key + " of predicate " + current.spannedText + " not in same sentence: " + mention.spannedText + " (" + mention.id + ")");
         }
              
      }
      
      return mention;
      
   }


   private ComparisonAnnotationToken[] makeArgument 
            (KnowtatorAnnotation predicate, KnowtatorAnnotation mention, int sentenceNo, String relation) 
            throws ArgumentNotFoundException {

      if (mention == null)
         return null;
      
      // Try mapping to tokens
      ComparisonAnnotationToken[] tokens = mapToTokens(mention.getCoveringSpan(), sentenceNo);   
      // Throw error if this fails
      if (tokens == null)
         throw new ArgumentNotFoundException("Argument " + relation + " of predicate " + predicate.spannedText + ", annotation could not be mapped to tokens: " + mention.spannedText + " (" + mention.id + ")");
      
      return tokens;
      
   }
   
   
   
   

   private static void getAllCoreferent(KnowtatorAnnotation mention, ArrayList<KnowtatorAnnotation> corefChain) {
      //System.err.println(key + ": " + mention);
      //System.err.println(sentences[sentenceNo].getCoveredText(text));
      for (String mentionKey : mention.annotationSlots.keySet()) {
         if (mentionKey.equals("RefersTo") || mentionKey.equals("inverse_of_RefersTo_")) {
            
            KnowtatorAnnotation referredMention = mention.annotationSlots.get(mentionKey);
            //System.out.println(mention.id + " refers to " + referredMention.id + "(" + referredMention.spannedText + ")");
            
            if (!corefChain.contains(referredMention)) {
               corefChain.add(referredMention);
               getAllCoreferent(referredMention, corefChain);
            }
            
         }
      }         
      
      
   }
   

   private ComparisonAnnotationToken[] mapToTokens (KnowtatorAnnotation.Span annotationSpan, int sentenceNo) {
      
      TextSpan[] tokenSpans = sentenceTokenizations[sentenceNo];
      TextSpan sentenceSpan = sentenceSpans[sentenceNo];
      
      //System.out.println("this span: " + annotationSpan);
      
      int first = -1;
      int last = -1;
      
      for (int i=0; i<tokenSpans.length; i++) {
         TextSpan thisTokenSpan = tokenSpans[i];
         //System.out.println("check this token + " + i + ": " + thisTokenSpan + " [" + thisTokenSpan.getCoveredText(sentenceText) + "] = " + (sentenceSpan.begin + thisTokenSpan.begin) + " " + (sentenceSpan.begin + thisTokenSpan.end));
         if ((sentenceSpan.begin + thisTokenSpan.begin) == annotationSpan.begin) {
            //System.out.println("found first");
            first = i;
         }
         if ((sentenceSpan.begin + thisTokenSpan.end) == annotationSpan.end) {
            //System.out.println("found last");
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
   



   private int findCorrespondingSentence(KnowtatorAnnotation.Span annotationSpan) {
      for (int i=0; i<sentenceSpans.length; i++) {
         TextSpan sentenceSpan = sentenceSpans[i];
         if (sentenceSpan.contains(annotationSpan.begin) & sentenceSpan.contains(annotationSpan.end)) {
            return i;
         }
      }
      return 0;
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
         sentenceAnnotations[j].setId(filename + "-" + j);
                  
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
      System.out.println("JDPA ANNOTATION WARNING !!! " + message
               + " in sentence " + this.sentenceSpans[sentenceNo].getCoveredText(text).replaceAll("\n", " ")
               + " in file " + filename);
   }
   
   

}
