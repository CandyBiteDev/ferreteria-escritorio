package com.umg.ferreteria.vista;

import com.umg.ferreteria.excepcion.ValidacionException;
import com.umg.ferreteria.modelo.Bodega;
import com.umg.ferreteria.modelo.ResultadoReporte;
import com.umg.ferreteria.modelo.Rol;
import com.umg.ferreteria.modelo.Usuario;
import com.umg.ferreteria.servicio.TipoReporte;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

public class PanelReportes extends JPanel {

    private final AppEscritorio app;
    private final JComboBox<TipoReporte> cbTipo;
    private final JTextField txtDesde = new JTextField(LocalDate.now().minusDays(30).toString(), 9);
    private final JTextField txtHasta = new JTextField(LocalDate.now().toString(), 9);
    private final JComboBox<Object> cbBodega = new JComboBox<>();
    private final JSpinner spDias = new JSpinner(new SpinnerNumberModel(30, 1, 3650, 5));
    private final JLabel lblTitulo = new JLabel(" ");
    private final JTable tabla = Ui.tabla(Ui.modelo());

    public PanelReportes(AppEscritorio app, Usuario u) {
        super(new BorderLayout(8, 8));
        this.app = app;
        setBorder(new EmptyBorder(12, 12, 12, 12));

        List<TipoReporte> tipos = Arrays.stream(TipoReporte.values())
                .filter(t -> t != TipoReporte.AUDITORIA || u.tieneRol(Rol.ADMIN, Rol.SUPERVISOR))
                .filter(t -> t != TipoReporte.RENTABILIDAD_CATEGORIA || u.tieneRol(Rol.ADMIN, Rol.SUPERVISOR))
                .toList();
        cbTipo = new JComboBox<>(tipos.toArray(new TipoReporte[0]));
        cbBodega.addItem("Todas");
        app.catalogo().listar(app.catalogo().bodegas()).forEach(cbBodega::addItem);

        JButton btnGenerar = Ui.boton("Generar", Ui.PRIMARIO);
        btnGenerar.addActionListener(e -> Ui.accion(this, this::generar));
        JButton btnExportar = Ui.boton("Exportar CSV");
        btnExportar.addActionListener(e -> Ui.exportarCsv(this, tabla, lblTitulo.getText()));
        cbTipo.addActionListener(e -> habilitarParametros());

        JPanel filtros = WrapLayout.barra();
        filtros.add(new JLabel("Reporte:"));
        filtros.add(cbTipo);
        filtros.add(btnGenerar);
        filtros.add(btnExportar);
        filtros.add(new JLabel("Desde (aaaa-mm-dd):"));
        filtros.add(txtDesde);
        filtros.add(new JLabel("Hasta:"));
        filtros.add(txtHasta);
        filtros.add(new JLabel("Bodega:"));
        filtros.add(cbBodega);
        filtros.add(new JLabel("Días sin mov.:"));
        filtros.add(spDias);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(Ui.encabezado("Reportes", "Visualizables en pantalla y exportables a CSV (Excel)"), BorderLayout.NORTH);
        norte.add(filtros, BorderLayout.CENTER);
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 15f));
        lblTitulo.setBorder(new EmptyBorder(8, 0, 0, 0));
        norte.add(lblTitulo, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        habilitarParametros();
    }

    private void habilitarParametros() {
        TipoReporte t = (TipoReporte) cbTipo.getSelectedItem();
        if (t == null) return;
        txtDesde.setEnabled(t.usaFechas());
        txtHasta.setEnabled(t.usaFechas());
        cbBodega.setEnabled(t.usaBodega());
        spDias.setEnabled(t == TipoReporte.SIN_MOVIMIENTO);
    }

    private LocalDate fecha(JTextField f) {
        try {
            return LocalDate.parse(f.getText().trim());
        } catch (DateTimeParseException e) {
            throw new ValidacionException("Fecha inválida '" + f.getText() + "'. Use el formato aaaa-mm-dd.");
        }
    }

    private void generar() {
        TipoReporte t = (TipoReporte) cbTipo.getSelectedItem();
        Integer bodega = t.usaBodega() && cbBodega.getSelectedItem() instanceof Bodega b ? b.getId() : null;
        ResultadoReporte r = app.reportes().generar(t, fecha(txtDesde), fecha(txtHasta), bodega, (Integer) spDias.getValue());
        DefaultTableModel m = Ui.modelo(r.getColumnas().toArray(new String[0]));
        for (List<Object> fila : r.getFilas()) m.addRow(fila.toArray());
        tabla.setModel(m);
        tabla.setAutoCreateRowSorter(true);
        lblTitulo.setText(r.getTitulo() + " — " + r.getFilas().size() + " filas");
    }
}
