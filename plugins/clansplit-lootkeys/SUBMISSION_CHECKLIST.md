# ClanSplit Loot Keys - Plugin Hub Submission Checklist

## ✅ Completed

### 1. Plugin Structure
```
plugins/clansplit-lootkeys/
├── build.gradle.kts         ✅ Created (Kotlin DSL)
├── plugin.properties         ✅ Created
└── src/
    └── main/
        ├── java/
        │   └── net/runelite/client/plugins/clansplit/
        │       ├── ClanSplitConfig.java    ✅ Copied
        │       └── ClanSplitPlugin.java    ✅ Copied
        └── resources/
            └── runelite-plugin.properties  ✅ Copied
```

### 2. Required Files

#### plugin.properties
- ✅ displayName
- ✅ author
- ✅ support (GitHub link)
- ✅ description
- ✅ tags
- ✅ plugins (main class)

#### build.gradle.kts
- ✅ Version number
- ✅ Plugin metadata
- ✅ Dependencies (OkHttp, Gson)
- ✅ Manifest configuration

### 3. Source Code
- ✅ Production version (no debug/test features)
- ✅ Compiles successfully
- ✅ Tested and working in-game

## 📋 Next Steps

### 1. Commit to Your Fork
```bash
cd /Users/salarvk/plugin-hub
git add plugins/clansplit-lootkeys/
git commit -m "Add ClanSplit Loot Keys plugin"
git push origin master
```

### 2. Create Pull Request
1. Go to: https://github.com/sudodevdante/plugin-hub
2. Click "Pull requests" → "New pull request"
3. Set base repository: `runelite/plugin-hub` (base: `master`)
4. Set head repository: `sudodevdante/plugin-hub` (compare: `master`)
5. Create pull request

### 3. Pull Request Description

**Title:**
```
Add ClanSplit Loot Keys plugin
```

**Description:**
```markdown
## Plugin Information

**Name:** ClanSplit Loot Keys  
**Author:** Thissixx  
**Repository:** https://github.com/sudodevdante/clansplit-rl-plugin  
**Website:** https://app.clansplit.com

## What does this plugin do?

ClanSplit is a web-based loot splitting tool for OSRS PvP clans. This plugin automatically detects when you open PvP Loot Keys and sends the values to your active ClanSplit session in real-time.

### Features:
- ✅ Automatic PvP Loot Key detection
- ✅ Real-time sync with ClanSplit sessions
- ✅ Secure token-based authentication
- ✅ Works with clan broadcast messages
- ✅ Privacy-first: only tracks YOUR loot keys

### Use Case:
PvP clans use ClanSplit to:
- Track all member loot in real-time
- Calculate fair splits with configurable taxes
- Manage late joiners automatically
- See who owes what instantly

### Technical Details:
- Uses OkHttp for API calls
- Gson for JSON serialization
- Lombok for logging
- Listens to chat messages for loot key detection
- Sends data over HTTPS to app.clansplit.com

### Testing:
- ✅ Plugin compiles successfully
- ✅ Tested in-game with real loot keys
- ✅ API integration verified
- ✅ No external plugin warnings

## Screenshots

(You can add screenshots here showing the plugin configuration panel and in-game functionality)

## Why is this plugin useful?

For PvP clans that split loot, manually tracking everyone's keys is time-consuming and error-prone. This plugin automates the process, making loot distribution fast, fair, and transparent for all clan members.

## Links

- **GitHub Repository:** https://github.com/sudodevdante/clansplit-rl-plugin
- **Web Application:** https://app.clansplit.com
- **Documentation:** https://app.clansplit.com/how-it-works
```

### 4. Screenshots to Include

Consider adding screenshots showing:
1. **Plugin Configuration Panel** - showing the 3 config options
2. **In-Game Detection** - when a loot key is opened
3. **Web Dashboard** - showing the real-time sync

You can take these screenshots and add them to your plugin repository, then link them in the PR.

## 📝 Important Notes

### Code Review Points
The RuneLite team will review:
- ✅ Code quality
- ✅ No malicious behavior
- ✅ Follows RuneLite guidelines
- ✅ Dependencies are reasonable
- ✅ Plugin does what it claims

### Common Rejection Reasons (We've Avoided):
- ❌ Using external plugins flag - We're submitting to the hub, not using as external
- ❌ Requesting unnecessary permissions - We only use chat messages and HTTP
- ❌ Poor code quality - Our code is clean and well-structured
- ❌ Missing documentation - We have comprehensive README
- ❌ Unclear purpose - Purpose is very clear

### Approval Timeline
- Initial review: 1-7 days
- Feedback/changes: Variable
- Final approval: After all requested changes are made

## ✅ Ready for Submission!

Your plugin is now ready to be submitted to the RuneLite Plugin Hub. Follow the "Next Steps" above to complete the submission process.

Good luck! 🚀

