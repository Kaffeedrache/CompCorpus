// (c) Wiltrud Kessler
// 27.01.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.compannotation;

import java.util.Iterator;
import java.util.TreeSet;


/**
 * Sentence annotation.
 *
 * A sentence is either comparative or not and it can contain several
 * ComparisonAnnotations.
 *
 * @author kesslewd
 *
 */
public class SentenceAnnotation {


   private static String separator = "\t";


   // Sentence-level information
   private String sentenceid = "";
   private boolean isComparative = false;

   // Debug information
   private String sentence = "";
   private String sourceFilename = "";
   private String annotationErrors = " ";

   // Comparisons information
   private TreeSet<ComparisonAnnotation> comparisonAnnotations = new TreeSet<ComparisonAnnotation>();



   // Constructors

   /**
    * Create empty sentence annotation
    */
   public SentenceAnnotation () {
   }


   /**
    * Create a sentence annotation from a String.
    * Format: ID \t 0|1 \t [comparison annotations separated with \t] \t sentence \t source \t comments
    */
   public SentenceAnnotation (String line) {

         String[] parts = line.split(separator);

         // Sentence Information

         // First part is ID (String)
         sentenceid = parts[0];

         // Second part is comp/noncomp: 0 or 1
         String isComp = parts[1];
         if (isComp.equals("1")) {
            this.isComparative = true;
         } else if (isComp.equals("0")) {
            this.isComparative = false;
         } else {
            System.out.println("WARNING, no valid entry for 'isComparative': found " + isComp + ", expected 0/1");
         }


         // Debug Information

         // Third-last part is sentence (this is only debug)
         // TODO this may be missing in distributed files
         this.sentence = parts[parts.length-3];

         // Second-last part is source filename (this is only debug)
         this.sourceFilename = parts[parts.length-2];

         // Last part is annotation comment or errors (this is only debug)
         this.addAnnotationError(parts[parts.length-1]);



         // Comparisons
         for (int i=2; i<parts.length-3; i++) {
            this.comparisonAnnotations.add(new ComparisonAnnotation(parts[i]));
         }


   }



   // Getter/Setter

   public String getId() {
      return this.sentenceid;
   }
   public void setId(String id) {
      this.sentenceid = id;
   }

   public boolean isComparative() {
      return isComparative;
   }
   public void setComparative(boolean isComparative) {
      this.isComparative = isComparative;
   }

   public String getSentence() {
      return sentence;
   }
   public void setSentence(String sentence) {
      this.sentence = sentence;
   }

   public String getSource() {
      return this.sourceFilename;
   }
   public void setSource(String sourceFilename) {
      this.sourceFilename = sourceFilename;
   }

   public String getAnnotationErrors() {
      return this.annotationErrors.trim();
   }
   public void setAnnotationErrors(String annotationErrors) {
      this.addAnnotationError(annotationErrors);
   }
   public void addAnnotationError(String annotationError) {
      // Avoid that this is null/empty
      if (annotationError == null || annotationError.trim().isEmpty())
         return;

      // non-empty -> append
      if (this.annotationErrors == null || this.annotationErrors.trim().isEmpty()) {
         this.annotationErrors = annotationError ; // set
      } else {
         this.annotationErrors += " ; " + annotationError ; // append
      }
   }

   public void addComparisonAnnotation (ComparisonAnnotation newAnnotation) {
      this.comparisonAnnotations.add(newAnnotation);
   }
   public void removeComparisonAnnotation (ComparisonAnnotation newAnnotation) {
      this.comparisonAnnotations.remove(newAnnotation);
   }
   public Iterator<ComparisonAnnotation> getComparisonAnnotationsIterator() {
         return this.comparisonAnnotations.iterator();
   }
   public int getNumberOfComparisons() {
      return this.comparisonAnnotations.size();
   }



   // Output
   /**
    * Format: ID \t 0|1 \t [comparison annotations separated with \t] \t sentence \t source \t comments
    */
   public String toString() {
      String str = "";

      // First ID
      str = this.sentenceid + separator;

      // Add 0/1 for comparative
      if (this.isComparative) {
         str += 1 + separator;
      } else {
         str += 0 + separator;
      }

      // Then all comparison annotations
      for (ComparisonAnnotation compi : this.comparisonAnnotations) {
         str += compi.toString() + separator;
      }

      // Last the text of the sentence, the source and recorded errors
      str += this.sentence + separator + this.sourceFilename + separator + this.annotationErrors;

      // Return
      return str;
   }




}