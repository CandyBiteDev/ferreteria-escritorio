package com.umg.ferreteria.vista;

import com.umg.ferreteria.modelo.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Consulta de stock por bodega con filtros y registro de ajustes autorizados. */
public class PanelExistencias extends JPanel {

    private final AppEscritorio app;
    private final Usuario usuario;
    private final JComboBox<Object> cbBodega = new JComboBox<>();
    private final JTextField txtBuscar = new JTextField(20);
    private final JCheckBox chkBajoMinimo = new JCheckBox("Solo bajo mínimo");
    private final DefaultTableModel modelo = Ui.modelo("Bodega", "Código", "Producto", "Categoría", "Existencia", "Unidad", "Mínimo", "Estado");
    private final JTable tabla = Ui.tabla(modelo);
    private final JLabel lblResumen = new JLabel(" ");

    public PanelExistencias(AppEscritorio app, Usuario usuario) {
        super(new BorderLayout(8, 8));
        this.app = app;
        this.usuario = usuario;
        setBorder(new EmptyBorder(12, 12, 12, 12));

        cbBodega.addItem("Todas las bodegas");
        for (Bodega b : app.catalogo().listar(app.catalogo().bodegas())) {
            cbBodega.addItem(b);
            if (b.getId().equals(usuario.getIdBodega())) cbBodega.setSelectedItem(b);
        }
        txtBuscar.putClientProperty("JTextField.placeholderText", "Código, nombre o categoría");
        txtBuscar.addActionListener(e -> buscar());
        cbBodega.addActionListener(e -> buscar());
        chkBajoMinimo.addActionListener(e -> buscar());

        JButton btnBuscar = Ui.boton("Buscar");
        btnBuscar.addActionListener(e -> buscar());
        JButton btnAjuste = Ui.boton("Ajuste de inventario", Ui.PRIMARIO);
        btnAjuste.addActionListener(e -> Ui.accion(this, this::abrirAjuste));
        JButton btnExportar = Ui.boton("Exportar CSV");
        btnExportar.addActionListener(e -> Ui.exportarCsv(this, tabla, "existencias"));

        JPanel filtros = WrapLayout.barra();
        filtros.add(new JLabel("Bodega:"));
        filtros.add(cbBodega);
        filtros.add(txtBuscar);
        filtros.add(chkBajoMinimo);
        filtros.add(btnBuscar);
        filtros.add(btnAjuste);
        filtros.add(btnExportar);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(Ui.encabezado("Existencias por bodega", "Stock en unidad base. En rojo: productos por debajo del mínimo."), BorderLayout.NORTH);
        norte.add(filtros, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(lblResumen, BorderLayout.SOUTH);

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
                boolean bajo = "BAJO MÍNIMO".equals(t.getValueAt(fila, 7));
                if (!sel) c.setBackground(bajo ? Ui.FONDO_ALERTA : t.getBackground());
                c.setForeground(bajo && col == 7 ? Ui.PELIGRO : sel ? t.getSelectionForeground() : t.getForeground());
                return c;
            }
        });
        buscar();
    }

    private Integer bodegaSeleccionada() {
        return cbBodega.getSelectedItem() instanceof Bodega b ? b.getId() : null;
    }

    private void buscar() {
        Ui.accion(this, () -> {
            List<Existencia> lista = app.inventario().existencias(bodegaSeleccionada(), txtBuscar.getText());
            modelo.setRowCount(0);
            int bajos = 0;
            for (Existencia e : lista) {
                if (e.isBajoMinimo()) bajos++;
                else if (chkBajoMinimo.isSelected()) continue;
                modelo.addRow(new Object[]{e.getBodega(), e.getCodigo(), e.getProducto(), e.getCategoria(),
                        Ui.cantidad(e.getCantidad()), e.getUnidad(), e.getStockMinimo(), e.isBajoMinimo() ? "BAJO MÍNIMO" : "OK"});
            }
            lblResumen.setText(lista.size() + " registros · " + bajos + " bajo el mínimo");
        });
    }

    /** Regla: un ajuste requiere motivo y autorización de un usuario con permisos. */
    private void abrirAjuste() {
        JComboBox<Producto> cbProd = Ui.combo(app.catalogo().productosActivos());
        List<Bodega> bodegas = new ArrayList<>(app.catalogo().listar(app.catalogo().bodegas()));
        JComboBox<Bodega> cbBod = Ui.combo(bodegas);
        if (bodegaSeleccionada() != null) Ui.seleccionar(cbBod, bodegaSeleccionada());
        JComboBox<String> cbTipo = new JComboBox<>(new String[]{"Salida (merma, daño, pérdida)", "Entrada (sobrante, corrección)"});
        JTextField txtCant = new JTextField();
        JTextArea txtMotivo = new JTextArea(3, 20);
        txtMotivo.setLineWrap(true);
        JLabel lblDisp = new JLabel(" ");
        Runnable disp = () -> {
            Integer p = Ui.idDe(cbProd), b = Ui.idDe(cbBod);
            if (p != null && b != null) lblDisp.setText("Disponible: " + Ui.cantidad(app.inventario().disponible(p, b)));
        };
        cbProd.addActionListener(e -> disp.run());
        cbBod.addActionListener(e -> disp.run());
        disp.run();

        Ui.Formulario f = new Ui.Formulario()
                .campo("Producto", cbProd).campo("Bodega", cbBod).campo("", lblDisp).campo("Tipo", cbTipo)
                .campo("Cantidad", txtCant).campo("Motivo", new JScrollPane(txtMotivo));
        JTextField txtSupUser = new JTextField();
        JPasswordField txtSupPass = new JPasswordField();
        boolean requiereAutorizacion = !usuario.puedeAutorizar();
        if (requiereAutorizacion) {
            JLabel aviso = new JLabel("Requiere autorización de un supervisor:");
            aviso.setForeground(Ui.PELIGRO);
            f.campo("", aviso).campo("Usuario supervisor", txtSupUser).campo("Contraseña", txtSupPass);
        }
        f.setPreferredSize(new Dimension(460, f.getPreferredSize().height));

        while (true) {
            int r = JOptionPane.showConfirmDialog(this, f, "Ajuste de inventario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return;
            try {
                AjusteInventario a = new AjusteInventario();
                a.setIdProducto(Ui.idDe(cbProd));
                a.setIdBodega(Ui.idDe(cbBod));
                BigDecimal cant = Ui.decimal(txtCant, "la cantidad").abs();
                a.setCantidad(cbTipo.getSelectedIndex() == 0 ? cant.negate() : cant);
                a.setMotivo(txtMotivo.getText());
                a.validar();
                Usuario autoriza = requiereAutorizacion
                        ? app.auth().autorizar(txtSupUser.getText(), new String(txtSupPass.getPassword()))
                        : usuario;
                app.inventario().registrarAjuste(a, usuario, autoriza);
                Ui.info(this, "Ajuste #" + a.getId() + " registrado. Autorizó: " + autoriza.getNombre());
                buscar();
                return;
            } catch (Exception ex) {
                Ui.error(this, ex);
            }
        }
    }
}
