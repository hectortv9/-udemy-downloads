package com.hectortv9.udemy;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedCondition;

public class ExpectedConditionsPlus {
    /**
     * An expectation to check if js executable.
     *
     * Useful whenyou know that there should be a Javascript value or something at
     * the stage.
     *
     * @param javaScript used as executable script
     * @return true once javaScript executed without errors
     */
    public static ExpectedCondition<Boolean> javaScriptThrowsNoExceptions(final String javaScript, Object... args) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    ((JavascriptExecutor) driver).executeScript(javaScript, args);
                    return true;
                } catch (WebDriverException e) {
                    return false;
                }
            }

            @Override
            public String toString() {
                return String.format("js %s to be executable", javaScript);
            }
        };
    }
}
