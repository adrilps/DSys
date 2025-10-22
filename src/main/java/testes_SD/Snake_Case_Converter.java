package testes_SD;

public class Snake_Case_Converter {

    public static String convertToSnakeCase(String camelCaseString) {
        if (camelCaseString == null || camelCaseString.isEmpty()) {
            return camelCaseString;
        }

        StringBuilder snakeCaseBuilder = new StringBuilder();
        // Append the first character in lowercase
        snakeCaseBuilder.append(Character.toLowerCase(camelCaseString.charAt(0)));

        // Iterate from the second character
        for (int i = 1; i < camelCaseString.length(); i++) {
            char currentChar = camelCaseString.charAt(i);

            // If the current character is uppercase, append an underscore and then the lowercase character
            if (Character.isUpperCase(currentChar)) {
                snakeCaseBuilder.append('_');
                snakeCaseBuilder.append(Character.toLowerCase(currentChar));
            } else {
                // Otherwise, just append the character
                snakeCaseBuilder.append(currentChar);
            }
        }
        return snakeCaseBuilder.toString();
    }

    public static void main(String[] args) {
        String camelCaseExample1 = "firstName";
        String camelCaseExample2 = "lastName";
        String camelCaseExample3 = "userId";
        String camelCaseExample4 = "myLongVariableName";

        System.out.println(camelCaseExample1 + " -> " + convertToSnakeCase(camelCaseExample1));
        System.out.println(camelCaseExample2 + " -> " + convertToSnakeCase(camelCaseExample2));
        System.out.println(camelCaseExample3 + " -> " + convertToSnakeCase(camelCaseExample3));
        System.out.println(camelCaseExample4 + " -> " + convertToSnakeCase(camelCaseExample4));
    }
}