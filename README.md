# 🧢 capes.me Scraper

A powerful desktop application for scraping Minecraft player usernames with specific cape types from [capes.me](https://capes.me).

## 📸 Features

- ✅ Select desired and blocked capes.
- ❌ Automatically filter out non-selected capes.
- 🧑‍💼 Filter users by Hypixel rank (VIP, MVP++, etc).
- 🧾 Real-time logging to an in-app console.
- 🎨 Dark/light theme toggle.
- ⚡ Fast scraping!

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher

### Running the App

- Download the latest version from the releases and double-click it!

<details>
<summary>⚙️ Headless Mode (Command-Line)</summary>

Run the scraper without a GUI using command-line arguments:

```bash
java -jar scraper.jar --headless [options]
```

Available Arguments:

`--desiredCapes - Comma-separated list of cape codes to include (e.g. 2011,Cobalt, etc)`

` --blockedCapes -Comma-separated list of cape codes to exclude or EXCLUDEDESIRED to block all except desired`

`--hypixelRank -Hypixel rank to filter by (e.g. MVP++, VIP, Default)`

Example:
```bash
java -jar capes-scraper.jar --headless --desiredCapes Vanilla,Migrator --blockedCapes EXCLUDEDESIRED --hypixelRank MVP++
```
</details>