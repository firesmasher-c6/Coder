# CoderJSLoader 🚀

An ultra-lightweight (~1.37MB) native JavaScript (.js) script execution addon for the **Coder v1.3.6 or newer** Minecraft plugin. 

Optimized specifically for modern **Paper 1.21+** servers running **OpenJDK 21 to 25**. It uses advanced Java reflection hooks to seamlessly unmask and support the `.js` extension inside Coder's core `/coder run` command pipeline without triggering any Mojang remapper bugs or Error T10 security blocks.

## 🌟 Key Features
- **Native `.js` Extension Parsing**: No ugly file masking or workarounds. Natively type `/coder run script.js`.
- **Pre-Baked API Variable Injections**: Automatically maps `bukkit` (Server instance), `api` (Coder API handle), and `sender` (Command Executor) straight into your JavaScript scope.
- **Pure Performance Engine**: Powered by a shaded Mozilla Rhino JSR-223 runtime environment that is highly optimized and exceptionally gentle on your host's RAM.
- **Remapper Protection**: Built completely using the modern `paper-plugin.yml` layout to completely dodge Mojang's plugin remapper bytecode corruption.

## 🛠️ Installation Layout
1. Drop the compiled `CoderJSLoader-1.0.0.jar` directly into your server's primary `/plugins/` folder.
2. Restart your server.
3. Verify that the startup console states: `[JS Reflection] SUCCESS! Surgically modified Coder's executor to support .js extension.`

## 📖 Scripting Quick Start
Place your script files inside your unified Coder folder: `/plugins/Coder/scripts/` ending with a `.js` extension.

**Example Script (`hello.js`):**
```javascript
// Use raw section symbols for clean color parameters
var prefix = "§e§l[JS Engine] §f";

// Broadcast to your game chat natively using the pre-baked 'bukkit' handle
bukkit.broadcastMessage(prefix + "JavaScript is flying natively on this node! 🚀");

// Log text directly straight to your Pterodactyl console log array
api.log("[JS Addon] Script executed successfully.");
```

**To Execute In-Game or via Terminal Console:**
```text
/coder run hello.js
```

