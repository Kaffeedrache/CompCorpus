// (c) Wiltrud Kessler
// 24.01.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.comparatives.liu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotation;
import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotationToken;
import de.uni_stuttgart.ims.corpus.compannotation.SentenceAnnotation;
import de.uni_stuttgart.ims.corpus.util.ArgumentNotFoundException;
import de.uni_stuttgart.ims.corpus.util.PredicateNotFoundException;
import de.uni_stuttgart.ims.nlpbase.nlp.PredicateType;
import de.uni_stuttgart.ims.nlpbase.nlp.SRLSentence;
import de.uni_stuttgart.ims.nlpbase.nlp.Word;
import de.uni_stuttgart.ims.nlpbase.tools.Tokenizer;
import de.uni_stuttgart.ims.util.StringWordMapping;


/**
 * Converts an annotaiton in the Jindal and Liu format to the IMS format.
 *
 * @author kesslewd
 *
 */
public class LiuAnnotation {

   static boolean useType4;


   public boolean isAnnotation;
   public List<Integer> types;
   public int linesToIgnore;
   public String sentence;
   public AnnotationContent[] annotationcontent;
   public boolean toWrite;
   private String originalLine = "";


   private static int numberOfComparisons = 0;

   /**
    * Global counter of predicates not found.
    */
   private static int predicatesNotFound = 0;

   /**
    * Global counter of arguments not found.
    */
   private static int argumentsNotFound = 0;



   /**
    * Tokenizer
    */
   private static Tokenizer tokenizer;


   /**
    * Per-document. Stores document name
    * (recorded as part of annotation).
    */
   private static String filename;

   /**
    * Tokenizer of sentences.
    */
   public static void setTokenizer(Tokenizer tokenizer) {
      LiuAnnotation.tokenizer = tokenizer;
   }

   /**
    * filename.
    */
   public static void setFilename(String filename) {
      LiuAnnotation.filename = filename;
   }
   /**
    * filename.
    */
   public static void setUseType4(boolean useType4) {
      LiuAnnotation.useType4 = useType4;
   }


   /**
    * Returns number of predicates that could not be found,
    * to know the reason, see log.
    * Equivalent to cought PredicateNotFoundExceptions.
    */
   public static int getPredicatesNotFound() {
      return predicatesNotFound;
   }

   /**
    * Returns number of arguments that could not be found,
    * to know the reason, see log.
    * Equivalent to cought ArgumentNotFoundExceptions.
    */
   public static int getArgumentsNotFound() {
      return argumentsNotFound;
   }


   public class AnnotationContent {
      public Integer type;
      public ArrayList<String> entity1 = new ArrayList<String>();
      public ArrayList<String> entity2 = new ArrayList<String>();
      public ArrayList<String> aspect = new ArrayList<String>();;
      public String predicate = "";

      public String toString() {
         return type + " " + predicate + " "  + entity1 + " " + entity2 + " " + aspect;
      }
   }


   public LiuAnnotation(boolean isAnnotation, List<Integer> types2, int linesToIgnore) {
      this.isAnnotation = isAnnotation;
      this.types = types2;
      this.linesToIgnore = linesToIgnore;
      this.annotationcontent = new AnnotationContent[types2.size()];
   }

   public LiuAnnotation(boolean isAnnotation, List<Integer> types2) {
      this.isAnnotation = isAnnotation;
      this.types = types2;
      this.annotationcontent = new AnnotationContent[types2.size()];
   }


   public LiuAnnotation(boolean isAnnotation) {
      this.isAnnotation = isAnnotation;
      this.types = new ArrayList<Integer>();
      this.linesToIgnore = 0;
      this.toWrite = false;
   }

   public int addEntities (String line, int i) {

      this.originalLine += line;

      this.annotationcontent[i] = new AnnotationContent();
      this.annotationcontent[i].type = this.types.get(i);

      // Search for predicate in () at the end of the line
      int startIndex = -1;
      int endIndex = -1;
      try {
         startIndex = line.lastIndexOf('(');
         endIndex = line.lastIndexOf(')');
         this.annotationcontent[i].predicate = line.substring(startIndex+1,endIndex);
      } catch (Exception e) {
         System.err.println("ERROR !!! in Liu annotation converter: " + e.getMessage());
         System.err.println("while adding predicate for annotation " + line);
         System.err.println("of sentence " + this.sentence);
         System.err.println("startIndex=" + startIndex + " endIndex=" + endIndex);
         e.printStackTrace();
         return 1;
      }


      // Search for entities/aspect
      line = line.substring(0, startIndex);
      int foundIndex = 0;
      int nextIndex = 0;

      try {
      while (foundIndex >= 0) {
         foundIndex = line.indexOf('_', foundIndex+1);
         if (foundIndex == -1) {
            break;
         } else {
            nextIndex = line.indexOf('_', foundIndex+1);
            if (nextIndex == -1) {
               nextIndex = line.length()+1; // so that it matches the -2
            }
         }
         switch (line.charAt(foundIndex-1)) {
         case '1':
            this.annotationcontent[i].entity1.add(line.substring(foundIndex+1,nextIndex-2));
            break;
         case '2':
            this.annotationcontent[i].entity2.add(line.substring(foundIndex+1,nextIndex-2));
            break;
         case '3':
            this.annotationcontent[i].aspect.add(line.substring(foundIndex+1,nextIndex-2));
            break;
         }
         foundIndex = nextIndex-1;
      }
      } catch (Exception e) {
         System.err.println("ERROR !!! in Liu annotation converter: " + e.getMessage());
         System.err.println("while adding entities of " + line);
         System.err.println("of sentence " + this.sentence);
         System.err.println("foundIndex=" + foundIndex + " nextIndex=" + nextIndex);
         e.printStackTrace();
         return 1;
      }
      return 0;

   }


   public static Integer findType (String annotationTag) {
      if (annotationTag.contains("1")) {
         return new Integer(1);
      }
      if (annotationTag.contains("2")) {
         return new Integer(2);
      }
      if (annotationTag.contains("3")) {
         return new Integer(3);
      }
      if (annotationTag.contains("4")) {
         return new Integer(4);
      }
      System.out.println("Error, unknown tag type: " + annotationTag);
      return new Integer(0);
   }


   public SentenceAnnotation convertToSentenceAnnotation() {
      numberOfComparisons += 1;
      return convertToSentenceAnnotation(numberOfComparisons);
   }



   public SentenceAnnotation convertToSentenceAnnotation(int comparisonID) {
      String thisSentence = this.sentence;
      thisSentence = thisSentence.replaceAll("\\n", " ");
      String[] tokens = tokenizer.tokenize(thisSentence);

      // Create complete annotation
      SentenceAnnotation thisSentenceAnnotation = new SentenceAnnotation();
      thisSentenceAnnotation.setId(comparisonID + "");
      thisSentenceAnnotation.setComparative(true);
      thisSentenceAnnotation.setSource(filename);

      // Add parts
      int j = -1; // need j to treat cases like [4,1] to skip the annotation for 4
      for (int i=0; i<this.types.size(); i++) {
         if (this.types.get(i) != 4) {
            j++;

            ComparisonAnnotation newAnnotation = new ComparisonAnnotation();
            newAnnotation.setFineType(mapLiuType(this.types.get(i)));

            String[] preds = this.annotationcontent[j].predicate.split(",");

            int done = 0;
            ComparisonAnnotationToken[] predicate = null;
            for (String pred : preds) {
               String[] parts = tokenizer.tokenize(pred);
               done++;
               try {
                  predicate = makePredicate(parts, tokens);
                  break;
               } catch (PredicateNotFoundException e) { // expected error
                  this.printError("Predicate not found (\"" + pred +  "\"):" + e.getMessage(), thisSentence);
                  predicatesNotFound++;
                  thisSentenceAnnotation.addAnnotationError(e.getMessage());
                  continue;
               } catch (Exception e) { // whatever strange thing might happen
                  System.err.println("Predicate not found (\"" + pred +  "\"): "  + e.getMessage());
                  e.printStackTrace();
                  this.printError("Predicate not found (\"" + pred +  "\"): " + e.getMessage(), thisSentence);
                  predicatesNotFound++;
                  thisSentenceAnnotation.addAnnotationError(e.getMessage());
               }
            }
            if (predicate == null) {
               continue;
            } else {
               newAnnotation.setPredicate(predicate);
            }
            for (String entity1 : this.annotationcontent[j].entity1) {
               try {
                  String[] parts = tokenizer.tokenize(entity1);
                  newAnnotation.addEntity1(makeArgument(parts, tokens));
               } catch (ArgumentNotFoundException e) { // expected error
                  this.printError("Argument 'entity1' not found (\"" + entity1 +  "\"): " + e.getMessage(), thisSentence);
                  argumentsNotFound++;
                  thisSentenceAnnotation.addAnnotationError(e.getMessage());
               } catch (Exception e) { // whatever strange thing might happen
                  System.err.println("Argument 'entity1' not found (\"" + entity1 +  "\"): "  + e.getMessage());
                  e.printStackTrace();
                  this.printError("Argument 'entity1' not found (\"" + entity1 +  "\"): " + e.getMessage(), thisSentence);
                  argumentsNotFound++;
                  thisSentenceAnnotation.addAnnotationError(e.getMessage());
               }
            }
            for (String entity2 : this.annotationcontent[j].entity2) {
               try {
                  String[] parts = tokenizer.tokenize(entity2);
                  newAnnotation.addEntity2(makeArgument(parts, tokens));
               } catch (ArgumentNotFoundException e) { // expected error
                  this.printError("Argument 'entity2' not found: " + e.getMessage(), thisSentence);
                  argumentsNotFound++;
                  thisSentenceAnnotation.addAnnotationError(e.getMessage());
               } catch (Exception e) { // whatever strange thing might happen
                  System.err.println("Argument 'entity1' not found (\"" + entity2 +  "\"): "  + e.getMessage());
                  e.printStackTrace();
                  this.printError("Argument 'entity1' not found (\"" + entity2 +  "\"): " + e.getMessage(), thisSentence);
                  argumentsNotFound++;
                  thisSentenceAnnotation.addAnnotationError(e.getMessage());
               }
            }
            for (String aspect : this.annotationcontent[j].aspect) {
               try {
                  String[] parts = tokenizer.tokenize(aspect);
                  newAnnotation.addAspect(makeArgument(parts, tokens));
               } catch (ArgumentNotFoundException e) { // expected error
                  this.printError("Argument 'aspect' not found: " + e.getMessage(), thisSentence);
                  argumentsNotFound++;
                  thisSentenceAnnotation.addAnnotationError(e.getMessage());
               } catch (Exception e) { // whatever strange thing might happen
                  System.err.println("Argument 'entity1' not found (\"" + aspect +  "\"): "  + e.getMessage());
                  e.printStackTrace();
                  this.printError("Argument 'entity1' not found (\"" + aspect +  "\"): " + e.getMessage(), thisSentence);
                  argumentsNotFound++;
                  thisSentenceAnnotation.addAnnotationError(e.getMessage());
               }
            }
            thisSentenceAnnotation.addComparisonAnnotation(newAnnotation);

            if (preds.length > 1) {
               thisSentenceAnnotation.addAnnotationError("Split predicate: " + this.annotationcontent[j].predicate);
            }

            // copy annotation for other predicates
            for (int m=done; m<preds.length; m++) {
               ComparisonAnnotation copiedAnnotation = new ComparisonAnnotation();
               String[] parts = tokenizer.tokenize(preds[m]);
               try {
                  copiedAnnotation.setPredicate(makePredicate(parts, tokens));
               } catch (PredicateNotFoundException e) {
                  this.printError("Predicate not found " + e.getMessage(), thisSentence);
                  predicatesNotFound++;
                  thisSentenceAnnotation.addAnnotationError(e.getMessage());
                  continue;
               }
               copiedAnnotation.setEntity1(newAnnotation.getEntity1());
               copiedAnnotation.setEntity2(newAnnotation.getEntity2());
               copiedAnnotation.setAspect(newAnnotation.getAspect());
               copiedAnnotation.setFineType(newAnnotation.getFineType());
               thisSentenceAnnotation.addComparisonAnnotation(copiedAnnotation);
            }


         } else { // type 4
            if (useType4) {
               ComparisonAnnotation newAnnotation = new ComparisonAnnotation();
               newAnnotation.setFineType(mapLiuType(this.types.get(i)));
               thisSentenceAnnotation.addComparisonAnnotation(newAnnotation);

            }
         }
      }

      if (!thisSentenceAnnotation.getComparisonAnnotationsIterator().hasNext()) {
         // no comparison annotations, is not comparative
         return null;
      }


      // Tokenize sentence and write to sentence file
      // Long sentences are cut off after 150 tokens.
      // -->> TODO Moved to general normalization
      String out = "";
      for (String token : tokens) {
         out += " " + token;
      }
      thisSentenceAnnotation.setSentence(out.trim());



      return thisSentenceAnnotation;
   }





   private PredicateType mapLiuType(Integer type) {
      switch (type) {
      case 1: return PredicateType.ranked;
      case 2: return PredicateType.equative;
      case 3: return PredicateType.superlative;
      case 4: return PredicateType.difference;
      }
      return PredicateType.undefined;

   }

   private ComparisonAnnotationToken[] makePredicate (String[] predicate, String[] tokens)
         throws PredicateNotFoundException {

      // Abort if spanned text is empty
      if (predicate == null || predicate.length == 0) {
         throw new PredicateNotFoundException("Error, predicate is empty.");
      }

      // Try mapping to tokens
      ComparisonAnnotationToken[] catokens = mapToTokens(predicate, tokens, true, false);

      // Throw error if this fails
      if (catokens == null)
         throw new PredicateNotFoundException("Error, predicate annotation could not be mapped to tokens: " + Arrays.asList(predicate));

      return catokens;
   }


   private ComparisonAnnotationToken[] makeArgument (String[] argument, String[] tokens)
         throws ArgumentNotFoundException {

      // Abort if spanned text is empty
      if (argument == null || argument.length == 0) {
         throw new ArgumentNotFoundException("Error, argument is empty.");
      }

      // Try mapping to tokens
      ComparisonAnnotationToken[] catokens = mapToTokens(argument, tokens, false, false);

      // Throw error if this fails
      if (catokens == null)
         throw new ArgumentNotFoundException("Error, argument annotation could not be mapped to tokens: " + Arrays.asList(argument));

      return catokens;
   }


   private ComparisonAnnotationToken[] mapToTokens (String[] thewords, String[] tokens, boolean ignorePredicates, boolean caseSensitive) {

      List<Word> words = null;
      SRLSentence tree = new SRLSentence();
      for (String token : tokens) {
         tree.addWord(new Word(token));
      }

      if (thewords.length > 1) { // MultiWordUnit
         words = StringWordMapping.identifyMWU(tree, thewords, ignorePredicates, caseSensitive);
      } else { // single word
         List<Word> found = StringWordMapping.identifyOneWordAll(tree, thewords[0].trim(), ignorePredicates, caseSensitive);
         if (found != null) {
            words = new ArrayList<Word>();
            words.add(found.get(0)); // add first place found, ignore rest
         } else {
            return null;
         }
      }

      // Not a complete token
      if (words == null) {
         return null;
      }

      ComparisonAnnotationToken[] catokens = new ComparisonAnnotationToken[words.size()];

      int j=0;
      for (Word word : words) {
         catokens[j] = new ComparisonAnnotationToken(word.getForm(), word.getId());
         j++;
      }

      return catokens;
   }



   public void printError(String message, String sentence) {
      System.out.println("LIU ANNOTATION WARNING !!! " + message
               + " in sentence " + sentence.replaceAll("\n", " ")
               + " with annotation " + originalLine
               + " in file " + filename);
   }



}