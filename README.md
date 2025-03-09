# SANCF (Simple Command Framework)
**SANCF** is a lightweight and easy-to-use command framework for Minecraft plugin development. It simplifies command handling using annotations — no need for `plugin.yml` and less boilerplate code.

## ⭐ Features
- ✅ Auto-registration with annotations  
- ✅ Clean and professional structure  
- ✅ Async and sync command execution  
- ✅ Subcommands and tab completion  
- ✅ No `plugin.yml` required  

## 🚀 Installation
### Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.SAN-Development</groupId>
    <artifactId>SANCF</artifactId>
    <version>latest</version>
</dependency>
```

### Gradle
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.SAN-Development:SANCF:latest'
}
```

## ⚙️ Setup
### Register Commands
Register commands in your `onEnable()` like this:
```java
@Override
public void onEnable() {
    SANCF.init(this);
    SANCF.register(new TestCommand(this));
}
```

### Example Command
```java
@Command(
        name = "test",
        description = "Test command for SANCF",
        usage = "/test <subcommand>",
        permission = "test.use",
        allowConsole = true,
        async = true
)
public class TestCommand {

  public void execute(CommandContext context) {
    context.sendMessage("&aTest command executed!");
  }

  @SubCommand(name = "message", permission = "test.message")
  public void message(CommandContext context) {
    if (context.getArgs().length > 1) {
      String message = String.join(" ", context.getArgs(), 1, context.getArgs().length);
      context.sendMessage("&eMessage: &f" + message);
    } else {
      context.sendMessage("&cUsage: /test message <text>");
    }
  }

  @TabComplete({"message,async", "Hello,World"})
  public void tabComplete() {}
}
```

## 🏷️ Annotations
| Annotation | Description |
|-----------|-------------|
| `@Command` | Marks the main command class |
| `@SubCommand` | Defines a subcommand |
| `@TabComplete` | Provides tab completion options |

## 🛠️ Commands
| Command | Description |
|---------|-------------|
| `/test` | Executes the main test command |
| `/test message <text>` | Displays the input message |

## 🔒 Permissions
| Permission | Description |
|------------|-------------|
| `test.use` | Allows `/test` usage |
| `test.message` | Allows `/test message` usage |

## 🚧 Status
- SANCF is still in development — some bugs might pop up.  
- Feedback and PRs are welcome! 😎
