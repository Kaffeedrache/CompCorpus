// (c) Wiltrud Kessler
// 24.01.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/

package de.uni_stuttgart.ims.corpus.util;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;


/**
 * Manage the options to be read in from the configuration file.
 * @author kesslewd
 *
 */
public class Options {

   private static HashMap<String, String> argumentMap;
   private static String listDelimiter = ":";


   public static String getOption (String optionName) {
      return argumentMap.get(optionName);
   }

   public static int getIntOption (String optionName) {
      return Integer.parseInt(getOption(optionName));
   }

   public static boolean getBooleanOption (String optionName) {
      return Boolean.getBoolean(getOption(optionName));
   }

   public static String[] getListOption (String optionName) {
      String result = getOption(optionName);
      if (result == null) {
         System.err.println("ERROR, could not find option: " + optionName);
         return null;
      }
      return result.split(listDelimiter);
   }

   public static void parseOptionsFile (String filename) throws IOException {

      DataInputStream in = new DataInputStream(new FileInputStream(filename));
      BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));

      argumentMap = new HashMap<String, String>();

      String strLine;
      while ((strLine = br.readLine()) != null) {

         strLine = strLine.trim();

         // Ignore empty lines
         if (strLine.equals("")) {
            continue;
         }

         // Ignore comments
         if (strLine.startsWith("#")) {
            continue;
         }

         String[] parts = strLine.split("=");

         if (parts.length < 2) { // empty option
            argumentMap.put(parts[0].trim(), "");
         } else { // argument given
            argumentMap.put(parts[0].trim(), parts[1].trim());
            if (parts.length > 2) {
               System.err.println("Warning: option value for " + parts[0].trim() + " contains '=', value was cut off.");
            }

         }

      }

      br.close();

   }

   public static void printAll() {
      for (String key : argumentMap.keySet()) {
         System.out.println(key + ": " + argumentMap.get(key));
      }
   }



}
