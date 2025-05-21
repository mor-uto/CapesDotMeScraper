package lol.moruto.scraper;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;

public class FilterByNameHistory {

    public void startProcessing(List<String> igns) {
        new Thread(() -> {
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
                Main.log("Interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            } finally {
                driver.quit();
                Main.log("Finished processing all IGNs.");
            }
        }).start();
    }

    private void processIgn(WebDriver driver, String ign) {
        try {
            Main.log("Searching NameMC for IGN: " + ign);
            driver.get("https://namemc.com/profile/" + ign);
            Thread.sleep(3000);

            Main.log("Loaded: " + driver.getCurrentUrl());

            WebElement historyCard = driver.findElement(By.xpath("//h2[text()='Name History']/following-sibling::div"));
            List<WebElement> nameRows = historyCard.findElements(By.cssSelector(".row"));

            if (nameRows.isEmpty()) {
                Main.log(ign + ": No name history found.");
                return;
            }

            Main.log(ign + " Name history:");
            for (WebElement row : nameRows) {
                String name = "Unknown";
                String date = "";

                try {
                    name = row.findElement(By.cssSelector("div.col-12.col-md-6 span")).getText().trim();
                } catch (Exception ignored) {}

                try {
                    date = row.findElement(By.cssSelector("div.text-right.text-muted")).getText().trim();
                } catch (Exception ignored) {}

                Main.log("- " + name + (date.isEmpty() ? "" : " (changed on " + date + ")"));
            }
        } catch (Exception e) {
            Main.log(ign + " Failed: " + e.getMessage());
        }
    }
}
