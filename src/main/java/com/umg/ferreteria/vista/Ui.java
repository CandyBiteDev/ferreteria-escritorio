package com.umg.ferreteria.vista;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.umg.ferreteria.excepcion.*;
import com.umg.ferreteria.modelo.EntidadBase;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Utilidades visuales compartidas por todas las pantallas. */
public final class Ui {

    public static final Color PRIMARIO = new Color(0xD35400);
    public static final Color EXITO = new Color(0x27AE60);
    public static final Color PELIGRO = new Color(0xC0392B);
    public static final Color INFO = new Color(0x2471A3);
    public static final Color FONDO_ALERTA = new Color(0xFDEDEC);

    private static final DecimalFormat MONEDA = new DecimalFormat("Q #,##0.00");
    private static final DecimalFormat CANTIDAD = new DecimalFormat("#,##0.###");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Ui() {
    }

    public static void instalarTema() {
        try {
            FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#D35400"));
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TitlePane.unifiedBackground", true);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
                // se queda el look and feel por defecto
            }
        }
    }

    // ---------- Botones ----------
    public static JButton boton(String texto, Color color) {
        JButton b = new JButton(texto);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------- Mensajes ----------
    /** Muestra la excepción con un título acorde a su tipo (polimorfismo de excepciones propias). */
    public static void error(Component padre, Throwable e) {
        String titulo;
        int icono = JOptionPane.WARNING_MESSAGE;
        if (e instanceof StockInsuficienteException) titulo = "Stock insuficiente";
        else if (e instanceof ReglaNegocioException) titulo = "Regla de negocio";
        else if (e instanceof ValidacionException) titulo = "Datos inválidos";
        else if (e instanceof AccesoDenegadoException) titulo = "Acceso denegado";
        else if (e instanceof AutenticacionException) titulo = "Autenticación";
        else if (e instanceof PersistenciaException) {
            titulo = "Error de base de datos";
            icono = JOptionPane.ERROR_MESSAGE;
        } else {
            titulo = "Error inesperado";
            icono = JOptionPane.ERROR_MESSAGE;
            e.printStackTrace();
        }
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        JOptionPane.showMessageDialog(padre, envolver(msg), titulo, icono);
    }

    public static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, envolver(mensaje), "Ferretería", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirmar(Component padre, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, envolver(mensaje), "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private static String envolver(String msg) {
        return "<html><body style='width:320px'>" + msg.replace("\n", "<br>") + "</body></html>";
    }

    /** Ejecuta una acción de la vista mostrando cualquier error como diálogo. */
    public static void accion(Component padre, Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            error(padre, e);
        }
    }

    // ---------- Conversión de entradas ----------
    public static BigDecimal decimal(JTextField campo, String nombre) {
        String t = campo.getText().trim().replace(",", "");
        if (t.isEmpty()) throw new ValidacionException("Ingrese " + nombre + ".");
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            throw new ValidacionException("'" + campo.getText() + "' no es un número válido para " + nombre + ".");
        }
    }

    public static int entero(JTextField campo, String nombre) {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            throw new ValidacionException("'" + campo.getText() + "' no es un número entero válido para " + nombre + ".");
        }
    }

    public static String moneda(BigDecimal v) {
        return v == null ? "" : MONEDA.format(v);
    }

    public static String cantidad(BigDecimal v) {
        return v == null ? "" : CANTIDAD.format(v);
    }

    public static String fecha(LocalDateTime f) {
        return f == null ? "" : f.format(FECHA);
    }

    // ---------- Tablas ----------
    public static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JTable tabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setRowHeight(28);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        t.setFillsViewportHeight(true);
        return t;
    }

    /** Índice en el modelo de la fila seleccionada (respeta el ordenamiento), o -1. */
    public static int filaSeleccionada(JTable t) {
        int v = t.getSelectedRow();
        return v < 0 ? -1 : t.convertRowIndexToModel(v);
    }

    /** Exporta lo visible de la tabla a CSV (UTF-8 con BOM para que Excel respete tildes). */
    public static void exportarCsv(Component padre, JTable tabla, String nombreSugerido) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(nombreSugerido.replaceAll("[^A-Za-z0-9áéíóúñÁÉÍÓÚÑ _-]", "") + ".csv"));
        if (fc.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) return;
        StringBuilder sb = new StringBuilder("\uFEFF");
        for (int c = 0; c < tabla.getColumnCount(); c++) {
            sb.append(c > 0 ? "," : "").append(csv(tabla.getColumnName(c)));
        }
        sb.append("\r\n");
        for (int r = 0; r < tabla.getRowCount(); r++) {
            for (int c = 0; c < tabla.getColumnCount(); c++) {
                Object v = tabla.getValueAt(r, c);
                sb.append(c > 0 ? "," : "").append(csv(v == null ? "" : v.toString()));
            }
            sb.append("\r\n");
        }
        try {
            Files.writeString(fc.getSelectedFile().toPath(), sb.toString(), StandardCharsets.UTF_8);
            info(padre, "Reporte exportado a:\n" + fc.getSelectedFile().getAbsolutePath());
        } catch (IOException e) {
            error(padre, new RuntimeException("No se pudo guardar el archivo: " + e.getMessage()));
        }
    }

    private static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    // ---------- Combos ----------
    public static <T> JComboBox<T> combo(List<T> items) {
        JComboBox<T> c = new JComboBox<>();
        items.forEach(c::addItem);
        return c;
    }

    public static <T> void recargar(JComboBox<T> combo, List<T> items) {
        combo.removeAllItems();
        items.forEach(combo::addItem);
    }

    public static <T extends EntidadBase> void seleccionar(JComboBox<T> combo, Integer id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            T item = combo.getItemAt(i);
            if (item != null && item.getId() != null && item.getId().equals(id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(-1);
    }

    public static Integer idDe(JComboBox<? extends EntidadBase> combo) {
        Object s = combo.getSelectedItem();
        return s instanceof EntidadBase e ? e.getId() : null;
    }

    // ---------- Encabezados y formularios ----------
    public static JPanel encabezado(String titulo, String subtitulo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(0, 0, 10, 0));
        JLabel t = new JLabel(titulo);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 20f));
        JLabel s = new JLabel(subtitulo);
        s.setForeground(Color.GRAY);
        p.add(t, BorderLayout.NORTH);
        p.add(s, BorderLayout.SOUTH);
        return p;
    }

    /** Formulario de dos columnas (etiqueta / campo) con GridBagLayout. */
    public static class Formulario extends JPanel {
        private int fila;

        public Formulario() {
            super(new GridBagLayout());
        }

        public Formulario campo(String etiqueta, JComponent componente) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = fila;
            g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(4, 0, 4, 8);
            JLabel l = new JLabel(etiqueta);
            l.setFont(l.getFont().deriveFont(Font.BOLD));
            add(l, g);
            g.gridx = 1;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.insets = new Insets(4, 0, 4, 0);
            add(componente, g);
            fila++;
            return this;
        }

        /** Empuja los campos hacia arriba. */
        public Formulario relleno() {
            GridBagConstraints g = new GridBagConstraints();
            g.gridy = fila++;
            g.weighty = 1;
            add(Box.createGlue(), g);
            return this;
        }
    }
}
