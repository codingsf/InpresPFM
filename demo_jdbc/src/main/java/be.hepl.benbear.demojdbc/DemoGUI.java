package be.hepl.benbear.demojdbc;

import be.hepl.benbear.commons.db.Database;
import be.hepl.benbear.commons.db.Table;
import be.hepl.benbear.commons.streams.UncheckedLambda;
import be.hepl.benbear.trafficdb.*;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class DemoGUI {
    private JTable tableData;
    private JComboBox<Class<?>> comboBoxTables;
    private JButton buttonOk;
    private JButton buttonInsert;
    private JButton buttonDelete;
    private JButton buttonUpdate;
    private JPanel mainPanel;

    private Database database;
    private Table<?> currentTable;

    public DemoGUI() {
        database = new Database();
        List<Class<?>> listClass = Arrays.asList(
            Company.class,
            be.hepl.benbear.trafficdb.Container.class,
            Destination.class,
            Movement.class,
            Parc.class,
            Reservation.class,
            Transporter.class,
            User.class
        );
        listClass.forEach(database::registerClass);

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch(ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        database.connect("jdbc:oracle:thin:@178.32.41.4:8080:xe", "dbtraffic", "bleh");

        comboBoxTables.setModel(new DefaultComboBoxModel<>(listClass.toArray(new Class<?>[listClass.size()])));

        buttonOk.addActionListener(e -> {
            currentTable = database.table((Class<?>) comboBoxTables.getSelectedItem());
            try {
                updateSelection();
            } catch (ExecutionException | InterruptedException e1) {
                e1.printStackTrace();
            }
        });
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame frame = new JFrame("DemoGUI");
        DemoGUI demoGUI = new DemoGUI();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    demoGUI.database.close();
                } catch(Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        frame.setContentPane(demoGUI.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void updateSelection() throws ExecutionException, InterruptedException {
        List<Field> listField = Arrays.stream(currentTable.getTableClass().getDeclaredFields())
            .filter(f -> !f.isSynthetic())
            .filter(f -> !Modifier.isTransient(f.getModifiers()))
            .peek(f -> f.setAccessible(true))
            .collect(Collectors.toList());

        List<String> columns = listField.stream()
            .map(Field::getName)
            .collect(Collectors.toList());

        Object[][] data = currentTable.find().get()
            .map(UncheckedLambda.function(o -> {
                Object[] values = new Object[listField.size()];
                int i = 0;
                for(Field f : listField) {
                    values[i++] = f.get(o);
                }
                return values;
            }, ex -> {
                throw new RuntimeException(ex);
            }))
            .toArray(Object[][]::new);

        tableData.setModel(new DefaultTableModel(data, columns.toArray()));
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        final JScrollPane scrollPane1 = new JScrollPane();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane1, gbc);
        tableData = new JTable();
        scrollPane1.setViewportView(tableData);
        comboBoxTables = new JComboBox();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(comboBoxTables, gbc);
        buttonInsert = new JButton();
        buttonInsert.setText("Insert");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(buttonInsert, gbc);
        buttonDelete = new JButton();
        buttonDelete.setText("Delete");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(buttonDelete, gbc);
        buttonUpdate = new JButton();
        buttonUpdate.setText("Update");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(buttonUpdate, gbc);
        buttonOk = new JButton();
        buttonOk.setText("Ok");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(buttonOk, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
