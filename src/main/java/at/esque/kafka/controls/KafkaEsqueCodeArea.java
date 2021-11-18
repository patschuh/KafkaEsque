package at.esque.kafka.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.F;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;
import static org.fxmisc.wellbehaved.event.EventPattern.anyOf;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;

public class KafkaEsqueCodeArea extends BorderPane {

    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";

    private static final Pattern SYNTAX_PATTERN = Pattern.compile(
            "(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<FIELDNAME>" + STRING_PATTERN + ")\\s*:"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
    );

    @FXML
    private ToolBar searchTool;
    @FXML
    private TextField searchField;
    @FXML
    private CodeArea codeArea;
    @FXML
    private Label currentSearchHitIndexLabel;
    @FXML
    private Label searchHitsLabel;
    @FXML
    private Button nextButton;
    @FXML
    private Button backButton;
    @FXML
    private FontIcon closeIcon;
    @FXML
    private ToggleButton caseSensitiveButton;

    private ObservableList<IndexRange> searchHits = FXCollections.observableArrayList();
    private IntegerProperty currentSearchHitIndex = new SimpleIntegerProperty(-1);

    public BooleanProperty editable = new SimpleBooleanProperty(true);
    public BooleanProperty searchVisible = new SimpleBooleanProperty(false);
    public LongProperty maxCharactersSyntaxHighlighting = new SimpleLongProperty(Long.MAX_VALUE);

    public KafkaEsqueCodeArea() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "/fxml/controls/kafkaEsqueCodeArea.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.setClassLoader(getClass().getClassLoader());
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    public void initialize() {
        getStylesheets().add(getClass().getResource("/css/kafkaEsqueCodeArea.css").toExternalForm());
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        searchField.textProperty().addListener((observableValue, s, t1) -> {
            highlightAndRememberHits(t1);
        });
        searchHits.addListener((ListChangeListener<? super IndexRange>) change -> {
            searchHitsLabel.setText(searchHits.size() + "");
        });

        editable.bindBidirectional(codeArea.editableProperty());
        searchVisible.addListener((observableValue, aBoolean, t1) -> {
            displaySearchTool(t1, true);
        });

        searchField.sceneProperty().addListener((observableValue, scene, t1) -> {
            if (t1 != null) {
                searchField.requestFocus();
            }
        });

        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(500))
                .subscribe(ignore -> highlightAndRememberHits(searchField.getText()));

        InputMap<KeyEvent> nextHitInputMap = InputMap.consume(
                anyOf(
                        keyPressed(ENTER),
                        keyPressed(DOWN)
                )
        ).ifConsumed(keyEvent -> nextSearchHitClick());

        InputMap<KeyEvent> previousHitInputMap = InputMap.consume(
                anyOf(
                        keyPressed(ENTER, SHIFT_DOWN),
                        keyPressed(UP)
                )
        ).ifConsumed(keyEvent -> previousSearchHitClick());

        InputMap<KeyEvent> searchToggleInputMap = InputMap.consume(
                anyOf(
                        keyPressed(F, SHORTCUT_DOWN)
                )
        ).ifConsumed(keyEvent -> toggleSearchTool());
        Nodes.addInputMap(searchField, nextHitInputMap);
        Nodes.addInputMap(searchField, previousHitInputMap);
        Nodes.addInputMap(this, searchToggleInputMap);
        backButton.setOnAction(actionEvent -> previousSearchHitClick());
        nextButton.setOnAction(actionEvent -> nextSearchHitClick());
        closeIcon.onMouseClickedProperty().set(mouseEvent -> toggleSearchTool());
        caseSensitiveButton.onActionProperty().set(actionEvent -> highlightAndRememberHits(searchField.getText()));
        displaySearchTool(searchVisible.get(), false);
        currentSearchHitIndexLabel.textProperty().bind(Bindings.createStringBinding(() -> currentSearchHitIndex.get() + 1 + "", currentSearchHitIndex));

    }

    private void displaySearchTool(Boolean t1, boolean requestFocus) {
        if (t1) {
            setTop(searchTool);
            if (requestFocus) {
                searchField.requestFocus();
            }
        } else {
            setTop(null);
            searchField.setText(null);
            if (requestFocus) {
                codeArea.requestFocus();
            }
        }
    }

    private void toggleSearchTool() {
        searchVisible.set(!searchVisible.get());
    }

    @FXML
    private void nextSearchHitClick() {
        if (searchHits.isEmpty()) {
            return;
        }
        currentSearchHitIndex.set(currentSearchHitIndex.get()+1);
        if (currentSearchHitIndex.get() >= searchHits.size()) {
            currentSearchHitIndex.set(0);
        }
        IndexRange indexRange = searchHits.get(currentSearchHitIndex.get());
        selectAndShowRange(indexRange);
    }

    @FXML
    private void previousSearchHitClick() {
        if (searchHits.isEmpty()) {
            return;
        }
        currentSearchHitIndex.set(currentSearchHitIndex.get()-1);
        if (currentSearchHitIndex.get() <= -1) {
            currentSearchHitIndex.set(searchHits.size() - 1);
        }
        IndexRange indexRange = searchHits.get(currentSearchHitIndex.get());
        selectAndShowRange(indexRange);
    }

    private void selectAndShowRange(IndexRange indexRange) {
        boolean backward = indexRange.getStart() < codeArea.getCaretPosition();
        if (backward) {
            codeArea.selectRange(indexRange.getEnd(), indexRange.getStart());
        } else {
            codeArea.selectRange(indexRange.getStart(), indexRange.getEnd());
        }
        codeArea.requestFollowCaret();
    }


    private void highlightAndRememberHits(String regex) {
        clearSearchHits();
        if(!StringUtils.isEmpty(codeArea.getText())) {
            String text = codeArea.getText();
            int lastKwEnd = 0;
            StyleSpansBuilder<Collection<String>> syntaxSpanBuilder = new StyleSpansBuilder<>();
            boolean syntaxMatches = false;
            if(text.length() <= maxCharactersSyntaxHighlighting.get()) {
                Matcher syntaxMatcher = SYNTAX_PATTERN.matcher(text);
                while (syntaxMatcher.find()) {
                    syntaxMatches = true;
                    String styleClass =
                            syntaxMatcher.group("PAREN") != null ? "paren" :
                                    syntaxMatcher.group("BRACE") != null ? "brace" :
                                            syntaxMatcher.group("BRACKET") != null ? "bracket" :
                                                    syntaxMatcher.group("SEMICOLON") != null ? "semicolon" :
                                                            syntaxMatcher.group("FIELDNAME") != null ? "fieldname" :
                                                                    syntaxMatcher.group("STRING") != null ? "string" :
                                                                            null; /* never happens */
                    assert styleClass != null;
                    syntaxSpanBuilder.add(Collections.emptyList(), syntaxMatcher.start() - lastKwEnd);
                    syntaxSpanBuilder.add(Collections.singleton(styleClass), syntaxMatcher.end() - syntaxMatcher.start());
                    lastKwEnd = syntaxMatcher.end();
                }
            }
            StyleSpans<Collection<String>> syntaxSpans = null;
            if(syntaxMatches){
                syntaxSpans = syntaxSpanBuilder.create();
            }


            StyleSpans<Collection<String>> searchSpans = null;
            if (!StringUtils.isEmpty(regex)) {
                Pattern searchPattern = Pattern.compile(regex, (caseSensitiveButton.isSelected() ? 0 : Pattern.CASE_INSENSITIVE) + Pattern.MULTILINE);
                Matcher searchMatcher = searchPattern.matcher(codeArea.getText());

                StyleSpansBuilder<Collection<String>> searchSpanBuilder = new StyleSpansBuilder<>();
                lastKwEnd = 0;
                while (searchMatcher.find()) {
                    searchSpanBuilder.add(Collections.emptyList(), searchMatcher.start() - lastKwEnd);
                    searchSpanBuilder.add(Collections.singleton("searchHit"), searchMatcher.end() - searchMatcher.start());
                    lastKwEnd = searchMatcher.end();
                    searchHits.add(new IndexRange(searchMatcher.start(), searchMatcher.end()));
                }
                searchSpanBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
                searchSpans = searchSpanBuilder.create();
            }

            if (searchSpans == null && syntaxSpans != null) {
                codeArea.setStyleSpans(0, syntaxSpans);
                return;
            }
            if(searchSpans != null && syntaxSpans != null) {
                StyleSpans<Collection<String>> combinedHighlighting = syntaxSpans.overlay(searchSpans, (strings, strings2) -> {
                    ArrayList<String> combined = new ArrayList<>(strings);
                    combined.addAll(strings2);
                    return combined;
                });
                codeArea.setStyleSpans(0, combinedHighlighting);
            }
            if(searchSpans != null && syntaxSpans == null){
                codeArea.setStyleSpans(0, searchSpans);
            }

        }
    }


    private void clearSearchHits() {
        currentSearchHitIndex.set(-1);
        searchHits.clear();
    }

    public ObservableValue<String> textProperty() {
        return codeArea.textProperty();
    }

    public String getText() {
        return codeArea.getText();
    }

    public void setText(String text) {
        codeArea.selectAll();
        codeArea.deleteText(codeArea.getSelection());
        codeArea.appendText(text != null ? text : "");
        codeArea.moveTo(0);
        codeArea.requestFollowCaret();
    }

    public boolean isEditable() {
        return editable.get();
    }

    public BooleanProperty editableProperty() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable.set(editable);
    }
}
