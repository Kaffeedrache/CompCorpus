// (c) Wiltrud Kessler
// 24.01.2013
// This code is distributed under a Creative Commons
// Attribution-NonCommercial-ShareAlike 3.0 Unported license
// http://creativecommons.org/licenses/by-nc-sa/3.0/

package de.uni_stuttgart.ims.corpus.util;

@SuppressWarnings("serial")
public class ArgumentNotFoundException extends Exception {

   public ArgumentNotFoundException(String errorMsg) {
      super(errorMsg);
   }

}
