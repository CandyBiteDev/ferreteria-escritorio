package com.umg.ferreteria;

import com.umg.ferreteria.vista.AppEscritorio;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

/**
 * Arranca en un solo proceso:
 *  - el servidor API REST (puerto 8080) que consume la app Android, y
 *  - la aplicación de escritorio Swing.
 * Con el argumento --sin-ventana solo se levanta la API.
 */
@SpringBootApplication
public class FerreteriaApplication {

    public static void main(String[] args) {
        boolean sinVentana = Arrays.asList(args).contains("--sin-ventana") || GraphicsEnvironment.isHeadless();
        ConfigurableApplicationContext context = new SpringApplicationBuilder(FerreteriaApplication.class)
                .headless(sinVentana)
                .run(args);

        if (!sinVentana) {
            AppEscritorio app = context.getBean(AppEscritorio.class);
            SwingUtilities.invokeLater(app::iniciar);
        }
    }
}
