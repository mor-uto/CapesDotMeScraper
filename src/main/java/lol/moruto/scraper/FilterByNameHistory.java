package lol.moruto.scraper;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.swing.*;
import java.util.List;

public class FilterByNameHistory {
    private final JTextArea consoleArea;
    private Thread scrapingThread;

    public FilterByNameHistory(JTextArea consoleArea) {
        this.consoleArea = consoleArea;
    }

    public void startProcessing(List<String> igns) {
        scrapingThread = new Thread(() -> {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.setBinary("C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe");
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");

            WebDriver driver = new ChromeDriver(options);

            try {
                for (String ign : igns) {
                    if (ign.trim().isEmpty()) continue;
                    processIgn(driver, ign.trim());
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                logToConsole("Interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            } finally {
                driver.quit();
                logToConsole("Finished processing all IGNs.");
            }
        });
        scrapingThread.start();
    }

    public void waitTillFinished() {
        if (scrapingThread != null) {
            try {
                scrapingThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logToConsole("Waiting thread interrupted: " + e.getMessage());
            }
        }
    }

    private void processIgn(WebDriver driver, String ign) {
        try {
            logToConsole("Searching NameMC for IGN: " + ign);
            driver.get("https://namemc.com/profile/" + ign);
            Thread.sleep(3000);

            logToConsole("Loaded: " + driver.getCurrentUrl());

            WebElement historyCard = driver.findElement(By.xpath("//h2[text()='Name History']/following-sibling::div"));
            List<WebElement> nameRows = historyCard.findElements(By.cssSelector(".row"));

            if (nameRows.isEmpty()) {
                logToConsole(ign + ": No name history found.");
                return;
            }

            logToConsole(ign + " Name history:");
            for (WebElement row : nameRows) {
                String name = "Unknown";
                String date = "";

                try {
                    name = row.findElement(By.cssSelector("div.col-12.col-md-6 span")).getText().trim();
                } catch (Exception ignored) {}

                try {
                    date = row.findElement(By.cssSelector("div.text-right.text-muted")).getText().trim();
                } catch (Exception ignored) {}

                logToConsole("- " + name + (date.isEmpty() ? "" : " (changed on " + date + ")"));
            }
        } catch (Exception e) {
            logToConsole(ign + " Failed: " + e.getMessage());
        }
    }

    private void logToConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(message + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }
}
