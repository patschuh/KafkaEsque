package at.esque.kafka.controls;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public enum KeyCodeCombinations {
    COPY(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));

    private KeyCodeCombination combination;

    KeyCodeCombinations(KeyCodeCombination combination) {
        this.combination = combination;
    }

    public KeyCodeCombination getCombination() {
        return combination;
    }
}
