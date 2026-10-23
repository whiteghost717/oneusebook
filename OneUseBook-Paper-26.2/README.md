# OneUseBook for Paper 26.2

This is a **source project**, not a precompiled `.jar`. It is designed for Paper 26.2 and Java 25.

## What it does
- An OP writes commands in a writable book, one command per line, then signs it with their own Minecraft account.
- The OP holds that signed book in their main hand and runs `/oneusebook bind`.
- A player right-clicks the registered book to execute the stored commands as the server console.
- The book is consumed, and its unique ID is recorded as used. Duplicated copies with the same ID cannot run the commands again.
- Commands support `{player}` and `{uuid}` placeholders.
- The command is marked used before execution, so it cannot be clicked repeatedly to rerun it.

## Example book
Page 1:
```
give {player} diamond 10
effect give {player} speed 30 1
```
Sign the book as your own Minecraft account. Hold the signed book and run:
```
/oneusebook bind
```
A player who clicks it gets the commands run by console, and the book is consumed.

## Build into a .jar
Paper 26.2 requires Java 25. On a computer with Java 25 and Gradle installed:
1. Extract this folder.
2. Open a terminal in this folder.
3. Run `gradle build`.
4. The plugin jar will be at `build/libs/OneUseBook-1.0.0.jar`.
5. Upload that JAR to the server's `plugins` folder and restart.

Paper's API dependency is downloaded from the official Paper Maven repository, so the build computer needs internet access.

## Install
1. Back up your server.
2. Upload `OneUseBook-1.0.0.jar` into `plugins/`.
3. Restart Paper.
4. In game, OP writes and signs a book containing console commands, one command per line.
5. Hold the signed book and run `/oneusebook bind`.
6. Give the authorized book to the intended player.

## Safety
This plugin intentionally gives the holder of a registered book the ability to trigger its saved commands as console. Only bind books you wrote and checked carefully. Never bind commands that grant OP, change permissions, delete worlds/files, or otherwise give away server control unless that is genuinely intended.

The used-book list is stored in `plugins/OneUseBook/data.yml`. Do not delete that file, or used IDs may no longer be remembered.

## Build without installing Java/Gradle locally
The ZIP includes a GitHub Actions workflow at `.github/workflows/build.yml`.
1. Create a new GitHub repository and upload/extract all files from this project into it (including the `.github` folder).
2. Open the repository's **Actions** tab and enable Actions if prompted.
3. Run **Build OneUseBook** using **Run workflow**, or push a commit to `main`.
4. Open the completed workflow run and download the artifact named `OneUseBook-Paper-26.2`.
5. Inside the downloaded artifact is `OneUseBook-1.0.0.jar`.
