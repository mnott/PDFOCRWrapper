package org.mnsoft.pdfocr;

/**
 * Some String Utilities.
 *
 * This program is a free software available under the GNU
 * Lesser General Public License.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * @author (c) 2010, Matthias Nott.
 */
public class StringUtility {
  /**
   * Returns an array of string related to the key value string.<p>
   *
   * If there is nothing left or right of the separator, that position
   * is returned as an empty string:<p>
   *
   * <ul>
   *   <li><code>a/b/c</code>   returns     "a", "b", "c".    </li>
   *   <li><code>/a/b/c</code>  returns "", "a", "b", "c".    </li>
   *   <li><code>a/b/c/</code>  returns     "a", "b", "c", "".</li>
   *   <li><code>/a/b/c/</code> returns "", "a", "b", "c", "".</li>
   * <ul>
   * <p>
   * If you do not want empty Strings, you can operate
   * {@link #condense(String[])} on the return value.
   *
   * @param strParamVal the String that has to be splitted
   * @param strSep the Separator at which the String shall be splitted
   * @return Array of Strings with the Substrings.
   */
  public static String[] split(String strParamVal, String strSep) {
    if ((strSep == null) || "".equals(strSep)) {
      return new String[] { strParamVal };
    }

    String[] strRetList = new String[0];

    if ((strParamVal == null) || (strParamVal.length() == 0)) {
      return strRetList;
    }

    final int separatorPosition = strParamVal.indexOf(strSep);
    final int separatorLength   = strSep.length();

    /*
     * If we no longer have the separator,
     * we do not need to split the String
     * again.
     */
    if (separatorPosition < 0) {
      strRetList    = new String[1];
      strRetList[0] = strParamVal;

      return strRetList;
    }

    String lowValue  = "";
    String highValue = "";

    lowValue = strParamVal.substring(0, separatorPosition);
    if (strParamVal.length() >= (separatorPosition + separatorLength)) {
      highValue = strParamVal.substring((strParamVal.equals(strSep)) ? 0 : (separatorPosition + separatorLength));
    }

    final String[] recursion       = ("".equals(highValue) || strParamVal.equals(strSep)) ? new String[] { "" } : split(highValue, strSep);
    final int      recursionLength = recursion.length;

    strRetList    = new String[recursionLength + 1];
    strRetList[0] = lowValue;

    for (int i = 0; i < recursionLength; i++) {
      strRetList[i + 1] = recursion[i];
    }

    return strRetList;
  }


  /**
   * Get the name or the value of a parameter. The parameter may look like:
   * <p>
   * <xmp>
   *   aname=bvalue
   *   -x=y
   *   --parameter=value
   * </xmp>
   * <p>
   * @param parameter A String as shown in the above examples.
   * @param name true if the name is required, false if the value is required.
   * @param casesensitive false if the parameters are to be converted to all upper case, else true.
   * @return Name or value, depending on the name parameter; null if an error occurred.
   */
  public static String getParameter(String parameter, boolean name, boolean casesensitive) {
    if (parameter == null) {
      return null;
    }

    String[] parametertokens = split(parameter, "=");

    if ((parametertokens == null) || ((parametertokens.length < 1) && name) || ((parametertokens.length < 2) && !name)) {
      return null;
    }

    if (name) {
      String parametername = parametertokens[0];
      while ("-".equals(parametername.substring(0, 1))) {
        parametername = parametername.substring(1);
      }

      if (casesensitive) {
        return parametername;
      } else {
        return parametername.toUpperCase();
      }
    }

    return parametertokens[1];
  }
  
  /**
   * Convert a String into an Integer and return 0 if this is not possible.<p>
   *
   * @param strString The String that has to be converted
   * @return The Integer Value
   */
  public static int StringToInteger(String strString) {
    try {
      return Integer.parseInt(strString);
    } catch (NumberFormatException e) {
      return 0;
    }
  }


  /**
   * Convert a String into an Integer and return a default value if this not possible.<p>
   *
   * @param strString The String that has to be converted
   * @param intDefault The default value that shall be returned on error
   * @return The Integer Value
   */
  public static int StringToInteger(String strString, int intDefault) {
    try {
      return Integer.parseInt(strString);
    } catch (NumberFormatException e) {
      return intDefault;
    }
  }
}
