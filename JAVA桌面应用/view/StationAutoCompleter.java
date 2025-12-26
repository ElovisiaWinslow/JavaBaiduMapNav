package view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;

public class StationAutoCompleter {
    private final JTextField textField;
    private final List<String> allItems;
    private final JPopupMenu popupMenu;
    private final JList<String> suggestionList;
    private boolean isAdjusting = false;

    public StationAutoCompleter(JTextField textField, List<String> items) {
        this.textField = textField;
        this.allItems = items;
        this.popupMenu = new JPopupMenu();
        this.suggestionList = new JList<>();
        
        initUI();
    }

    private void initUI() {
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFont(textField.getFont());
        
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) selectItem();
            }
        });

        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(null);
        popupMenu.add(scrollPane);
        popupMenu.setFocusable(false);

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateList(); }
            @Override public void removeUpdate(DocumentEvent e) { updateList(); }
            @Override public void changedUpdate(DocumentEvent e) { updateList(); }
        });

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int size = suggestionList.getModel().getSize();
                    if (size > 0) {
                        suggestionList.setSelectedIndex((suggestionList.getSelectedIndex() + 1) % size);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int size = suggestionList.getModel().getSize();
                    if (size > 0) {
                        int idx = suggestionList.getSelectedIndex() - 1;
                        if (idx < 0) idx = size - 1;
                        suggestionList.setSelectedIndex(idx);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    selectItem();
                }
            }
        });
    }

    private void updateList() {
        if (isAdjusting) return;
        String input = textField.getText().trim();
        if (input.isEmpty()) {
            popupMenu.setVisible(false);
            return;
        }

        List<String> filtered = allItems.stream()
                .filter(item -> item.contains(input))
                .limit(10)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            popupMenu.setVisible(false);
        } else {
            suggestionList.setListData(filtered.toArray(new String[0]));
            suggestionList.setSelectedIndex(0);
            popupMenu.setPopupSize(textField.getWidth(), 150);
            popupMenu.show(textField, 0, textField.getHeight());
            textField.requestFocus();
        }
    }

    private void selectItem() {
        String selected = suggestionList.getSelectedValue();
        if (selected != null) {
            isAdjusting = true;
            textField.setText(selected);
            isAdjusting = false;
            popupMenu.setVisible(false);
        }
    }
}