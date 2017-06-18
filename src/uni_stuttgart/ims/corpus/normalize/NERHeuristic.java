// (c) Wiltrud Kessler
// 07.04.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.normalize;

/**
 * Named Entity Recognizer to be used by NERNormalizer.
 *
 * Heuristics for Named Entity detection in the domain of cameras.
 *
 * @author kesslewd
 *
 */
public class NERHeuristic {


   static final String labelBrand = "PRODUCT";
   static final String labelProduct = "PRODUCT";
   static final String labelModel = "PRODUCT";



   public String[] getHeuristicLabels (String sentence) {
      String[] words = sentence.split(" ");
      return getHeuristicLabels(words);
   }



  public String[] getHeuristicLabels (String[] words) {
      String[] labels = new String[words.length];

      boolean lastDet = false;
      boolean lastCapitalized = false;

      for (int i=0; i<words.length; i++) {
         String word = words[i];

         boolean thisIsDet = false;
         boolean thisCapitalized = false;


         if (isDet(word))
            thisIsDet = true;
         if (isCapitalized(word))
            thisCapitalized = true;


         if (lastDet & (isAllUpper(word) || isNumber(word) || thisCapitalized)) {
            // do this before next!! otherwise The X -> PRODUCT
            labels[i] = labelProduct;  // TODO my 5 year old
         } else

         if (lastCapitalized & (isNumber(word) || thisCapitalized)) {
            labels[i-1] = labelProduct;
            labels[i] = labelProduct;
         } else

         if (isBrand(word)) {
            labels[i] = labelBrand;
         } else

         if (isModelName(word)) {
            labels[i] = labelModel;
         }

         lastDet = thisIsDet;
         lastCapitalized = thisCapitalized;

      }

      return labels;

   }


   private boolean isCapitalized(String form) {

      if (form.equalsIgnoreCase("I"))
         return false;

      char firstLetter = form.charAt(0);
      return Character.isUpperCase(firstLetter);
   }


   private boolean isModelName(String form) {

      boolean hasDigit = false;
      boolean hasLetter = false;

      for (Character letter : form.toCharArray()) {
         if (Character.isDigit(letter)) {
            hasDigit = true;
         }

         if (Character.isLetter(letter)) {
            hasLetter = true;
         }
      }

      return hasDigit & hasLetter;
   }


   private boolean isDet(String form) {
      if (form.equalsIgnoreCase("the"))
         return true;
      if (form.equalsIgnoreCase("a"))
         return true;
      if (form.equalsIgnoreCase("an"))
         return true;
      if (form.equalsIgnoreCase("my"))
         return true;
      if (form.equalsIgnoreCase("this"))
         return true;

      return false;

   }


   private boolean isAllUpper(String form) {

      if (form.equalsIgnoreCase("I"))
         return false;

      for (Character letter : form.toCharArray()) {
         if (!Character.isLetter(letter))
            return false;
         if (Character.isLowerCase(letter))
            return false;
      }
      return true;

   }


   private boolean isNumber(String form) {

      for (Character letter : form.toCharArray()) {
         if (!Character.isDigit(letter))
            return false;
      }
      return true;

   }


   private boolean isBrand(String form) {

      if (form.equalsIgnoreCase("Sony"))
         return true;
      if (form.equalsIgnoreCase("Samsung"))
         return true;
      if (form.equalsIgnoreCase("Bravia"))
         return true;
      if (form.equalsIgnoreCase("Panasonic"))
         return true;
      if (form.equalsIgnoreCase("Loewe"))
         return true;
      if (form.equalsIgnoreCase("Freeview"))
         return true;
      if (form.equalsIgnoreCase("Philips"))
         return true;
      if (form.equalsIgnoreCase("Toshiba"))
         return true;

      return false;

   }


}
