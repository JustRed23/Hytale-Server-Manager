# Hytale Server Manager

A small Java command-line utility for managing Hytale servers. It provides an easy way to keep your Hytale server up to date and manages versions between patchlines.

### Features
- Authenticates with Hytale's official servers to download the latest server versions.
- Automatically updates your server to the latest version within a specified patchline.
- Simple command-line interface for ease of use.

### Usage
To use the Hytale Server Manager, run the following command:
```bash
java -jar Hytale-Server-Manager-<version>-all.jar [options]
```
**NOTE: Replace `<version>` with the actual version number of the JAR file.**

#### Options
| Option              | Arguments     | Description                                                         | Default   |
|---------------------|---------------|---------------------------------------------------------------------|-----------|
| `--help`            | —             | Display help information about the command-line options.            | false     |
| `--printServerHelp` | —             | Print help information specific to the Hytale server.               | false     |
| `--patchline`       | `<patchline>` | Specify the patchline to use (e.g., `release`, `pre-release`).      | `release` |
| `--skipUpdate`      | —             | Skip the update check and proceed with the existing server version. | false     |

#### Examples

```bash
# Start the server with default patchline and update
java -jar Hytale-Server-Manager-1.0-all.jar

# Start the server using the pre-release patchline
java -jar Hytale-Server-Manager-1.0-all.jar --patchline pre-release

# Start the server without checking for updates
java -jar Hytale-Server-Manager-1.0-all.jar --skipUpdate
```

### Prerequisites
- JDK 25 or higher
  - This is needed because Hytale uses Java 25.

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/JustRed23/Hytale-Server-Manager
   ```
2. Run the Gradle build command:
   ```bash
   # On Windows, use gradlew.bat instead
   ./gradlew build
   ```

### Creating an executable JAR
This project uses the Gradle Shadow plugin, so to create an executable JAR file, run:
```bash
# On Windows, use gradlew.bat instead
./gradlew shadowJar
```
The resulting JAR file will be located in the `build/libs` directory.

## License

Hytale Server Manager is licensed under the MIT License.  
This software is unofficial and is not affiliated with Hypixel Studios or Riot Games in any way.