// (c) Wiltrud Kessler
// 07.04.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/

package de.uni_stuttgart.ims.corpus.normalize;

import java.util.ArrayList;
import java.util.List;

import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotation;
import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotationToken;


/**
 * Some replacements of tokens that make the parser crash,
 * change token indices and contents in all annotations.
 *
 * @author kesslewd
 *
 */
public class TokenLevelReplacements {

   public static void globalReplaceTokens(String[] tokens, Integer[] newIndices,
         String[] replacements) {
      for (int i = 0; i < tokens.length; i++) {
         if (tokens[i].equals("="))
            replacements[i + 1] = "eq"; // beware of index token array from 0 /
                                        // tokens from 1
      }
   }

   public static String getSentenceString(String[] tokens, Integer[] newIndices,
         String[] replacements) {
      String newSentence = "";
      for (int i = 1; i < newIndices.length; i++) {
         if (newIndices[i] != null) {
            if (replacements[i] != null) {
               newSentence += " " + replacements[i];
            } else {
               newSentence += " " + tokens[i - 1];
            }
         }
      }
      newSentence = newSentence.trim();
      return newSentence;
   }


   /**
    * Change all token indices of all annotations.
    */
   public static void convertAnnotation(ComparisonAnnotation annotation,
         Integer[] newIndices, String[] replacements) {

      ComparisonAnnotationToken[] newpred = convertAnnotationTokens(
            annotation.getPredicate(), newIndices, replacements);
      annotation.setPredicate(newpred);

      List<ComparisonAnnotationToken[]> entity1list = annotation.getEntity1();
      annotation.removeEntity1();
      for (ComparisonAnnotationToken[] entity1 : entity1list) {
         ComparisonAnnotationToken[] newentity1 = convertAnnotationTokens(
               entity1, newIndices, replacements);
         annotation.addEntity1(newentity1);
      }

      List<ComparisonAnnotationToken[]> entity2list = annotation.getEntity2();
      annotation.removeEntity2();
      for (ComparisonAnnotationToken[] entity2 : entity2list) {
         ComparisonAnnotationToken[] newentity2 = convertAnnotationTokens(
               entity2, newIndices, replacements);
         annotation.addEntity2(newentity2);
      }

      List<ComparisonAnnotationToken[]> aspectlist = annotation.getAspect();
      annotation.removeAspect();
      for (ComparisonAnnotationToken[] aspect : aspectlist) {
         ComparisonAnnotationToken[] newaspect = convertAnnotationTokens(aspect,
               newIndices, replacements);
         annotation.addAspect(newaspect);
      }

      List<ComparisonAnnotationToken[]> sentimentlist = annotation
            .getSentiment();
      annotation.removeSentiment();
      for (ComparisonAnnotationToken[] sentiment : sentimentlist) {
         ComparisonAnnotationToken[] newsentiment = convertAnnotationTokens(
               sentiment, newIndices, replacements);
         annotation.addSentiment(newsentiment);
      }

   }


   /**
    * Change all words of all annotations.
    */
   public static ComparisonAnnotationToken[] convertAnnotationTokens(
         ComparisonAnnotationToken[] tokenlist, Integer[] newIndices,
         String[] replacements) {

      List<ComparisonAnnotationToken> list = new ArrayList<ComparisonAnnotationToken>();
      for (ComparisonAnnotationToken token : tokenlist) {
         Integer newIndex = newIndices[token.tokenNumber];
         if (newIndex != null) {
            String replacement = replacements[token.tokenNumber];
            if (replacement != null) {
               list.add(new ComparisonAnnotationToken(replacement, newIndex));
            } else {
               list.add(new ComparisonAnnotationToken(token.word, newIndex));
            }
         } else if (list.isEmpty()) {
            // if list is empty and index is null -> go left until we find
            // something
            // This goes very wrong if we find something totally different....
            for (int j = token.tokenNumber; j > 0; j--) {
               Integer newIndex2 = newIndices[j];
               if (newIndex2 != null) {
                  String replacement = replacements[j];
                  if (replacement != null) {
                     list.add(new ComparisonAnnotationToken(replacement,
                           newIndex2));
                  } else {
                     list.add(new ComparisonAnnotationToken(token.word,
                           newIndex2));
                  }
                  break;
               }
            }

         }
      }

      return list.toArray(new ComparisonAnnotationToken[list.size()]);
   }

}
