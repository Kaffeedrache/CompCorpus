// (c) Wiltrud Kessler
// 07.04.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.normalize;


/**
 * Do Named Entity recognition and replace all found entities
 * by one placeholder token.
 *
 * @author kesslewd
 *
 */
public class NERNormalizer {

   private int haveDeleted = 0;
   private int haveNE = 0;

   private NERHeuristic nerClassifier;


   public NERNormalizer() {
      this.nerClassifier = new NERHeuristic();
   }


   public void doNormalize (String[] tokens, Integer[] newIndices, String[] replacements) {

      // Get new positions / replacements
      String lastLabel = "O";
      int del = 0;
      int i=1; // start from 1, leave 0 empty

      String[] labels = nerClassifier.getHeuristicLabels(tokens);

      // Go through all tokens / labels from the NER classification
      for (String thisLabel : labels) { // sentences

         if (!(thisLabel == null) && !thisLabel.equals("O")) {

            // We have a named entity
            haveNE ++;

            if (thisLabel.equals(lastLabel)) {
               // Two things with the same label -> merge,
               // i.e., this token will be deleted.
               newIndices[i] = null;
               del += 1;
               haveDeleted ++;
            } else {
               // Replace by NER label in sentence.
               // No merging, just normal offset.
               replacements[i] = thisLabel.toUpperCase();
               newIndices[i] = newIndices[i]-del;
            }
         } else {
            // This is no named entity, keep whatever we have ('newIndices[i]'),
            // consider already merged things ('del') before this index.
            newIndices[i] = newIndices[i]-del;
         }
         lastLabel = thisLabel;

         i++;
      }


   }


   public void writeDebug () {
      System.out.println("NERNormalizer deleted tokens: " + haveDeleted);
      System.out.println("NERNormalizer found NE: " + haveNE );
   }


}
