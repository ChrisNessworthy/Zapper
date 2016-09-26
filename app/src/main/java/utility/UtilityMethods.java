package utility;

public class UtilityMethods {

    /**
     * Removes whitespace from text string
     * @param text string to clear whitespace from
     * @return Whitespace cleared text
     */
    public static String removeWhitespaceFromText(String text){
        String clearedText = text.replace(" ", "");

        return clearedText;
    }

    /**
     * Validates that text string is longer than 1 character
     * @param text string to validate
     * @return boolean
     */
    public static boolean isStringValidLength(String text){
        if (text.length() > 0) {
            return true;
        } else {
            return false;
        }
    }
}
