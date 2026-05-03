import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Clase principal del intérprete
public class NoiselessInterpreter {

    // Aquí se guardan todas las variables del programa
    private final Map<String, Object> variables = new HashMap<>();

    public static void main(String[] args) {

        // Se crea una instancia del intérprete
        NoiselessInterpreter interpreter = new NoiselessInterpreter();

        // Código escrito en Noiseless (como si fuera un script)
        String code = """
        set int a = 10
        set int b = 3

        set int c = add(a, b)
        print("Suma: " + c)

        set int d = mod(a, b)
        print("Módulo: " + d)

        set string saludo = "Hola "
        set string nombre = "Noiseless"
        print(concat(saludo, nombre))

        print("Ciclo while del 1 al 5")
        set int i = 1
        while (i <= 5)
            print(i)
            set i = add(i, 1)
        end

        if (a > b)
            print(a + " es mayor que " + b)
        end
        """;

        // Ejecuta el código
        interpreter.run(code);
    }

    // Metodo main que ejecuta el código
    public void run(String code) {

        // Divide el código en líneas
        String[] lines = code.split("\n");

        // Recorre cada línea
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Detecta si la linea está vacía
            if(line.isEmpty()) continue;

            // Separación de tokens
            List<String> tokens = tokenize(line);

            System.out.println("Línea: " + line);
            System.out.println("Tokens: " + tokens);
            System.out.println("\n");

            // Detecta tipo de instrucción
            if (line.startsWith("set")) {
                handleSet(line);
            }

            else if (line.startsWith("print")) {
                handlePrint(line);
            }

            else if (line.startsWith("if")) {
                String condition = extractCondition(line);

                // Si la condición es falsa, salta hasta "end"
                if (!evaluateCondition(condition)) {
                    while (!lines[i].trim().equals("end")) {
                        i++;
                    }
                }
            }

            else if (line.startsWith("while")) {
                String condition = extractCondition(line);

                int start = i + 1;
                List<String> block = new ArrayList<>();

                int j = start;

                // Guarda el bloque dentro del while
                while (!lines[j].trim().equals("end")) {
                    block.add(lines[j]);
                    j++;
                }

                // Ejecuta el bloque mientras la condición sea verdadera
                while (evaluateCondition(condition)) {
                    run(String.join("\n", block));
                }

                i = j;
            }
        }
    }

    // Maneja asignación de variables
    private void handleSet(String line) {

        // Divide en izquierda y derecha del "="
        String[] parts = line.split("=");

        if (parts.length < 2) {
            throw new RuntimeException("Error en set: " + line);
        }

        String left = parts[0].trim();
        String value = parts[1].trim();

        String[] leftParts = left.split(" ");

        String varName;

        // Caso: set int a
        if (leftParts.length == 3) {
            varName = leftParts[2];
        }
        // Caso: set a
        else if (leftParts.length == 2) {
            varName = leftParts[1];
        }
        else {
            throw new RuntimeException("Sintaxis inválida en set: " + line);
        }

        // Si es texto
        if (value.startsWith("\"")) {
            variables.put(varName, value.replace("\"", ""));
        }
        // Si es concat
        else if (value.startsWith("concat")) {
            variables.put(varName, handleConcat(value));
        }
        // Si es operación matemática
        else {
            variables.put(varName, evaluateExpression(value));
        }
    }

    // Maneja print con concatenación (+)
    private void handlePrint(String line) {

        int start = line.indexOf("(");
        int end = line.lastIndexOf(")");

        if (start == -1 || end == -1 || end <= start) {
            throw new RuntimeException(
                    "Error en instrucción print\n\n" +
                            "Línea: " + line + "\n\n" +
                            "Qué pasó:\n" +
                            "La instrucción print está mal formada. Faltan paréntesis o están mal ubicados.\n\n" +
                            "Solución:\n" +
                            "Usa la sintaxis correcta de print con paréntesis.\n" +
                            "Ejemplo: print(\"Hola mundo\")\n"
            );
        }

        String content = line.substring(start + 1, end).trim();

        // Divide por "+"
        String[] parts = content.split("\\+");

        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            part = part.trim();

            // Texto
            if (part.startsWith("\"") && part.endsWith("\"")) {
                result.append(part, 1, part.length() - 1);
            }
            // Variable
            else if (variables.containsKey(part)) {
                result.append(variables.get(part));
            }
            // concat()
            else if (part.startsWith("concat")) {
                result.append(handleConcat(part));
            }
            // Expresión matemática
            else {
                if (part.matches("\\d+")){
                    result.append(evaluateExpression(part));
                } else {
                    throw new RuntimeException(
                            "Error en instrucción print\n" +
                                    "Línea: " + line +
                                    "\nQué pasó:\n" +
                                    "La variable \"" + part + "\" no ha sido definida." +
                                    "\nSolución:\n" +
                                    "Declara la variable antes de utilizarla." +
                                    "\nEjemplo:\n" +
                                    "set int x = 10 \n" +
                                    "print(x)"
                    );
                }
            }
        }

        System.out.println(result);
    }

    // Concatena dos valores
    private Object handleConcat(String expr) {

        int start = expr.indexOf("(");
        int end = expr.lastIndexOf(")");

        String inside = expr.substring(start + 1, end);
        String[] parts = inside.split(",");

        String a = getValue(parts[0].trim()).toString();
        String b = getValue(parts[1].trim()).toString();

        return a + b;
    }

    // Evalúa expresiones matemáticas
    private int evaluateExpression(String expr) {

        expr = expr.replace(" ", "");

        if (expr.startsWith("add")) return operate(expr, "+");
        if (expr.startsWith("subtract")) return operate(expr, "-");
        if (expr.startsWith("multiply")) return operate(expr, "*");
        if (expr.startsWith("divide")) return operate(expr, "/");
        if (expr.startsWith("mod")) return operate(expr, "%");
        if (expr.startsWith("power")) return operate(expr, "^");

        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                    "\nError de valor numérico inválido\n" +
                            "Expresión: " + expr + "\n\n" +
                            "Qué pasó:\n" +
                            "Se esperaba un número, pero se recibió un valor no numérico.\n\n" +
                            "Solución:\n" +
                            "Usa un número válido o una variable que contenga un número.\n" +
                            "Ejemplo: set int a = 10\n"
            );
        }
    }

    // Realiza operaciones
    private int operate(String expr, String op) {

        String inside = expr.substring(expr.indexOf("(") + 1, expr.indexOf(")"));
        String[] parts = inside.split(",");

        int a = Integer.parseInt(getValue(parts[0].trim()).toString());
        int b = Integer.parseInt(getValue(parts[1].trim()).toString());

        return switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;
            case "%" -> a % b;
            case "^" -> (int) Math.pow(a, b);
            default -> 0;
        };
    }

    // Obtiene valor de variable o numero
    private Object getValue(String token) {
        if (variables.containsKey(token)) {
            return variables.get(token);
        }
        return Integer.parseInt(token);
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

        throw new RuntimeException("Condición inválida");
    }

    // Convierte a entero
    private int getInt(String val) {
        val = val.trim();

        if (variables.containsKey(val)) {
            return Integer.parseInt(variables.get(val).toString());
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
}