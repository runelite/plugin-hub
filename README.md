![](https://runelite.net/img/logo.png)
# plugin-hub [![Discord](https://img.shields.io/discord/301497432909414422.svg)](https://discord.gg/mePCs8U)

This repository contains markers for [RuneLite](https://github.com/runelite/runelite)
plugins that are not supported by the RuneLite Developers. The plugins are
provided "as is"; we make no guarantees about any plugin in this repo.


## Creating new plugins
There are two methods to create an external plugin, you can either:
 - Use [this](https://github.com/runelite/example-plugin/) plugin template.

 - Clone this repository and run the `create_new_plugin.py` script. This requires you to have `python3` installed


### Using the template repository
 1. Generate your own repository with [this](https://github.com/runelite/example-plugin/generate) link. You have to be logged in to GitHub.
 
 2. Name your repository something appropriate, in my case I will name it `helmet-check` with the description `You should always wear a helmet.` **Make sure that your repository is set to public**.

 3. Add a `README` to your repository, this is a short description of what your plugin does. In the bottom right you will find an *Add README*-button. 
 Click it and add a description of your plugin, it will be autofilled with the description you wrote in the previous step. This is the first thing a user will see when it visits your plugin repository.

 4. In the top right, you will see a *Clone or download*-button. Click on it and copy the link.

 5. Open IntelliJ and choose *Get from Version Control*. Paste the link you just copied in the URL field and where you want to save it in the second field.

 6. In order to make sure everything works correctly, try to start the client with your external plugin enabled by running the test.

 ![run-test](https://i.imgur.com/tKSQH5e.png)

 7. Use the refactor tool to rename the package to what you want your plugin to be. Rightclick the package in the sidebar and choose *Refactor > Rename*. 

  Do the same for `ExamplePlugin`, `ExampleConfig` and `ExamplePluginTest`.
 
  Right-click the src folder in the sidebar and choose *Replace in path*, enter `example` in the upper field and `<yourpluginname>`, no spaces. in the lower one. Then press replace all. Go to your plugin file and set it's name in the `PluginDescriptor`, this can have spaces.

 9. Open the `runelite-plugin.properties` file and add info to each row. 
 ```
displayName=Helmet check
author=dekvall
support=https://github.com/dekvall/helmet-check
description=Alerts you when you have nothing equipped in your head slot
tags=hint,gear,head
plugins=com.example.HelmetCheckPlugin
 ```
 `support` is the URL you want players to use to leave feedback for your plugin; you can just use your repository for that. `tags` will make it easier to find your plugin when searching for related words.

 10. Optionally, you can add an icon to be displayed alongside with your plugin. Place a file with the name `icon.png` no larger than 48x72 px at the root of the repository.

 11. When you have your plugin working. Commit your changes and push them to your repository. 


### Using the script
 1. Navigate to the folder in which you cloned the `plugin-hub` repository.

 2. Run the script with:
 ```
 python3 create_new_plugin.py
 ```
 It will ask you a series of questions, and then generate a folder with the name of your plugin.

 3. Move the generated folder to it's own git repository and open the `build.gradle` file in IntelliJ.

 4. In order to make sure everything works correctly, try to start the client with your external plugin enabled by running the test.

 ![run-test](https://i.imgur.com/tKSQH5e.png)

 5. Edit `runelite-plugin.properties` with a support link and tags. `support` is the URL you want players to use to leave feedback for your plugin; you can just use your repository for that. `tags` will make it easier to find your plugin when searching for related words.

 6. Optionally, you can add an icon to be displayed alongside with your plugin. Place a file with the name `icon.png` no larger than 48x72 px at the root of the repository.

 7. When you have your plugin working. Commit your changes and push them to your repository.

### Licensing your repository
 1. Go to your repository on GitHub and select *Insights* from the top bar. Then choose *Community* in the list to the left.
 2. In the license section of the list press *Add*.
 3. Select `BSD 2-Clause "Simplified" License` from the list to the left. Fill in your details and press *Review and submit*.
 4. Commit your changes by clicking *Create new file* in the bottom of the page. Make sure you check the button to directly commit to the master branch.

## Submitting a plugin
 1. Fork the [plugin-hub repository](https://github.com/runelite/plugin-hub).
 2. Create a new branch for your plugin. 
 3. Create a new file in the `plugin-hub/plugins` directory with the fields:
 ```
repository=
commit=
 ```
 4. To get the repository url, click the *Clone or download*-button choose *Use HTTPS*. Paste the url in in the `repository=` field.

 5. To get the commit hash, go to your plugin repository on GitHub and click on commits. Choose the latest one and copy the full 40-character hash. It can be seen in the top right after selecting a commit. Paste this into the `commit=` field of the file. 
 Your file should now look something like this:
 ```
repository=https://github.com/dekvall/helmet-check.git
commit=9db374fc205c5aae1f99bd5fd127266076f40ec8
 ```
 6. This is the only change you need to make, so commit your changes and push them to your fork. Then go back to the [plugin-hub](https://github.com/runelite/plugin-hub) and click *New pull request* in the upper left. Choose *Compare across forks* and select your fork and branch as head and compare.

 7. Write a short description of what your plugin does and then create your pull request.

 8. Be patient and wait for your plugin to be reviewed and merged.

## Updating a plugin
To update a plugin, simply update the manifest with the most recent commit hash. 

## Reviewing
We will review your plugin to ensure it isn't malicious or [breaking
jagex's rules](https://secure.runescape.com/m=news/another-message-about-unofficial-clients?oldschool=1).
__If it is difficult for us to ensure the plugin isn't against the rules we
will not merge it__. 