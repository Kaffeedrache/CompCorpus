// (c) Wiltrud Kessler
// 07.04.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/

package de.uni_stuttgart.ims.corpus.normalize;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotation;
import de.uni_stuttgart.ims.corpus.compannotation.ComparisonAnnotationToken;
import de.uni_stuttgart.ims.nlpbase.tools.POSTagger;
import de.uni_stuttgart.ims.nlpbase.tools.POSTaggerStanford;

/**
 * Normalization for annotations, especially regarding multiword-predicates.
 *
 * @author kesslewd
 *
 */
public class AnnotationNormalizer {

   private POSTagger taggy = null;

   private HashMap<String, Integer> exchangedCount = new HashMap<String, Integer>();
   private int reorder = 0;
   private int addSentiment = 0;
   private int splitPredicate = 0;


   public AnnotationNormalizer() {
      try {
         taggy = new POSTaggerStanford();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }



   public void splitPredicate(ComparisonAnnotation annotation,
         String[] tokens) {

      ComparisonAnnotationToken[] pred = annotation.getPredicate();

      if (pred.length > 1) { // multiword predicate

         // only happens with 'as'
         if (pred[0].word.equals("as")
               && pred[pred.length - 1].word.equals("as")) {
            annotation
                  .addSentiment(Arrays.copyOfRange(pred, 1, pred.length - 1));
            addSentiment++;
            annotation.setPredicate(pred[0]);
            splitPredicate++;
         }

      } else { // one-word predicate

         String word = pred[0].word;
         int tokenid = pred[0].tokenNumber;

         if (word.equalsIgnoreCase("than")) {
            // TODO? error correction
         }

         List<ComparisonAnnotationToken[]> aspectlist = annotation.getAspect();
         ComparisonAnnotationToken[] aspect = null;
         if (aspectlist != null && !aspectlist.isEmpty()) { // have an annotated
                                                            // aspect
            aspect = aspectlist.get(0); // TODO treat several aspects

            // Check if the aspect is the same word as the predicate.
            // If the aspect is longer than one token, remove the predicate from
            // the aspect
            // Example annotation:
            // pred: greater
            // aspect: greater degree of choice
            // New annotation:
            // pred: greater
            // aspect: degree of choice
            if (aspect[0].tokenNumber == tokenid) {
               if (aspect.length > 1) {
                  annotation.removeAspect();
                  addSentiment++;
                  aspect = Arrays.copyOfRange(aspect, 1, aspect.length);
                  annotation.addAspect(aspect);
               } else {
                  aspect = null; // don't need to proceed any further
               }
            }

         }


         // as X ... as -> X always works as sentiment!
         if (word.equalsIgnoreCase("as")) {
            if (aspect != null) { // have aspect -> should be X

               // Annotated aspect is word after predicate or two words after
               // (mostly works)
               // -> annotate
               if (aspect[0].tokenNumber == tokenid + 1
                     || aspect[0].tokenNumber == tokenid + 2) {
                  annotation.removeAspect();
                  annotation.addSentiment(aspect);
                  addSentiment++;
                  // System.out.println(" -> " + Arrays.toString(pred) + " sent
                  // " + Arrays.toString(annotation.getSentiment().get(0)));

                  // Annotated aspect is word before predicate
                  // this is the 'as' after the aspect
                  // search for 'as' before [start with -3, this is the token
                  // exactly before]
                  // -> annotate
               } else if (aspect[0].tokenNumber == tokenid - 1) {
                  int index = 0;
                  for (int j = 3; tokenid - j > 0; j++) {
                     if (tokens[tokenid - j].equalsIgnoreCase("as")) {
                        index = tokenid - j;
                        break;
                     }
                  }
                  // found second as? - index != 0
                  // this fails in one case "as complete a [solution]A [as]P"
                  if (index != 0) {
                     annotation.removeAspect();
                     annotation.addSentiment(aspect);
                     addSentiment++;
                     annotation.setPredicate(new ComparisonAnnotationToken(
                           tokens[index], index + 1));
                     // System.out.println("added " + (index+1) + " " +
                     // tokens[index] + " " + Arrays.toString(aspect) + " " +
                     // Arrays.toString(pred));
                     // System.out.println(" -> " +
                     // Arrays.toString(annotation.getPredicate()) + " sent " +
                     // Arrays.toString(annotation.getSentiment().get(0)));

                  } // else {
                    // we just leave the rest, no sentiment left in JDPA
                    // System.out.println(index + " " + tokens[index] + " " +
                    // Arrays.toString(aspect) + " " + Arrays.toString(pred));
                    // }
               }

            } else { // aspect is null -> too bad!
            }

         }


         // Regular multiword predicates for comparative/superlative
         if (word.equalsIgnoreCase("more") || word.equalsIgnoreCase("less")
               || word.equalsIgnoreCase("least")
               || word.equalsIgnoreCase("most")) {

            if (aspect != null && aspect[0].tokenNumber == tokenid + 1) {

               String[] result = taggy.getPOSTags(tokens);

               if (result[tokenid].equals("JJ")) {
                  annotation.removeAspect();
                  annotation.addSentiment(aspect);
                  addSentiment++;
               } else {
               }

            }
         }

      }

   }


   public void changePredicate(ComparisonAnnotation annotation) {
      List<ComparisonAnnotationToken[]> sentimentlist2 = annotation
            .getSentiment();
      if (!sentimentlist2.isEmpty()) {

         Integer count = exchangedCount.get(annotation.getPredicateString());
         if (count == null)
            exchangedCount.put(annotation.getPredicateString(), 1);
         else
            exchangedCount.put(annotation.getPredicateString(), count + 1);
         annotation.setPredicate(sentimentlist2.get(0));
      }
   }



   public void getStatistics() {
      int count = 0;
      for (String key : exchangedCount.keySet()) {
         count += exchangedCount.get(key);
         System.out.println(
               "Exchanged " + key + " " + exchangedCount.get(key) + " times.");
      }

      System.out.println("exchanged total: " + count + "\n");
      System.out.println("reordered: " + reorder + "\n");
      System.out.println("addSentiment: " + addSentiment + "\n");
      System.out.println("splitPredicate: " + splitPredicate + "\n");

   }

}
