// (c) Wiltrud Kessler
// 07.03.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license 
// http://creativecommons.org/licenses/by-nc-sa/3.0/


package de.uni_stuttgart.ims.corpus.comparatives.jdpa;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;

import de.uni_stuttgart.ims.nlpbase.tools.TextSpan;


/**
 * Create some debug output with markup for entities.
 */
public class HTMLDebugOutputJDPA implements Closeable {

   private BufferedWriter outHTML = null;
   private String[] htmlAnnotationArray;
   private int textlength;
   private String text;

   /**
    * Open file.
    */
   public HTMLDebugOutputJDPA(String htmlOutputFilename) {
      FileWriter fstream3;
      try {
         fstream3 = new FileWriter(htmlOutputFilename);
         outHTML = new BufferedWriter(fstream3);
      } catch (IOException e) {
         System.err.println("Error in creating HTML Debug output:" + e.getMessage());
         System.err.println("Will fail silently from now on.");
      }
   }

   /**
    * Write HTML header (call once before any other write operation).
    */
   public void initialize() { 
      this.write("<head>");
      this.write("<style type=\"text/css\">");
      this.write(".comp { color:#FF0000 }");
      this.write(".mention {color:#CCCC00 }");
      this.write(".sent { color:#0000FF }");
      this.write(".error { color:#00AAFF }");
      this.write(".ann { color:#AAAAAA }");
      this.write("font:active:after { content:\" #\"attr(hovertext); color:#888; font-size:70%; font-style:italic; }");
      this.write("</style>");
      this.write("</head><body>");
      this.newLine();
   }


   /**
    * Reset things for every new document.
    */
   public void startNewDocument(String text, TextSpan[] sentenceSpans) {
      this.textlength = text.length();
      this.htmlAnnotationArray = new String[textlength+1];
      this.text = text;      

      for (TextSpan sentenceSpan : sentenceSpans) {
         if (htmlAnnotationArray[sentenceSpan.end] == null) {
            htmlAnnotationArray[sentenceSpan.end] = "<br/>";
         }
      }
      
   }
   
   
   /**
    * Add HTML annotations for this entity to document.
    */
   public void addHTMLDebugOutput(KnowtatorAnnotation current) {
      
      // Get type of annotation
      String annoClass = "ann";
      if (current.type.equals("SentimentBearingExpression")) {
         annoClass = "sent";
      } else if (current.type.startsWith("Mention")) {
         annoClass = "mention";
      } else if (current.type.equals("Comparison")) {
         annoClass = "comp";
      }

      // Error handling: annotation span outside of document
      KnowtatorAnnotation.Span annoSpan = current.spans.get(0);
      int startAnnotation = annoSpan.begin;
      if (startAnnotation > textlength) {
         //System.err.println("ERROR !!! SOMETHING IS VERY WRONG!");
         //System.err.println("Looking for span [" + annoSpan.begin + "," + annoSpan.end + "], " +
         //      "but text length is only " + textlength);
         //System.err.println(current);
         startAnnotation = textlength;
         if (htmlAnnotationArray[startAnnotation] == null) {
            htmlAnnotationArray[startAnnotation] = "<font class=error>length error begin!</font>";
         } else {
            htmlAnnotationArray[startAnnotation] += "<font class=error>length error begin!</font>";
         }
      }
      int endAnnotation = annoSpan.end;
      if (endAnnotation > textlength) {
         //System.err.println("ERROR !!! SOMETHING IS VERY WRONG!");
         //System.err.println("Looking for span [" + annoSpan.begin + "," + annoSpan.end + "], " +
         //      "but text length is only " + textlength);
         //System.err.println(current);
         endAnnotation = textlength;
         if (htmlAnnotationArray[endAnnotation] == null) {
            htmlAnnotationArray[endAnnotation] = "<font class=error>length error end!</font>";
         } else {
            htmlAnnotationArray[endAnnotation] += "<font class=error>length error end!</font>";
         }
      }

      // Actual adding
      String startText = "<font class=" + annoClass 
            + " hovertext=\"" + current + "\"" 
            + ">[";
      if (htmlAnnotationArray[startAnnotation] == null) {
         htmlAnnotationArray[startAnnotation] = startText;
      } else {
         htmlAnnotationArray[startAnnotation] += startText;
      }
      if (htmlAnnotationArray[endAnnotation] == null) {
         htmlAnnotationArray[endAnnotation] = "]</font>";
      } else {
         htmlAnnotationArray[endAnnotation] += "]</font>";
      }
      
   }


   /**
    * Write whole annotated document to file.
    */
   public void writeHTMLToFile(String filename) {
      this.write(filename);
      this.newLine();
      String str = "<p>";
      for (int k=0; k<textlength; k++) {
         if (htmlAnnotationArray[k] != null) {
            str += htmlAnnotationArray[k];
         }
         str += text.charAt(k);
      }
      if (htmlAnnotationArray[text.length()] != null) {
         str += htmlAnnotationArray[text.length()];
      }
      this.write(str + "</p>");
      this.newLine();
      this.flush();
      
   }
   

   /**
    * Write one line, fail silently.
    */
   private void write (String text) {
      try {
         outHTML.write(text);
      } catch (IOException e) {
      }
   }

   /**
    * Write new line, fail silently.
    */
   private void newLine() {
      try {
         outHTML.newLine();
      } catch (IOException e) {
      }
   }

   /**
    * Flush lines to document, fail silently.
    */
   private void flush() {
      try {
         outHTML.flush();
      } catch (IOException e) {
      }
   }

   /**
    * Close file, fail silently.
    */
   public void close() {
      try {
         outHTML.close();
      } catch (IOException e) {
      }      
   }


}