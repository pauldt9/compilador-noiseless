/**
 * Intérprete del lenguaje Noiseless
 * Soporta variables de int y texto, operaciones aritméticas,
 * concatenación, condicionales (si), ciclos (mientras), impresión y asignaciones
 *
 * El intérprete trabaja línea por línea y cuenta con un modo bloque para estructuras
 * de varias líneas (si/mientras). Mantiene un mapa de variables con su tipo
 * y valor, y registra los tokens de cada línea ejecutada
 */

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Clase principal del intérprete
public class NoiselessInterpreter {

    // Aquí se guardan todas las variables del programa con tipo y valor
    private final Map<String, Variable> variables = new HashMap<>();

    // Map para guardar los tokens por linea
    private final Map<Integer, List<String>> tokensForLine = new LinkedHashMap<>();

    // Contador para numero de linea
    private int globalLineNumber = 1;

    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";

    public static void main(String[] args) {

        // Instancia del intérprete
        NoiselessInterpreter interpreter = new NoiselessInterpreter();

        System.out.println(CYAN + "Guía del lenguaje Noiseless" + RESET);

        System.out.println(YELLOW + "TIPOS DE DATOS" + RESET);

        System.out.println("""
            int  -> números enteros
            texto   -> cadenas de texto
            """);

        System.out.println(YELLOW + "DECLARACIÓN DE VARIABLES" + RESET);

        System.out.println("""
                definir int edad = 20
                definir texto nombre = Juan
                """);

        System.out.println(YELLOW + "IMPRIMIR EN CONSOLA" + RESET);

        System.out.println("""
            imprimir("Hola mundo")
            imprimir(nombre)
            imprimir("Edad: " + edad)
            """);

        System.out.println(YELLOW + "OPERACIONES MATEMÁTICAS" + RESET);

        System.out.println("""
            sumar(a,b)
            restar(a,b)
            multiplicar(a,b)
            dividir(a,b)
            modulo(a,b)
            potencia(a,b)
            """);

        System.out.println(YELLOW + "EJEMPLOS" + RESET);

        System.out.println("""
            definir int a = 10
            definir int b = 5
            
            definir int c = sumar(a,b)
            
            imprimir(c)
            """);

        System.out.println(YELLOW + "CONCATENACIÓN" + RESET);

        System.out.println("concatenar(" + "Hola" + "," + " Mundo)\n");

        System.out.println(YELLOW + "CONDICIONALES" + RESET);

        System.out.println("""
            si (a > b)
                imprimir("a es mayor")
            fin
            """);

        System.out.println(YELLOW + "CICLOS" + RESET);

        System.out.println("""
            mientras (a < 10)
                imprimir(a)
            fin
            """);

        System.out.println(YELLOW + "OPERADORES LÓGICOS" + RESET);

        System.out.println("""
            >
            <
            >=
            <=
            ==
            """);

        System.out.println(YELLOW + "COMANDOS DEL INTÉRPRETE" + RESET);

        System.out.println("""
            tokens      -> mostrar tokens de una línea
            variables   -> mostrar variables almacenadas
            salir       -> cerrar intérprete
            """);

        Scanner scanner = new Scanner(System.in);

        // "Modo bloque" para bloques de código de más de una línea (como el while)
        // Se utiliza un deck a modo de pila
        Deque<StringBuilder> blockStack = new ArrayDeque<>();

        while (true) {

            // Prompt distinto si está dentro de bloque
            if (!blockStack.isEmpty()) {
                System.out.print(YELLOW + "... " + RESET);
            } else {
                System.out.print(GREEN + ">> " + RESET);
            }

            String input = scanner.nextLine();

            if (input.trim().isEmpty()) {
                continue;
            }

            // SALIR
            if (input.equalsIgnoreCase("salir")) {
                System.out.println(RED + "Cerrando Noiseless..." + RESET);
                break;
            }

            // TOKENS
            else if (input.equalsIgnoreCase("tokens")) {

                System.out.print("Número de línea: ");

                int line = Integer.parseInt(scanner.nextLine());

                interpreter.printTokensOfLine(line);
            }

            // VARIABLES
            else if (input.equalsIgnoreCase("variables")) {

                interpreter.showVariables();
            }

            // INICIO DE BLOQUE
            else if (
                    input.trim().startsWith("mientras") ||
                    input.trim().startsWith("si")
            ) {
                StringBuilder newBlock = new StringBuilder();
                newBlock.append(input).append("\n");
                blockStack.push(newBlock);
            }
            // FIN DE BLOQUE
            // Cuando encontramos "fin" y la pila no está vacía, extraemos el bloque completo y lo ejecutamos
            else if (input.trim().equals("fin") && !blockStack.isEmpty()) {
                StringBuilder completed = blockStack.pop();
                interpreter.run(completed.toString());
            // Si estamos dentro de un bloque pero no es "fin", se añade la línea al bloque actual
            } else if (!blockStack.isEmpty()) {
                blockStack.peek().append(input).append("\n");
            }
            // EJECUCIÓN NORMAL
            else {
                interpreter.run(input);
            }
        }
    }

    // Metodo que ejecuta el fragmento de código
    public boolean run(String code) {

        // Divide el código en líneas
        String[] lines = code.split("\n");

        // Recorre cada línea
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Guarda la linea actual antes de saltar
            int currentLineNumber = globalLineNumber;

            try {
                // Detecta si la linea está vacía
                if (line.isEmpty()) continue;

                // Separación de tokens
                List<String> tokens = tokenize(line);

                // Almacena los tokens en la linea correspondiente y suma al contador
                tokensForLine.put(currentLineNumber, tokens);
                globalLineNumber++;

                // Ignorar cierre de bloques
                if (line.equals("fin")){
                    continue;
                }

                // Detecta tipo de instrucción
                if (
                        line.startsWith("sumar") ||
                        line.startsWith("restar") ||
                        line.startsWith("multiplicar") ||
                        line.startsWith("dividir") ||
                        line.startsWith("modulo") ||
                        line.startsWith("potencia")
                ) {

                    int result = evaluateExpression(line);

                    System.out.println(GREEN + result + RESET);
                }else if (line.startsWith("definir")) {
                    handleSet(line);
                } else if (line.contains("=") && !line.startsWith("definir")){
                    handleAssignment(line);
                } else if (line.startsWith("imprimir")) {
                    handlePrint(line);
                } else if (line.startsWith("si")) {
                    String condition = extractCondition(line);

                    // Si la condición es falsa, salta hasta "fin"
                    if (!evaluateCondition(condition)) {
                        int nested = 1; // revisa anidacion para ignorar "si" internos y sus "fin"
                        while (i < lines.length && nested > 0) {
                            i++;
                            if (i >= lines.length) {
                                throw new NoiselessException(
                                        "Bloque 'si' sin cerrar con 'fin'"
                                );
                            }
                            String l = lines[i].trim();
                            if (l.startsWith("si")) nested++;
                            else if (l.equals("fin")) nested--;
                        }
                    }

                } else if (line.startsWith("mientras")) {
                    String condition = extractCondition(line);

                    int start = i + 1;
                    List<String> block = new ArrayList<>();

                    int j = start;

                    while (j < lines.length && !lines[j].trim().equals("fin")) {
                        block.add(lines[j]);
                        j++;
                    }

                    // Ejecuta el bloque mientras la condición sea verdadera
                    while (evaluateCondition(condition)) {

                        // Cada iteración llama recursivamente a run() con el bloque capturado
                        boolean success = run(String.join("\n", block));
                        // Si hubo error, romper ciclo
                        if (!success) {
                            System.out.println(
                                    RED +
                                            "Ejecución del ciclo detenida por error." +
                                            RESET
                            );
                            break;
                        }
                    }

                    if (j > lines.length) {
                        throw new NoiselessException(
                                "Bloque 'mientras' sin cerrar con 'fin'"
                        );
                    }
                    // Salta la línea "fin" para que el bucle principal no la procese de nuevo
                    i = j;
                } else {
                    throw new NoiselessException(
                            "Instrucción desconocida:\n" +
                            line +
                            "\n\nComando no reconocido"
                    );
                }
            }catch (Exception e) {
                System.out.println(
                        RED +
                                "\n[ERROR EN LÍNEA " +
                                currentLineNumber +
                                "]\n" +
                                e.getMessage() +
                                RESET
                );
                return false;
            }
        }
        return true;
    }

    // Maneja asignación de variables
    private void handleSet(String line) {

        // Divide en izquierda y derecha del "="
        String[] parts = line.split("=",2);
        if (!line.contains("=")) {

            throw new NoiselessException(
                    """
                            Error de sintaxis en definir
                            
                            Falta '='
                            
                            Ejemplo correcto:
                            definir int x = 10"""
            );
        }

        if (parts.length < 2) {
            throw new NoiselessException("Falta '=' en instrucción definir: " + line);
        }

        String left = parts[0].trim();
        String valueExpr = parts[1].trim();

        String[] leftTokens = left.split(" ");
        String varName;
        String varType = null;

        // Parsear "definir int a" o "definir texto a"
        if (leftTokens.length == 3 && leftTokens[0].equals("definir")) {
            varType = leftTokens[1];   // "int" o "texto"
            varName = leftTokens[2];
        } else if (leftTokens.length == 2 && leftTokens[0].equals("definir")) {
            // Si no se especifica tipo, lo detectamos/obligamos automáticamente
            throw new NoiselessException("Debes especificar el tipo (int o texto) en definir: " + line);
        } else {
            throw new NoiselessException("Sintaxis inválida en definir: " + line);
        }

        // Evaluar el valor según el tipo
        Object value;
        if (varType.equals("int")) {
            value = evaluateExpression(valueExpr); // retorna Integer
        } else if (varType.equals("texto")) {
            value = evaluateTextExpression(valueExpr);
        } else {
            throw new NoiselessException("Tipo desconocido: " + varType + ". Usa 'int' o 'texto'.");
        }

        variables.put(varName, new Variable(varType, value));
    }

    private void handleAssignment(String line) {
        String[] parts = line.split("=", 2);
        if (parts.length < 2) {
            throw new NoiselessException("Asignación inválida: falta '='");
        }
        String varName = parts[0].trim();
        String valueExpr = parts[1].trim();

        if (!variables.containsKey(varName)) {
            throw new NoiselessException("Variable no definida: " + varName);
        }

        Variable var = variables.get(varName);
        Object newValue;

        if (var.type.equals("int")) {
            newValue = evaluateExpression(valueExpr);
        } else if (var.type.equals("texto")) {
            newValue = evaluateTextExpression(valueExpr);
        } else {
            throw new NoiselessException("Tipo desconocido: " + var.type);
        }

        var.value = newValue;
    }

    // Maneja imprimir con concatenación (+)
    private void handlePrint(String line) {
        String content = extractParenthesesContent(line);
        List<String> parts = splitByPlusOutsideQuotes(content);
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            result.append(evaluateTextExpression(part.trim()));
        }
        System.out.println(GREEN + result + RESET);
    }

    // Concatena dos valores
    private String handleConcat(String expr) {

        int start = expr.indexOf("(");
        int end = expr.lastIndexOf(")");
        if (start == -1 || end == -1 || end <= start) {
            throw new NoiselessException(
                    "Error en concatenar: Faltan paréntesis"
            );
        }

        String inside = expr.substring(start + 1, end);
        String[] parts = inside.split(",");
        if (parts.length != 2) {
            throw new NoiselessException(
                    "Error en instrucción concatenar\n" +
                            "Expresión: " + expr + "\n" +
                            "Qué pasó:\n" +
                            "Se requieren exactamente 2 argumentos separados por coma.\n" +
                            "Recibidos: " + parts.length + "\n" +
                            "Solución:\n" +
                            "Escribe: concatenar(primerValor, segundoValor)\n" +
                            "Ejemplo: concatenar(\"Hola\", \"Mundo\")"
            );
        }

        String a = extractStringOrVariable(parts[0].trim());
        String b = extractStringOrVariable(parts[1].trim());

        return a + b;
    }

    // Obtiene el valor textual de un token para ser usado en concatenación
    private String extractStringOrVariable(String token) {
        // Si es texto entre comillas
        if (token.startsWith("\"") && token.endsWith("\"")) {
            return token.substring(1, token.length() - 1);
        }
        // Si es una variable
        if (variables.containsKey(token)) {
            Object value = variables.get(token);
            return value.toString();
        }
        // Si es un número (lo convertimos a texto)
        try {
            return Integer.toString(Integer.parseInt(token));
        } catch (NumberFormatException e) {
            throw new NoiselessException(
                    "Valor inválido en concatenar: " + token + "\n" +
                            "Asegúrate de usar texto entre comillas o una variable definida."
            );
        }
    }

    // Evalúa expresiones matemáticas
    private int evaluateExpression(String expr) {

        expr = expr.replace(" ", "");

        if (expr.startsWith("sumar")) return operate(expr, "+");
        if (expr.startsWith("restar")) return operate(expr, "-");
        if (expr.startsWith("multiplicar")) return operate(expr, "*");
        if (expr.startsWith("dividir")) return operate(expr, "/");
        if (expr.startsWith("modulo")) return operate(expr, "%");
        if (expr.startsWith("potencia")) return operate(expr, "^");

        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            throw new NoiselessException(
                    "\nError de valor numérico inválido\n" +
                            "Expresión: " + expr + "\n\n" +
                            "Qué pasó:\n" +
                            "Se esperaba un número, pero se recibió un valor no numérico.\n\n" +
                            "Solución:\n" +
                            "Usa un número válido o una variable que contenga un número.\n" +
                            "Ejemplo: definir int a = 10\n"
            );
        }
    }

    // Realiza operaciones
    private int operate(String expr, String op) {

        if (!expr.contains("(") || !expr.contains(")")) {

            throw new NoiselessException(
                    "Sintaxis inválida en operación:\n" +
                            expr +
                            "\n\nFaltan paréntesis."
            );
        }

        String inside = expr.substring(
                expr.indexOf("(") + 1,
                expr.lastIndexOf(")")
        );

        String[] parts = inside.split(",");

        if (parts.length != 2){

            throw new NoiselessException(
                    "Operación inválida:\n" +
                            expr +
                            "\n\nLas operaciones requieren 2 parámetros."
            );
        }

        int a = Integer.parseInt(getValue(parts[0].trim()).toString());
        int b = Integer.parseInt(getValue(parts[1].trim()).toString());

        return switch (op) {

            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> {

                if (b == 0)
                    throw new NoiselessException("División entre cero");

                yield a / b;
            }
            case "%" -> {

                if (b == 0)
                    throw new NoiselessException("Módulo entre cero");

                yield a % b;
            }
            case "^" -> (int) Math.pow(a, b);

            default -> throw new NoiselessException(
                    "Operador desconocido"
            );
        };
    }

    // Obtiene valor de variable o numero
    private Object getValue(String token) {
        if (variables.containsKey(token)) {
            Variable var = variables.get(token);
            if (!var.type.equals("int")) {
                throw new NoiselessException("La variable '" + token + "' es de tipo " + var.type + ", no se puede usar en operación aritmética.");
            }
            return var.value;
        }

        try{
            return Integer.parseInt(token);
        }catch (NumberFormatException e) {
            throw new NoiselessException("Variable o valor invalido: " + token);
        }
    }

    // Extraer condicion de if/while
    private String extractCondition(String line) {
        return line.substring(line.indexOf("(") + 1, line.indexOf(")"));
    }

    // Evalúa condiciones
    private boolean evaluateCondition(String cond) {

        cond = cond.replace(" ", "");

        if (cond.contains("<=")) {
            String[] parts = cond.split("<=");
            return getInt(parts[0]) <= getInt(parts[1]);
        }
        else if (cond.contains(">=")) {
            String[] parts = cond.split(">=");
            return getInt(parts[0]) >= getInt(parts[1]);
        }
        else if (cond.contains("==")) {
            String[] parts = cond.split("==");
            return getInt(parts[0]) == getInt(parts[1]);
        }
        else if (cond.contains(">")) {
            String[] parts = cond.split(">");
            return getInt(parts[0]) > getInt(parts[1]);
        }
        else if (cond.contains("<")) {
            String[] parts = cond.split("<");
            return getInt(parts[0]) < getInt(parts[1]);
        }

        throw new NoiselessException("Condición inválida: " + cond);
    }

    private int getInt(String val) {
        val = val.trim();
        if (variables.containsKey(val)) {
            Variable var = variables.get(val);
            if (!var.type.equals("int")) {
                throw new NoiselessException("No se puede comparar una variable de tipo " + var.type + " con números.");
            }
            return (Integer) var.value;
        }
        return Integer.parseInt(val);
    }

    private List<String> tokenize(String line) {
        // Lista para guardar los tokens encontrados
        List<String> tokens = new ArrayList<>();

        // Expresión regular que define qué es un token
        // "\"[^\"]*\""     = cadenas de texto entre comillas
        // \\w+             = palabras (variables, keywords)
        // == <= >=         = operadores dobles
        // [()+\-*/%=,<>]   = símbolos individuales
        String regex = "\"[^\"]*\"|\\w+|==|<=|>=|[()+\\-*/%=,<>]";

        // Matcher para buscar coincidencias en la línea
        Matcher matcher = Pattern.compile(regex).matcher(line);

        // Recorre cada coincidencia encontrada
        while (matcher.find()) {
            // Y siendo que cada coincidencia es un token, lo añade a la lista
            tokens.add(matcher.group());
        }

        return tokens;
    }

    public void printTokensOfLine(int lineNumber) {

        // Verifica si la línea existe
        if (tokensForLine.containsKey(lineNumber)) {

            System.out.println(
                    "Tokens de la línea " +
                            lineNumber +
                            ": " +
                            tokensForLine.get(lineNumber)
            );

        } else {

            System.out.println(
                    "No existen tokens para la línea " +
                            lineNumber
            );
        }
    }

    public void showVariables() {
        if (variables.isEmpty()) {
            System.out.println(YELLOW + "No hay variables." + RESET);
            return;
        }
        System.out.println(CYAN + "\n=== VARIABLES ===" + RESET);
        for (Map.Entry<String, Variable> entry : variables.entrySet()) {
            System.out.println(entry.getKey() + " (" + entry.getValue().type + ") = " + entry.getValue().value);
        }
        System.out.println();
    }
    // Variable de Noiseless, guarda el tipo ("int" o "texto") y el valor (Integer o String)
    private static class Variable {
        String type;   // "int" o "texto"
        Object value;  // Integer o String

        Variable(String type, Object value) {
            this.type = type;
            this.value = value;
        }
    }
    private String evaluateTextExpression(String expr) {
        expr = expr.trim();

        // Literal entre comillas
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return expr.substring(1, expr.length() - 1);
        }

        // Concatenación
        if (expr.startsWith("concatenar")) {
            return handleConcat(expr);
        }

        // Variable
        if (variables.containsKey(expr)) {
            Variable var = variables.get(expr);
            if (!var.type.equals("texto")) {
                throw new NoiselessException("No se puede asignar una variable de tipo " + var.type + " a una variable de tipo texto");
            }
            return (String) var.value;
        }

        // Si es número sin comillas, da error
        try {
            Integer.parseInt(expr);
            throw new NoiselessException(
                    "No se puede asignar un número (" + expr + ") a una variable de tipo texto. " +
                            "Usa comillas: \"" + expr + "\""
            );
        } catch (NumberFormatException e) {
            // No es número, entonces es otro texto inválido
            throw new NoiselessException("Valor de texto inválido: " + expr + ". Debe estar entre comillas.");
        }
    }
    private String extractParenthesesContent(String line) {
        int start = line.indexOf("(");
        int end = line.lastIndexOf(")");
        if (start == -1 || end == -1) {
            throw new NoiselessException("Faltan paréntesis en imprimir");
        }
        return line.substring(start + 1, end);
    }

    /**
     * Divide una cadena por el carácter '+', pero respetando que los '+' dentro de comillas
     * no se consideran separadores. Es para procesar la expresión dentro de imprimir().
     */
    private List<String> splitByPlusOutsideQuotes(String s) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == '+' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }
}