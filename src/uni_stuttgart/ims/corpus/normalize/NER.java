// (c) Wiltrud Kessler
// 07.04.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/

package de.uni_stuttgart.ims.corpus.normalize;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.WhitespaceTokenizer;


/**
 * Named Entity Recognizer to be used by NERNormalizer.
 *
 * Users Standford NER.
 *
 * @author kesslewd
 *
 */
public class NER {

   private String serializedClassifier = "../models/ner-model.ser.gz";

   private AbstractSequenceClassifier<CoreLabel> classifier;


   public NER () {
      classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
   }


   public List<List<String>> getNamedEntities (String sentence) {

      List<List<String>> foundEntities = new ArrayList<List<String>>();
      String[] parts = sentence.split(" ");
      List<String> currentEntity = new ArrayList<String>();
      String lastLabel = null;
      int i  = 0;
      for (String label : getNERLabels(sentence)) {
         if (label.equals(lastLabel) & label != null) {
            currentEntity.add(parts[i]);
         } else {
            foundEntities.add(currentEntity);
            currentEntity = new ArrayList<String>();
         }
         lastLabel = label;
         i++;
      }

      return foundEntities;
   }



   public String[] getNERLabels (String sentence) {
      StringReader reader=new StringReader(sentence);
      WhitespaceTokenizer<CoreLabel> ptbt = WhitespaceTokenizer.newCoreLabelWhitespaceTokenizer(reader);

      List<CoreLabel> asfd = ptbt.tokenize();
      String[] labels = new String[asfd.size()];

      List<CoreLabel> resultNER = classifier.classify(asfd);
      int i=0;
      for (CoreLabel cl : resultNER) {
         String label = cl.get(CoreAnnotations.AnswerAnnotation.class);
         if (!label.equals("O")) {
            labels[i] = label;
         }
         i+= 1;
      }

      return labels;
   }



}
