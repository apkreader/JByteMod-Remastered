
# JByteMod Remastered

[![Build Status](https://ci.mdma.dev/api/badges/apkreader/JByteMod-Remastered/status.svg)](https://ci.mdma.dev/apkreader/JByteMod-Remastered)
![GitHub Release](https://img.shields.io/github/v/release/apkreader/JByteMod-Remastered)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/681e07293b4c491fae53c3be6d8469fe)](https://app.codacy.com/gh/apkreader/JByteMod-Remastered/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/apkreader/JByteMod-Remastered)
![GitHub Issues or Pull Requests](https://img.shields.io/github/issues-pr/apkreader/JByteMod-Remastered)

JByteMod Remastered is an enhanced Java bytecode editor that offers a wide array of features for decompiling, editing, and recompiling Java class files. This version includes improvements over the original JByteMod, making it a versatile tool for Java developers and enthusiasts.

## Features
-   **Android APK Support** (Decompile only at the moment)
-   **Advanced Bytecode Editing**: Intuitive interface for directly modifying Java bytecode.
-   **Running JVM Attachment**: Attach to a local JVM to inspect or dump loaded classes and apply compatible bytecode changes at runtime.
-   **Decompiler Integration**: Use CFR, Vineflower, Procyon, JD-Core, Koffee, and ASMifier. Packaged builds compile the latest CFR master source instead of using the old published release.
-   **Graphical Bytecode Viewer**: Visualize bytecode in a graphical format for easier comprehension.
-   **Control Flow Visualization**: Generate and view control flow diagrams of methods to understand execution flow better.
-   **Drag and Drop Functionality**: Easily drag and drop `.jar`, `.apk`, and `.class` files onto the window for quick access.
-   **Search and Replace**: Effortlessly find and replace bytecode instructions.
-   **Constant Pool Editor**: Manage and edit constant pool entries within class files.
-   **Plugin System**: Extend functionality with custom plugins tailored to specific needs.
-   **Cross-Platform Compatibility**: Compatible with Windows, macOS, and Linux operating systems.

## Installation

### Prerequisites
-   A full Java Development Kit (JDK) 21 or newer. A JRE alone is not sufficient for JVM attachment.
-   JDK 8 builds are no longer provided or supported.

### Download

1.  Obtain the latest release of JByteMod Remastered from the [releases page](https://github.com/apkreader/JByteMod-Remastered/releases).

### Usage

1. Open a terminal or command prompt.

2. Navigate to the directory containing `JByteMod-Remastered.jar`.

3. Launch JByteMod Remastered using the following command:
    ```sh 
    java -jar JByteMod-Remastered.jar
    ```

4. Alternatively, drag and drop `.jar`, `.apk`, or `.class` files directly onto the JByteMod Remastered window to open them for editing.

### Attaching to a running JVM

1. Run JByteMod with a full JDK 21 or newer.
2. Open `Utilities` > `Attach to process` and select a local JVM.
3. Browse and edit the loaded classes in the current JByteMod window.
4. Use `File` > `Apply changes` to redefine the modified classes in the target JVM.

Class redefinition is limited by the target JVM. Method-body and constant changes are generally supported, while structural changes such as adding or removing fields, methods, superclasses, or interfaces are normally rejected.

### Building from source

Building requires JDK 21 or newer, Maven, Git, and an internet connection:

```sh
mvn package
```

The package build downloads the current CFR `master` branch, records its commit in the displayed CFR version, compiles it from source, and includes it in the final JByteMod jar.


### Getting Started

-   **Opening Files**: Use the drag and drop feature or navigate through `File` > `Open` to load `.jar`, `.apk`, or `.class` files.
-   **Editing Bytecode**: Select a method from the left panel to view and modify its bytecode.
-   **Decompiling**: Switch to the `Decompiler` tab to view and edit decompiled Java source code.
-   **Generating Control Flow Diagrams**: In the `Analysis` tab, select a method to generate and view its control flow diagram, you can also save it by clicking `Save`.
-   **Saving Changes**: After making edits, save your changes via `File` > `Save`.

### Contributing

Contributions to JByteMod Remastered are encouraged! Follow these steps to contribute:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/your-feature`).
3.  Make your changes and commit them (`git commit -am 'Add some feature'`).
4.  Push to the branch (`git push origin feature/your-feature`).
5.  Create a new Pull Request.

### Issues

Report any bugs or suggest improvements on the [issue tracker](https://github.com/apkreader/JByteMod-Remastered/issues).

## License

JByteMod Remastered is licensed under the MIT License. See the LICENSE file for details.

## Acknowledgements

-   Gratitude to all contributors and community members who support the development of JByteMod Remastered.
