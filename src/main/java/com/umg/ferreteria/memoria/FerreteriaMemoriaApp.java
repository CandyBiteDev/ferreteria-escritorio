package com.umg.ferreteria.memoria;

import com.umg.ferreteria.vista.Ui;

import javax.swing.SwingUtilities;

/**
 * ENTREGA 2 — Punto de entrada de la versión en memoria.
 * No usa Spring, MySQL ni la API: todo vive en ArrayList mientras la ventana está abierta.
 * En IntelliJ: clic en el ▶ verde junto a main.
 */
public class FerreteriaMemoriaApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.instalarTema();
            new VentanaMemoria(new InventarioMemoria()).setVisible(true);
        });
    }
}
