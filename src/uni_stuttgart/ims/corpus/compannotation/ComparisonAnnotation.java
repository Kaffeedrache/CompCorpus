// (c) Wiltrud Kessler
// 27.01.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.compannotation;

import java.util.ArrayList;
import java.util.List;

import de.uni_stuttgart.ims.nlpbase.nlp.ArgumentType;
import de.uni_stuttgart.ims.nlpbase.nlp.PredicateType;
import de.uni_stuttgart.ims.nlpbase.nlp.PredicateDirection;



/**
 * Annotation of a comparsion in a sentence.
 * Predicate - The expression used to compare two things, e.g. 'bigger' or 'more'.
 * Entity1 - List of entities that are being compared and evaluated as inferior.
 * Entity2 - List of entities that are being compared and evaluated as superior.
 * Aspect - List of dimensions that are being compared, e.g. 'zoom' of a camera.
 *
 * @author kesslewd
 */
public class ComparisonAnnotation implements Comparable<ComparisonAnnotation> {


   private static String comparisonInnerSeparator = "; ";
   private static String comparisonEntitySeparator = " , ";
   private static String comparisonStart = "[";
   private static String comparisonEnd = "]";
   private static String regex = "\\[|\\]";


   private PredicateType finetype = PredicateType.undefined;
   private PredicateDirection direction = PredicateDirection.UNDEFINED;
   private Character entity1Type = '-';
   private char entity2Type = '-';
   private List<ComparisonAnnotationToken[]> entity1;
   private List<ComparisonAnnotationToken[]> entity2;
   private List<ComparisonAnnotationToken[]> aspect;
   private List<ComparisonAnnotationToken[]> sentiment;
   private ComparisonAnnotationToken[] predicate;




   /**
    * Create empty comparison annotation
    */
   public ComparisonAnnotation () {
      this.predicate = new ComparisonAnnotationToken[0];
      this.entity1 = new ArrayList<ComparisonAnnotationToken[]>();
      this.entity2 = new ArrayList<ComparisonAnnotationToken[]>();
      this.aspect = new ArrayList<ComparisonAnnotationToken[]>();
      this.sentiment = new ArrayList<ComparisonAnnotationToken[]>();
   }



   /**
    * Create a comparison annotation from a String.
    * Format: [type; [entity1]; [entity2]; [aspect]; [sentiment]; predicate]
    * Parts in [] may occur several times, separated by ' , '
    * Everything may consist of several tokens.
    * Type has the format TYPE_ab_d where a=entity1type, b=entity2type, d=direction
    */
   public ComparisonAnnotation (String line) {

      String[] parts = line.split(comparisonInnerSeparator);
      int length = parts.length;

      if (length < 5) {
         System.out.println("ERROR! Malformed annotation, not enough parts: " + line);
         return;
      }

      // Predicate
      this.predicate = getTokens(cleanupAnnotation(parts[length-1]));

      // Entity 1
      this.entity1 = new ArrayList<ComparisonAnnotationToken[]>();
      if (!cleanupAnnotation(parts[1]).equals("")) {
         for (String part : parts[1].split(comparisonEntitySeparator)) {
            this.entity1.add(getTokens(cleanupAnnotation(part)));
         }
      }

      // Entity 2
      this.entity2 = new ArrayList<ComparisonAnnotationToken[]>();
      if (!cleanupAnnotation(parts[2]).equals("")) {
         for (String part : parts[2].split(comparisonEntitySeparator)) {
            this.entity2.add(getTokens(cleanupAnnotation(part)));
         }
      }

      // Aspect
      this.aspect = new ArrayList<ComparisonAnnotationToken[]>();
      if (!cleanupAnnotation(parts[3]).equals("")) {
         for (String part : parts[3].split(comparisonEntitySeparator)) {
            this.aspect.add(getTokens(cleanupAnnotation(part)));
         }
      }

      // Sentiment
      this.sentiment = new ArrayList<ComparisonAnnotationToken[]>();
      if (!cleanupAnnotation(parts[4]).equals("")) {
         for (String part : parts[4].split(comparisonEntitySeparator)) {
            this.sentiment.add(getTokens(cleanupAnnotation(part)));
         }
      }


      // Types (comparison, entities, direction)
      String typeetc = cleanupAnnotation(parts[0]);
      String[] typeparts = typeetc.split("_");

      // Predicate type
      this.finetype = PredicateType.valueOf(typeparts[0].toLowerCase());


      // Entity types
      // Check how many we expect
      int expectedtypes = 0;
      if (!this.entity1.isEmpty())
         expectedtypes += 1;
      if (!this.entity2.isEmpty())
         expectedtypes += 1;
      // Check what we have
      int havetypes = 0;
      if (typeparts.length > 1) {
         String types = typeparts[1];
         if (types.length() == 0)
            havetypes = expectedtypes;
         havetypes = types.replace("-","").length();
         if (types.length() == 2) { // both are given explicitly
            this.entity1Type = types.charAt(0);
            this.entity2Type = types.charAt(1);
         } else if (types.length() == 1) { // only one given, the other is empty
            if (!this.entity1.isEmpty()) {
               this.entity1Type = types.charAt(0);
            }
            if (!this.entity2.isEmpty()) {
               this.entity2Type = types.charAt(0);
            }
         } // else none given
      }
      // TODO only print warning if we have more than expected or have 1 and expected 2
      if (expectedtypes != havetypes && havetypes != 0)
         System.out.println("WARNING Malformed annotation, have " + havetypes + " entity types, expected " + expectedtypes + ": " + line);


      // Predicate direction
      this.direction = PredicateDirection.UNDEFINED;
      if (typeparts.length > 2) {
         char sign = typeparts[2].charAt(0);
         if (sign == '>' || sign == '+') {
            this.direction = PredicateDirection.SUPERIOR;
         } else if (sign == '<' || sign == '-') {
            this.direction = PredicateDirection.INFERIOR;
         }
      }


   }


   private ComparisonAnnotationToken[] getTokens(String annotation) {
      String[] parts = annotation.split(" ");
      ComparisonAnnotationToken[] tokens = new ComparisonAnnotationToken[parts.length];
      for (int i=0; i<parts.length; i++) {
         tokens[i] = new ComparisonAnnotationToken(parts[i]);
      }
      return tokens;
   }



   private static String cleanupAnnotation(String line) {
      return line.replaceAll(regex, "").trim();
   }



   public String toString (List<ComparisonAnnotationToken[]> thingy) {
      String str = "";
      for (ComparisonAnnotationToken[] tokenlist : thingy) {
         if (!str.isEmpty()) {
            str += comparisonEntitySeparator;
         }
         for (ComparisonAnnotationToken token : tokenlist) {
            str += token + " ";
         }
      }
      return comparisonStart + str.trim() + comparisonEnd;
   }


   public String toString() {
      String str = "";
      str = comparisonStart + this.finetype + "_" + this.entity1Type + this.entity2Type + "_" + this.direction + comparisonInnerSeparator;
      str += toString(this.entity1) + comparisonInnerSeparator;
      str += toString(this.entity2) + comparisonInnerSeparator;
      str += toString(this.aspect) + comparisonInnerSeparator;
      str += toString(this.sentiment) + comparisonInnerSeparator;
      for (ComparisonAnnotationToken token : this.predicate) {
         str += token + " ";
      }
      str = str.trim();
      str += comparisonEnd;
      return str;
   }


   // Normalization helpers


   public static ComparisonAnnotationToken[] changeTokenOffsets(ComparisonAnnotationToken[] tokenlist, int offset) {
      ComparisonAnnotationToken[] newTokenlist = new ComparisonAnnotationToken[tokenlist.length];
      for (int i=0; i<tokenlist.length; i++) {
         ComparisonAnnotationToken token = tokenlist[i];
         newTokenlist[i] = new ComparisonAnnotationToken(token.word, token.tokenNumber - offset);
      }
      return newTokenlist;
   }



   public static int[] getMinMaxIndex(ComparisonAnnotationToken[] tokenlist) {
      int[] array = new int[2];
      if (tokenlist == null || tokenlist.length == 0) {
         array[0] = -1;
         array[1] = -1;
         return array;
      }
      array[0] = tokenlist[0].tokenNumber;
      array[1] = tokenlist[tokenlist.length-1].tokenNumber;
      return array;
   }


   public static int[] getMinMaxIndex(List<ComparisonAnnotationToken[]> tokenlistlist) {
      int[] array = new int[2];
      array[0] = -1;
      array[1] = -1;
      if (tokenlistlist == null || tokenlistlist.size() == 0) {
         return array;
      }
      for (ComparisonAnnotationToken[] tokenlist : tokenlistlist) {
         int[] newArray = getMinMaxIndex(tokenlist);
         if (newArray[0] != 0 && newArray[0] < array[0]) {
            array[0] = newArray[0];
         }
         if (newArray[1] != 0 && newArray[1] > array[1]) {
            array[0] = newArray[1];
         }
      }
      return array;
   }


   // Comparison

   /** Compares two predicates with respect to token id.
    * TODO what to do in case  of empty??
    *
    * @return 0 if the ids are the same
    *    >1 if this one is better
    *    <1 if the other is better
    */
   @Override
   public int compareTo(ComparisonAnnotation o) {
      if (this.predicate == null || this.predicate.length == 0)
         if (o.predicate == null || o.predicate.length == 0)
            return 0; // they are the same (both empty)
         else
            return -1; // this is null, the other is not null
      else
         if (o.predicate == null || o.predicate.length == 0)
            return 1; // this is not null, the other is null
         else
            return Double.compare(this.predicate[0].tokenNumber, o.predicate[0].tokenNumber);
   }


   // Getter / Setter



   public PredicateType getFineType() {
      return this.finetype;
   }
   public void setFineType(PredicateType type) {
      this.finetype = type;
   }

   public char getEntity1Type() {
      return this.entity1Type;
   }

   public char getEntity2Type() {
      return this.entity2Type;
   }

   public PredicateDirection getDirection() {
      return this.direction;
   }


   public ComparisonAnnotationToken[] getPredicate() {
      return this.predicate;
   }
   public String getPredicateString() {
      String str = "";
      for (ComparisonAnnotationToken pred : this.predicate) {
         str += pred.word + " ";
      }
      return str.trim();
   }
   public void setPredicate (String predicate) {
      this.predicate = getTokens(predicate);
   }
   public void setPredicate(ComparisonAnnotationToken predicate) {
      this.predicate = new ComparisonAnnotationToken[] {predicate};
   }
   public void setPredicate (ComparisonAnnotationToken[] predicate) {
      this.predicate = predicate;
   }
   public void setPredicate (List<ComparisonAnnotationToken> predicate) {
      this.predicate = predicate.toArray(new ComparisonAnnotationToken[0]);
   }


   public List<ComparisonAnnotationToken[]> getArguments () {
      ArrayList<ComparisonAnnotationToken[]> result = new ArrayList<ComparisonAnnotationToken[]>();
      result.addAll(this.entity1);
      result.addAll(this.entity2);
      result.addAll(this.aspect);
      result.addAll(this.sentiment);
      return result;
   }

   public List<ComparisonAnnotationToken[]> getArgument (ArgumentType argumentName) {
      switch (argumentName) {
      case entity1 : return this.entity1;
      case entity2 : return this.entity2;
      case aspect : return this.aspect;
      case sentiment : return this.sentiment;
      default : return null;
      }
   }
   public void removeArgument (ArgumentType argumentName) {
      switch (argumentName) {
      case entity1 : this.entity1 = new ArrayList<ComparisonAnnotationToken[]>(); break;
      case entity2 : this.entity2 = new ArrayList<ComparisonAnnotationToken[]>(); break;
      case aspect : this.aspect = new ArrayList<ComparisonAnnotationToken[]>(); break;
      case sentiment : this.sentiment = new ArrayList<ComparisonAnnotationToken[]>(); break;
      default: // TODO
      }
   }
   public void addArgument (ArgumentType argumentName, String argumentString) {
      this.addArgument(argumentName, getTokens(argumentString));
   }
   public void addArgument (ArgumentType argumentName, List<ComparisonAnnotationToken> argumentTokenList) {
      this.addArgument(argumentName, argumentTokenList.toArray(new ComparisonAnnotationToken[0]));
   }
   public void addArgument (ArgumentType argumentName, ComparisonAnnotationToken[] argumentTokens) {
      if (argumentTokens != null) {
         switch (argumentName) {
         case entity1 : this.entity1.add(argumentTokens); break;
         case entity2 : this.entity2.add(argumentTokens); break;
         case aspect : this.aspect.add(argumentTokens); break;
         case sentiment : this.sentiment.add(argumentTokens); break;
         default: // TODO
         }
      }
   }
   public void setArgument (ArgumentType argumentName, List<ComparisonAnnotationToken[]> argumentTokenLists) {
      this.removeArgument(argumentName);
      for (ComparisonAnnotationToken[] argument : argumentTokenLists) {
         this.addArgument(argumentName, argument);
      }
   }


   public List<ComparisonAnnotationToken[]> getEntity1 () {
      return this.entity1;
   }
   public void addEntity1 (String entity1) {
      if (entity1 != null)
         this.entity1.add(getTokens(entity1));
   }
   public void addEntity1 (ComparisonAnnotationToken[] entity1) {
      if (entity1 != null)
         this.entity1.add(entity1);
   }
   public void setEntity1 (List<ComparisonAnnotationToken[]> entity1) {
      if (entity1 != null)
         this.entity1 = entity1;
   }
   public void removeEntity1 () {
      this.entity1 = new ArrayList<ComparisonAnnotationToken[]>();
   }

   public List<ComparisonAnnotationToken[]> getEntity2 () {
      return this.entity2;
   }
   public void addEntity2 (String entity2) {
      if (entity2 != null)
         this.entity2.add(getTokens(entity2));
   }
   public void addEntity2 (ComparisonAnnotationToken[] entity2) {
      if (entity2 != null)
         this.entity2.add(entity2);
   }
   public void setEntity2 (List<ComparisonAnnotationToken[]> entity2) {
      if (entity2 != null)
         this.entity2 = entity2;
   }
   public void removeEntity2 () {
      this.entity2 = new ArrayList<ComparisonAnnotationToken[]>();
   }

   public List<ComparisonAnnotationToken[]> getAspect () {
      return this.aspect;
   }
   public void addAspect (String aspect) {
      if (aspect != null)
         this.aspect.add(getTokens(aspect));
   }
   public void addAspect (ComparisonAnnotationToken[] aspect) {
      if (aspect != null)
         this.aspect.add(aspect);
   }
   public void setAspect (List<ComparisonAnnotationToken[]> aspect) {
      if (aspect != null)
         this.aspect = aspect;
   }
   public void removeAspect () {
      this.aspect = new ArrayList<ComparisonAnnotationToken[]>();
   }

   public List<ComparisonAnnotationToken[]> getSentiment() {
      return sentiment;
   }
   public void addSentiment (String sentiment) {
      if (sentiment != null)
         this.sentiment.add(getTokens(sentiment));
   }
   public void addSentiment (ComparisonAnnotationToken[] sentiment) {
      if (sentiment != null)
         this.sentiment.add(sentiment);
   }
   public void setSentiment (List<ComparisonAnnotationToken[]> sentiment) {
      if (sentiment != null)
         this.sentiment = sentiment;
   }
   public void removeSentiment () {
      this.sentiment = new ArrayList<ComparisonAnnotationToken[]>();
   }




}