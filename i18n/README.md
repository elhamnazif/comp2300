# i18n Module

This module is dedicated to managing internationalisation (i18n) and localised resources.

## Purpose

The `i18n` module serves as a central repository for all string resources, ensuring consistent localisation across all platforms (Android, iOS, JVM) in this Kotlin Multiplatform (KMP) project.

## Technology

- **Compose Multiplatform Resources**: Used to manage and access resources across platforms.
- **Strings**: Defined in XML format within the `commonMain` source set.

## Structure

- `src/commonMain/composeResources/values/strings.xml`: The primary location for default (English) string resources.
- Other `values-<locale>/strings.xml` directories can be added for additional languages.

## Adding New Strings

1. Open `i18n/src/commonMain/composeResources/values/strings.xml`.
2. Add a new `<string name="your_string_key">Your string value</string>` entry.
3. Use the generated `Res.string.your_string_key` in your Composable functions or shared code.

## Handling Strings with Arguments or Plurals

### Strings with Arguments
Use the `%<number>` format to place arguments within the string and include a `$d` or `$s` suffix to indicate variable placeholders. For example:

```xml
<string name="str_template">Hello, %2$s! You have %1$d new messages.</string>
```

Access the string with arguments:

```kotlin
Text(stringResource(Res.string.str_template, 100, "User_name"))
```

### Plurals
Define plurals using the `<plurals>` element to handle grammatical agreement for different quantities:

```xml
<plurals name="new_message">
    <item quantity="one">%1$d new message</item>
    <item quantity="other">%1$d new messages</item>
</plurals>
```

Access plurals in your code:

```kotlin
Text(pluralStringResource(Res.plurals.new_message, 1, 1))
```

---

## Resources
- [JetBrain's String Resources Guide](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html#strings)