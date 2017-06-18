// (c) Wiltrud Kessler
// 07.04.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.normalize;

import java.util.Iterator;

import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotation;
import de.uni_stuttgart.ims.corpus.compannotation.SentenceAnnotation;


/**
 * Sentence normalization:
 * Long sentences are cut off after 150 tokens, try to pick out the
 * part that contains the annotations.
 *
 * @author kesslewd
 *
 */
public class LengthNormalizer {

   public static int tokenlimit = 150;


   public static int[] getStartEnd (SentenceAnnotation thisAnnotation, int thisSentenceTokenLength) {
      // remember counting of tokens starts at 1!!

      // Long sentences are cut off after 150 tokens.
      if (thisSentenceTokenLength <= LengthNormalizer.tokenlimit) {
         return new int[] {1, thisSentenceTokenLength};
      }

      // we have more than 150 tokens

      // no annotation
      if (thisAnnotation == null) {
         return new int[] {1, 150};
      }


      // look at annotation
      int minIndex = thisSentenceTokenLength;
      int maxIndex = 1;

      Iterator<ComparisonAnnotation> itty = thisAnnotation.getComparisonAnnotationsIterator();

      // Get min/max token numbers for all preds and args in the sentence
      int[] startend;
      while (itty.hasNext()) {
         ComparisonAnnotation annotation = itty.next();

         // pred
         startend = ComparisonAnnotation.getMinMaxIndex(annotation.getPredicate());
         if (startend[0] > 0 && startend[0] < minIndex) {
            minIndex = startend[0];
         }
         if (startend[1] > 0 && startend[1] > maxIndex) {
            maxIndex = startend[1];
         }

         // entity1
         startend = ComparisonAnnotation.getMinMaxIndex(annotation.getEntity1());
         if (startend[0] > 0 && startend[0] < minIndex) {
            minIndex = startend[0];
         }
         if (startend[1] > 0 && startend[1] > maxIndex) {
            maxIndex = startend[1];
         }

         // entity2
         startend = ComparisonAnnotation.getMinMaxIndex(annotation.getEntity2());
         if (startend[0] > 0 && startend[0] < minIndex) {
            minIndex = startend[0];
         }
         if (startend[1] > 0 && startend[1] > maxIndex) {
            maxIndex = startend[1];
         }

         // aspect
         startend = ComparisonAnnotation.getMinMaxIndex(annotation.getAspect());
         if (startend[0] > 0 && startend[0] < minIndex) {
            minIndex = startend[0];
         }
         if (startend[1] > 0 && startend[1] > maxIndex) {
            maxIndex = startend[1];
         }

      }


      int startIndex = minIndex;
      int endIndex = maxIndex;
      int nowLength = (maxIndex - minIndex);
      if (nowLength > tokenlimit) {
         // We still have more than 150 tokens - take the first 150
         startIndex = minIndex;
         endIndex = Math.min(minIndex + tokenlimit - 1, thisSentenceTokenLength);
      } else {
         int addon = (tokenlimit - nowLength) / 2;
         if (minIndex - addon < 0) {
            startIndex = 1;
            endIndex = tokenlimit;
         } else if (maxIndex + addon > thisSentenceTokenLength) {
            endIndex = thisSentenceTokenLength;
            startIndex = endIndex - tokenlimit;
         } else {
            startIndex = Math.max(minIndex - addon, 1);
            endIndex =  Math.min(maxIndex + addon, thisSentenceTokenLength);
         }
      }


      return new int[] {startIndex, endIndex};

   }



   public static void adjustReplacementIndices (int startIndex, int endIndex,
         Integer[] newIndices, String[] replacements) {

      for (int i=0; i<startIndex; i++) {
         newIndices[i] = null;
         replacements[i] = null;
      }

      for (int i=startIndex; i<=endIndex; i++) {
         if (newIndices[i] != null) {
            newIndices[i] = newIndices[i] - startIndex+1;
         }
      }

      for (int i=endIndex+1; i<newIndices.length; i++) {
         newIndices[i] = null;
         replacements[i] = null;
      }

   }


}
