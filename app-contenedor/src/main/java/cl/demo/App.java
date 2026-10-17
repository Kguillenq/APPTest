package cl.demo;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/*
 * Libreta de notas por usuario (aplicación de consola).
 *
 * Qué hace:
 *   1. Pide un nombre de usuario y una nota.
 *   2. Guarda la nota en <carpeta de datos>/<usuario>.txt
 *   3. Muestra cuántas notas tiene ese usuario.
 *
 * Se puede usar de dos formas:
 *   - Interactiva:      java -jar libreta.jar
 *   - Con argumentos:   java -jar libreta.jar ana "comprar pan"
 *
 * La carpeta de datos se toma de la variable de entorno DATA_DIR
 * (por defecto "datos"). En el contenedor se monta como volumen.
 */
public class App {

    private static final Logger LOG = Logger.getLogger(App.class);

    private static final Path CARPETA_DATOS = Paths
            .get(System.getenv().getOrDefault("DATA_DIR", "datos"))
            .toAbsolutePath().normalize();

    // Solo letras, números, punto, guion y guion bajo; debe empezar con
    // letra o número.
    private static final Pattern PATRON_USUARIO =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,29}$");

    private static final int LARGO_MAX_NOTA = 200;

    public static void main(String[] args) {
        LOG.info("Aplicación iniciada");

        String usuario;
        String nota;

        if (args.length >= 2) {
            usuario = args[0];
            nota = args[1];
        } else {
            try (Scanner sc = new Scanner(System.in)) {
                System.out.print("Ingrese nombre de usuario: ");
                if (!sc.hasNextLine()) {
                    System.out.println("No se recibió entrada.");
                    return;
                }
                usuario = sc.nextLine();

                System.out.print("Ingrese su nota: ");
                if (!sc.hasNextLine()) {
                    System.out.println("No se recibió entrada.");
                    return;
                }
                nota = sc.nextLine();
            }
        }

        if (!PATRON_USUARIO.matcher(usuario).matches()) {
            System.out.println("Nombre de usuario inválido.");
            return;
        }
        if (nota.isBlank() || nota.length() > LARGO_MAX_NOTA) {
            System.out.println("La nota debe tener entre 1 y " + LARGO_MAX_NOTA + " caracteres.");
            return;
        }

        guardarNota(usuario, nota);
    }

    private static void guardarNota(String usuario, String nota) {
        try {
            Files.createDirectories(CARPETA_DATOS);

            // La ruta se resuelve dentro de la carpeta de datos y se
            // normaliza; si queda fuera, se rechaza.
            Path archivo = CARPETA_DATOS.resolve(usuario + ".txt").normalize();
            if (!archivo.startsWith(CARPETA_DATOS)) {
                System.out.println("Ruta no permitida.");
                return;
            }

            // Se quitan saltos de línea para que cada nota sea una línea.
            String linea = nota.replace("\r", " ").replace("\n", " ") + System.lineSeparator();
            Files.write(archivo, linea.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            List<String> notas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            System.out.println("Nota guardada. " + usuario + " tiene " + notas.size() + " nota(s).");
            LOG.info("Nota guardada para el usuario " + usuario);

        } catch (IOException e) {
            LOG.error("No se pudo guardar la nota", e);
            System.out.println("No se pudo guardar la nota.");
        }
    }
}
