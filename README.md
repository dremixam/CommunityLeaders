# Community Leaders Mod for Minecraft Fabric

**Community Leaders** is a Minecraft Fabric mod designed to help streamers and community leaders easily manage their servers by inviting, uninviting, and banning members of their community. The mod works with the LuckPerms permission management system.

## Features

### Invitation Management
- **Personalized invitations**: Streamers can invite specific players to their server
- **Integrated whitelist system**: Automatically adds invited players to the whitelist
- **Traceability**: Tracks who invited whom for better control

### Member Management
- **Uninvite**: Removes a player from the whitelist and disconnects them from the server
- **Ban**: Permanently bans a player invited by the streamer
- **Access control**: Only streamers can manage their own invited players

### Rules System
- **Custom rules screen**: Automatically displays server rules to new players
- **Mandatory validation**: Players must accept the rules to play
- **Graphical interface**: Screen with scrolling, checkbox, and customizable buttons

### Security and Permissions
- **LuckPerms integration**: Uses LuckPerms for permission management
- **Granular permissions**: Precise control over authorized actions
- **Rights validation**: Automatic verification of permissions before each action

## Requirements

- **Minecraft**: Version 1.20.1
- **Fabric Loader**: Recent version
- **Fabric API**: Included with the mod
- **LuckPerms**: Required for permission management

## Installation

1. **Download dependencies**:
   - Install [Fabric Loader](https://fabricmc.net/)
   - Download [LuckPerms for Fabric](https://luckperms.net/)

2. **Install the mod**:
   - Place the `CommunityLeaders-1.0-SNAPSHOT.jar` file in the `mods/` folder
   - Also place LuckPerms in the `mods/` folder

3. **Start the server**:
   - The mod will configure itself automatically on first startup
   - Configuration files will be created in `config/communityleaders/`

## Configuration

### Permission Setup

Create a group for your streamers in LuckPerms:

```bash
# Create the streamer group
/lp creategroup streamer

# Set permissions for the streamer group
/lp group streamer permission set communityleaders.invite true
/lp group streamer permission set communityleaders.uninvite true
/lp group streamer permission set communityleaders.ban true

# Assign the streamer group to a user
/lp user <streamer_name> parent add streamer
```

### Mod Configuration

The `config/communityleaders/config.yml` file allows you to customize:

#### Rules System
```yaml
rules:
  enabled: true
  title: "§6§lServer Rules"
  content: |
    §b§lWelcome to our community server!
    
    §fBy playing on this server, you agree to follow these rules:
    
    §a1. §fRespect other players and their builds
    §a2. §fNo griefing, stealing or intentional destruction
    §a3. §fNo offensive language or harassment
    §a4. §fRespect protected areas and private properties
    §a5. §fNo cheating, hacking or bug exploitation
    §a6. §fListen to and respect moderators and administrators
    
    §c§lBreaking these rules may result in a warning,
    §c§ltemporary suspension or permanent ban.
    
    §e§lThank you for helping maintain a friendly community!
  accept_button: "I Accept"
  decline_button: "I Decline"
  checkbox_text: "I understand and accept the rules"
  decline_message: "You must accept the rules to play on this server."
```

#### Custom Messages
```yaml
messages:
  invite_success: "Successfully invited '%player%' to the server!"
  uninvite_success: "Successfully uninvited '%player%' from the server."
  ban_success: "Successfully banned '%player%' from the server."
  player_not_found: "Player '%player%' not found."
  # ... and many other customizable messages
```

## Usage

### Available Commands

#### `/invite <player_name>`
Invites a player to the server by adding them to the whitelist.

**Required permissions**: `communityleaders.invite`

**Example**:
```bash
/invite Steve
```

#### `/uninvite <player_name>`
Uninvites a player you invited, removes them from the whitelist and disconnects them.

**Required permissions**: `communityleaders.uninvite`

**Restrictions**: You can only uninvite players you invited yourself.

**Example**:
```bash
/uninvite Steve
```

#### `/kickban <player_name>`
Permanently bans a player you invited.

**Required permissions**: `communityleaders.ban`

**Restrictions**: You can only ban players you invited yourself.

**Example**:
```bash
/kickban Steve
```

### Rules System

When a new player connects to the server:

1. **Automatic display**: The rules screen appears automatically
2. **Mandatory reading**: The player must scroll to the bottom
3. **Validation**: A checkbox becomes available after complete reading
4. **Acceptance**: The player must check the box and click "I Accept"
5. **Memory**: Acceptance is saved, no need to re-validate

## License

This project is under "All Rights Reserved" license. All rights reserved to the author.

## Author

**DrEmixam** - Main developer

## Support

To report bugs or request features, please create an issue on the project repository.

---

*Community Leaders - Manage your Minecraft community easily!*
