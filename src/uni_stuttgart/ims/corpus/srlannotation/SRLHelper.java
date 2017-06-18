// (c) Wiltrud Kessler
// 14.03.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.srlannotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotationToken;
import de.uni_stuttgart.ims.corpus.util.ArgumentNotFoundException;
import de.uni_stuttgart.ims.corpus.util.PredicateNotFoundException;
import de.uni_stuttgart.ims.nlpbase.nlp.SRLSentence;
import de.uni_stuttgart.ims.nlpbase.nlp.Word;
import de.uni_stuttgart.ims.util.HeadFinder;
import de.uni_stuttgart.ims.util.StringWordMapping;


/**
 * Annotation helper.
 * Map from Strings (single tokens or multiword tokens to words).
 * For multiword predicates and arguments, find the head.
 */
public class SRLHelper {



   // ====== Predicates =====


   /**
    * Map given String of predicate to word.
    * Does not change the tree.
    *
    * @param sentence Sentence that is supposed to contain the String.
    * @param predicate String to be found.
    * @return Word where the form is equivalent to the String to be found,
    *    in case of multiword predicate, one word is selected as the head.
    * @throws PredicateNotFoundException if the String is empty
    *    or cannot be mapped to a word in the sentence.
    */
   public static Word identifyPredicate (SRLSentence sentence, String predicate)
         throws PredicateNotFoundException {

      // Return with error if String is empty.
      if (predicate == null || predicate.trim().equals("")) {
         throw new PredicateNotFoundException("ANNOTATION ERROR!!! Predicate is empty!!");
      }

      Word pred = null;

      // Treat simple preds different from MW pred
      String[] predwords = predicate.trim().split(" ");
      if (predwords.length > 1) {
         List<Word> preds = StringWordMapping.identifyMWU(sentence, predwords, true, true);
         // TODO HERE markWords(preds, ComparativeUtils.getPredicateMarker(pred)); // SRLFtrs, only for display
         pred = HeadFinder.getPredicateHead(sentence, preds);
      } else {
         pred = StringWordMapping.identifyOneWordAll(sentence, predicate.trim(), true, true).get(0);  // TODO what if null?
         // TODO HERE markWords(pred, ComparativeUtils.getPredicateMarker(pred)); // SRLFtrs, only for display
      }

      // Return with error if no corresponding word was found
      if (pred == null) {
         throw new PredicateNotFoundException("ANNOTATION ERROR!!! Could not find predicate \"" + predicate + "\"!!");
      }

      // Return with error if the found word is already a predicate
      if (pred.isPredicate()) {
         throw new PredicateNotFoundException("ERROR found the same predicate \"" + predicate + "\" twice -- this should not happen");
      }

      return pred;
   }


   /**
    * Map given String/Tokenindex of predicate to word.
    * Does not change the tree.
    *
    * @param sentence Sentence that is supposed to contain the String.
    * @param predicate String/Tokenindex to be found.
    * @return Word at the token index indicated,
    *    in case of multiword predicate, one word is selected as the head.
    * @throws PredicateNotFoundException if the String is empty
    *    or cannot be mapped to a word in the sentence,
    *    or at the index the word found is different from the expected token.
    */
   public static Word identifyPredicate (SRLSentence sentence, ComparisonAnnotationToken[] predicate)
         throws PredicateNotFoundException {

      // Return with error if Predicate is empty.
      if (predicate == null || predicate.length == 0) {
         throw new PredicateNotFoundException("ANNOTATION ERROR!!! Predicate is empty!!");
      }

      List<Word> preds = new ArrayList<Word>();
      for (ComparisonAnnotationToken predtoken : predicate) {
         Word predWord = sentence.getWord(predtoken.tokenNumber);
         if (predWord == null) {
            throw new PredicateNotFoundException("ANNOTATION ERROR!!! Annotation token is not in this sentence " + predtoken);
         }
         if (predWord.getForm().equals(predtoken.word)) {
            preds.add(predWord);
         } else {
            throw new PredicateNotFoundException("ANNOTATION ERROR!!! Annotation token " + predtoken + " does not correspond to sentence token at this place " + predWord);
         }
      }

      Word pred = HeadFinder.getPredicateHead(sentence, preds);

      // Return with error if no corresponding word was found
      if (pred == null) {
         throw new PredicateNotFoundException("ANNOTATION ERROR!!! Could not find predicate \"" + Arrays.asList(predicate) + "\"!!");
      }

      // Return with error if the found word is already a predicate
      if (pred.isPredicate()) {
         throw new PredicateNotFoundException("ANNOTATION ERROR!!! Found the same predicate \"" + Arrays.asList(predicate) + "\" twice -- this should not happen");
      }

      return pred;
   }



   // ====== Arguments =====


   /**
    *
    * Map given String of argument to word.
    * Does not change the tree.
    *
    * @param sentence Sentence that is supposed to contain the String.
    * @param argument String to be found.
    * @return Word where the form is equivalent to the String to be found,
    *    in case of multiword argument, the lowest common ancestor
    *    is selected as the head.
    * @throws ArgumentNotFoundException If the String is empty
    *    or cannot be mapped to a word in the sentence.
    */
   public static Word identifyArgument
         (SRLSentence sentence, String argument)
         throws ArgumentNotFoundException {

      // Return with error if String is empty.
      if (argument == null || argument.trim().equals("")) {
         throw new ArgumentNotFoundException("ANNOTATION ERROR!!! Argument is empty!!");
      }

      Word argumentWord = null;

      // Treat simple arguments different from MW arguments
      String[] splitArgument = argument.trim().split(" ");
      if (splitArgument.length>1) {
         List<Word> argumentWords = StringWordMapping.identifyMWU(sentence, splitArgument, false, true);
         argumentWord = HeadFinder.getArgumentHead(sentence, argumentWords);
      } else {
         argumentWord = StringWordMapping.identifyOneWordAll(sentence, argument.trim(), false, true).get(0); // TODO what if null?
      }

      // Return with error if no corresponding word was found
      if (argumentWord == null) {
         throw new ArgumentNotFoundException("ANNOTATION ERROR!!! argument \"" + argument + "\" not found !!");
      }

      return argumentWord;
   }



   /**
    *
    * Map given String of argument to word.
    * Does not change the tree.
    *
    * @param sentence Sentence that is supposed to contain the String.
    * @param argument String to be found.
    * @return Word where the form is equivalent to the String to be found,
    *    in case of multiword argument, the lowest common ancestor
    *    is selected as the head.
    * @throws ArgumentNotFoundException If the String is empty
    *    or cannot be mapped to a word in the sentence.
    */
   public static Word identifyArgument
         (SRLSentence sentence,  ComparisonAnnotationToken[] argument)
         throws ArgumentNotFoundException {

      // Return with error if Argument is empty.
      if (argument == null || argument.length == 0) {
         throw new ArgumentNotFoundException("ANNOTATION ERROR!!! Argument is empty!!");
      }

      List<Word> argumentWords = new ArrayList<Word>();
      for (ComparisonAnnotationToken argtoken : argument) {
         Word argWord = sentence.getWord(argtoken.tokenNumber);
         if (argWord == null) {
            throw new ArgumentNotFoundException("ANNOTATION ERROR!!! Annotation token is not in this sentence " + argtoken);
         }
         if (argWord.getForm().equals(argtoken.word)) {
            argumentWords.add(argWord);
         } else {
            throw new ArgumentNotFoundException("ANNOTATION ERROR!!! Annotation token " + argtoken + " does not correspond to sentence token at this place " + argWord);
         }
      }

      Word argumentWord = HeadFinder.getArgumentHead(sentence, argumentWords);

      // Return with error if no corresponding word was found
      if (argumentWord == null) {
         throw new ArgumentNotFoundException("ANNOTATION ERROR!!! argument \"" + Arrays.asList(argument) + "\" not found !!");
      }

      return argumentWord;
   }



}
