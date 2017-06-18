// (c) Wiltrud Kessler
// 23.07.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license 
// http://creativecommons.org/licenses/by-nc-sa/3.0/

package de.uni_stuttgart.ims.corpus.compannotation;

/** 
 * Representes a token with token number and word.
 * 
 * @author kesslewd
 */
public class ComparisonAnnotationToken {
   
   public String word;
   public int tokenNumber = 0;

   /** 
    * Representes a token with token number and word.
    * 
    * @param word
    * @param tokenNumber
    */
   public ComparisonAnnotationToken(String word, int tokenNumber) {
      this.word = word;
      this.tokenNumber = tokenNumber;
   }

   /** 
    * Representes a token with token number and word.
    * 
    * @param annotation should have the format tokennumber_word
    */
   public ComparisonAnnotationToken(String annotation) {
      String[] parts = annotation.split("_");
      if (parts.length > 1) {
         this.word = parts[1];
         try {
            this.tokenNumber = Integer.parseInt(parts[0]);
         } catch (NumberFormatException e) {
            System.err.println("ERROR! Expected token number, but found this: " + annotation);
         }
      } else {
         this.word = annotation; // fallback
      }
   }
   
   /** 
    * Returns tokenNumber_Word
    */
   public String toString() {
      return tokenNumber + "_" + word;
   }

   /** 
    * Checks both token number and word.
    */
   public boolean equals(ComparisonAnnotationToken other) {
      return (tokenNumber == other.tokenNumber) & word.equals(other.word);
   }

   
   
   // Array functions
   

   /**
    * Get a string of words separated with whitespaces from annotations array.
    * @param tokenarray
    * @return
    */
   public static String getString(ComparisonAnnotationToken[] tokenarray) {
      String str = "";
      for (ComparisonAnnotationToken pred : tokenarray) {
         str += pred.word + " ";
      }
      return str.trim();
   }

   /**
    * Get a string of words+id separated with whitespaces from annotations array.
    * @param tokenarray
    * @return
    */
   public static String getStringWithIDs(ComparisonAnnotationToken[] tokenarray) {
      String str = "";
      for (ComparisonAnnotationToken token : tokenarray) {
         str += token.toString() + " ";
      }
      return str.trim();
   }

   /**
    * Get an array from a String of words+id separated with whitespaces.
    * @param stringWithIDs
    * @return
    */
   public static ComparisonAnnotationToken[] getTokens(String stringWithIDs) {
      String[] split = stringWithIDs.split(" ");
      ComparisonAnnotationToken[] array = new ComparisonAnnotationToken[split.length];
      int i=0;
      for (String part : split) {
         array[i] = new ComparisonAnnotationToken(part);
         i++;
      }
      return array;
   }
   
}
