package ru.jamsys.core.flat.template.twix;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
public class Parser {

    private Dictionary[] expectedNextStates;
    private Dictionary lastState = Dictionary.ANY;
    private final Map<Dictionary, Dictionary[]> follow = new HashMap<>();
    private final StringBuilder buffer = new StringBuilder();
    private boolean terminal = false;
    private boolean finished = false;
    private boolean parsing = false;
    private boolean firstEntry = false;

    public Parser() {
        follow.put(Dictionary.DOLLAR, new Dictionary[]{Dictionary.CURLY_BRACE_OPEN});
        follow.put(Dictionary.CURLY_BRACE_OPEN, new Dictionary[]{Dictionary.ANY, Dictionary.ESCAPE});
        follow.put(Dictionary.ESCAPE, new Dictionary[]{Dictionary.CURLY_BRACE_OPEN, Dictionary.CURLY_BRACE_CLOSE, Dictionary.ESCAPE, Dictionary.DOLLAR});
        follow.put(Dictionary.ANY, new Dictionary[]{Dictionary.ANY, Dictionary.CURLY_BRACE_CLOSE, Dictionary.ESCAPE});
    }

    public void read(String inputChar) {
        boolean shouldAppend = true;
        Dictionary currentState = Dictionary.parse(inputChar);

        if (currentState == Dictionary.DOLLAR && !parsing && lastState == Dictionary.ESCAPE) {
            if (!buffer.isEmpty()) {
                buffer.setLength(buffer.length() - 1);
            }
        }

        if (currentState == Dictionary.DOLLAR && !parsing && lastState != Dictionary.ESCAPE) {
            parsing = true;
            terminal = true;
            setExpectedNextStates(currentState);
            firstEntry = true;
            shouldAppend = false;
        } else if (parsing) {
            if (firstEntry) {
                buffer.append(Dictionary.DOLLAR.getAlias());
                firstEntry = false;
            }

            if (isExpected(currentState)) {
                if (currentState == Dictionary.ESCAPE) {
                    shouldAppend = false;
                }

                if (lastState == Dictionary.ESCAPE) {
                    if (currentState == Dictionary.ESCAPE) {
                        shouldAppend = true;
                    }
                    setExpectedNextStates(Dictionary.ANY);
                } else if (currentState == Dictionary.CURLY_BRACE_CLOSE) {
                    finished = true;
                    parsing = false;
                } else {
                    setExpectedNextStates(currentState);
                }
            } else {
                terminal = true;
                parsing = false;

                if (currentState == Dictionary.DOLLAR) {
                    parsing = true;
                    firstEntry = true;
                    shouldAppend = false;
                }
            }
        }

        if (shouldAppend) {
            buffer.append(inputChar);
        }

        lastState = currentState;
    }

    private boolean isExpected(Dictionary state) {
        return expectedNextStates != null && Arrays.asList(expectedNextStates).contains(state);
    }

    private void setExpectedNextStates(Dictionary currentState) {
        expectedNextStates = follow.get(currentState);
    }

    public String flush() {
        terminal = false;
        finished = false;
        String result = buffer.toString();
        buffer.setLength(0);
        return result;
    }

}
