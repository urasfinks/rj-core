package ru.jamsys.core.template.twix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Parser {

    private Dictionary[] future;
    private Dictionary lastState = Dictionary.ANY;
    private final Map<Dictionary, Dictionary[]> follow = new HashMap<>();
    private StringBuilder sb = new StringBuilder();
    private boolean isTerminal = false;
    private boolean isFinish = false;
    private boolean isParse = false;
    private boolean firstEntry = false;

    public Parser() {
        follow.put(Dictionary.DOLLAR, new Dictionary[]{Dictionary.CURLY_BRACE_OPEN});
        follow.put(Dictionary.CURLY_BRACE_OPEN, new Dictionary[]{Dictionary.ANY, Dictionary.ESCAPE});
        follow.put(Dictionary.ESCAPE, new Dictionary[]{Dictionary.CURLY_BRACE_OPEN, Dictionary.CURLY_BRACE_CLOSE, Dictionary.ESCAPE, Dictionary.DOLLAR});
        follow.put(Dictionary.ANY, new Dictionary[]{Dictionary.ANY, Dictionary.CURLY_BRACE_CLOSE, Dictionary.ESCAPE});
    }

    public void read(String ch) {
        boolean append = true;
        Dictionary curState = Dictionary.parse(ch);
        if (curState == Dictionary.DOLLAR && !isParse && lastState == Dictionary.ESCAPE) {
            String curSb = sb.toString();
            if (!curSb.equals("")) {
                sb = new StringBuilder();
                sb.append(curSb, 0, curSb.length() - 1);
            }
        }
        if (curState == Dictionary.DOLLAR && !isParse && lastState != Dictionary.ESCAPE) {
            isParse = true;
            isTerminal = true;
            setFuture(curState);
            append = false;
            firstEntry = true;
        } else if (isParse) {
            if (firstEntry) {
                sb.append(Dictionary.DOLLAR.getAlias());
                firstEntry = false;
            }
            if (isMust(curState)) {
                if (curState == Dictionary.ESCAPE) {
                    append = false;
                }
                if (lastState == Dictionary.ESCAPE) {
                    if (curState == Dictionary.ESCAPE) {
                        append = true;
                    }
                    setFuture(Dictionary.ANY);
                } else if (curState == Dictionary.CURLY_BRACE_CLOSE) {
                    isFinish = true;
                    isParse = false;
                } else {
                    setFuture(curState);
                }
            } else {
                isTerminal = true;
                isParse = false;
                if (curState == Dictionary.DOLLAR) {
                    append = false;
                    isParse = true;
                    firstEntry = true;
                }
            }
        }
        if (append) {
            sb.append(ch);
        }
        lastState = curState;
    }

    private boolean isMust(Dictionary dictionary) {
        return Arrays.asList(future).contains(dictionary);
    }

    private void setFuture(Dictionary dictionary) {
        future = follow.get(dictionary);
    }

    public boolean isParse() {
        return isParse;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public String flush() {
        isTerminal = false;
        isFinish = false;
        String result = sb.toString();
        sb = new StringBuilder();
        return result;
    }
}
