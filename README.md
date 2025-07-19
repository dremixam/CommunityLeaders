# Community Leaders Mod for Minecraft Fabric

**Community Leaders** is a Minecraft Fabric mod designed to help **server administrators** delegate community management to trusted **community leaders**. Server admins can grant invitation permissions to streamers, content creators, or community managers, who can then manage their own communities on the server and delegate moderation tasks to their trusted members.

## Concept

The mod creates a **three-tier management hierarchy**:

1. **Server Administrators** → Grant invitation permissions to trusted community leaders
2. **Community Leaders** → Manage their own community members and assign moderators  
3. **Moderators** → Help leaders manage their communities with delegated permissions

This system allows server admins to scale their community management by empowering trusted leaders while maintaining oversight and control.

## Features

### Administrative Delegation
- **Permission-based leadership**: Admins grant `communityleaders.invite` permission to trusted community leaders
- **Autonomous community management**: Leaders can independently invite and manage their community members
- **Scalable server growth**: Support multiple communities on a single server without admin micromanagement
- **Maintained oversight**: All actions are tracked and can be audited by server administrators

### Community Management
- **Personalized invitations**: Community leaders can invite their followers, subscribers, or community members
- **Integrated whitelist system**: Automatically manages server whitelist for invited players
- **Invitation limits**: Configurable limits per leader (with admin override capability)
- **Member lifecycle management**: Full control over invitation, removal, and banning of community members

### Moderation Delegation
- **Trusted moderator system**: Leaders can delegate permissions to trusted community members
- **Hierarchical management**: Moderators act on behalf of their assigned leader
- **Automatic validation**: System ensures moderators don't gain conflicting permissions
- **Flexible delegation**: Leaders can add/remove moderators as their community evolves

### Advanced Administrative Tools
- **Invitation tree visualization**: Server admins can view the complete community structure
- **Data consistency monitoring**: Automatic validation and cleanup of permission relationships
- **Real-time synchronization**: Integration with LuckPerms for instant permission updates
- **Command customization**: Configurable aliases and messages for server branding

### Security and Permissions
- **LuckPerms integration**: Leverages enterprise-grade permission management
- **Granular access control**: Fine-tuned permissions for each management level
- **Automatic validation**: Real-time verification of permissions and relationships
- **Consistency enforcement**: Automatic cleanup of invalid data when permissions change

## Requirements

- **Minecraft**: Version 1.20.1
- **Fabric Loader**: Version 0.16.14+
- **Fabric API**: Version 0.92.6+1.20.1
- **LuckPerms**: Version 5.4+ (Required for permission management)

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

## Setup Guide for Server Administrators

### 1. Create Community Leader Groups

Set up different tiers of community leaders in LuckPerms:

```bash
# Create community leader groups
/lp creategroup community_leader
/lp creategroup premium_leader

# Basic community leader permissions
/lp group community_leader permission set communityleaders.invite true
/lp group community_leader permission set communityleaders.ban true
/lp group community_leader permission set communityleaders.moderator true

# Premium leaders get additional permissions
/lp group premium_leader parent add community_leader
/lp group premium_leader permission set communityleaders.tree true
/lp group premium_leader permission set communityleaders.unlimited true

# Assign leadership to trusted community members
/lp user <streamer_name> parent add community_leader
/lp user <premium_creator> parent add premium_leader
```

### 2. Server Administrator Permissions

For full server oversight, admins should have all permissions. Server operators automatically have full access, but you can explicitly set permissions for the admin group:

```bash
# Grant admin full access (optional - admins usually have * permission)
/lp group admin permission set communityleaders.* true
```

### Available Permissions

The mod provides granular permissions that can be assigned individually:

**`communityleaders.invite`**
- Allows inviting players to the server via `/cl invite`
- Allows uninviting players you invited via `/cl uninvite`
- Allows viewing your invitation list via `/cl list`
- Subject to invitation limits (unless `communityleaders.unlimited` is also granted)

**`communityleaders.ban`**
- Allows permanently banning players you invited via `/cl ban`
- Banned players are disconnected, added to server ban list, and removed from whitelist
- Can only ban players you personally invited

**`communityleaders.tree`**
- Allows viewing the complete server invitation structure via `/cl tree`
- Shows all leaders and their invited communities
- Useful for server administrators and premium community leaders

**`communityleaders.moderator`**
- Allows managing moderators via `/cl mod add/remove/list`
- Can promote invited players to moderator status
- Moderators can use leader commands on behalf of their assigned leader
- Includes automatic validation to prevent permission conflicts

**`communityleaders.unlimited`**
- Bypasses invitation limits set in configuration
- Allows unlimited invitations regardless of `max_invitations_per_leader` setting
- Typically granted to trusted premium leaders or server staff

**Permission Combinations:**
- **Basic Community Leader**: `communityleaders.invite` + `communityleaders.ban`
- **Advanced Community Leader**: Add `communityleaders.moderator`
- **Premium Community Leader**: Add `communityleaders.tree` + `communityleaders.unlimited`
- **Server Administrator**: Grant all permissions or use wildcard `communityleaders.*`

## Configuration for Administrators

The `config/communityleaders/config.yml` file allows server-wide customization:

```yaml
# Community Leaders Configuration
# Administrative settings for community delegation system

# Command configuration
command:
  alias: "cl"  # Alias for /communityleaders command (customize for your server)

# Community management limits
limits:
  max_invitations_per_leader: 10  # Default limit per leader (-1 for unlimited)
  # Note: Leaders with 'communityleaders.unlimited' permission bypass this limit

# Server branding - customize all messages
messages:
  # Customize all messages to match your server's tone and language
  invite_success: "Welcome to our community! '%player%' has been invited by their community leader."
  ban_reason: "Community guidelines violation - banned by community leader."
  # ... (all other messages can be customized)
```

## Usage for Community Leaders

Community leaders use the mod to manage their own communities within the server:

### Core Management Commands

The mod provides two command formats:
- Full command: `/communityleaders <subcommand>`
- Server alias: `/cl <subcommand>` (configurable by admins)

#### Member Invitation and Management

- `/cl invite <player>` - Invite a community member to the server
- `/cl uninvite <player>` - Remove a community member you invited
- `/cl ban <player>` - Permanently ban a community member you invited
- `/cl list` - View all members you've invited to the server

#### Moderation Delegation

- `/cl mod add <player>` - Promote an invited member to moderator for your community
- `/cl mod remove <player>` - Remove moderator status from a member  
- `/cl mod list` - View all your current moderators

#### Community Overview

- `/cl tree` - View the complete community structure

### How Moderation Works

1. **Leader invites community members** using `/cl invite`
2. **Leader promotes trusted members** to moderators using `/cl mod add`
3. **Moderators can then manage the leader's community** using the same commands
4. **All actions are attributed to the leader** for administrative tracking

## Administrative Tools

### Monitoring Community Growth

Server administrators can monitor community development:

```bash
# View complete server community structure
/cl tree

# Check specific leader's community
/lp user <leader> info
```

### Data Storage and Backup

The mod stores community data in JSON files for easy administration:

- `config/communityleaders/config.yml` - Server configuration
- `config/communityleaders/invites.json` - Community invitation relationships  
- `config/communityleaders/moderators.json` - Moderation delegation structure

**Admin Tip**: Regularly backup these files to preserve community structures.

### Automated Consistency Management

The mod automatically maintains data integrity:

- **Permission synchronization**: When leaders lose permissions, their communities are cleaned up
- **Moderation validation**: Moderators who gain leader permissions are automatically managed
- **Real-time updates**: Changes via LuckPerms are immediately reflected in community structures
- **Startup validation**: Complete consistency check on each server restart

## Troubleshooting for Administrators

### Common Setup Issues

1. **"LuckPerms not found" warning**:
   - Ensure LuckPerms loads before Community Leaders
   - Verify LuckPerms is properly configured

2. **Community leaders can't use commands**:
   - Check LuckPerms permission assignments: `/lp user <leader> permission check communityleaders.invite`
   - Verify group inheritance: `/lp user <leader> info`

3. **Whitelist integration issues**:
   - Enable server whitelist for best results: `/whitelist on`
   - The mod automatically manages whitelist entries for invited players

### Performance Considerations

- **Invitation limits**: Set reasonable limits to prevent server overload
- **Regular cleanup**: The mod automatically cleans up invalid relationships
- **Monitoring**: Check server logs for consistency check results

## Migration and Integration

### From Other Whitelist Systems

The mod can work alongside existing whitelist management:
1. Existing whitelist entries are preserved
2. Community Leaders only manages entries it creates
3. Admins retain full whitelist control

## Support for Server Administrators

## License

This project is licensed under AGPL-3.0.

## Author

**DrEmixam** - Main developer

## Support

For server setup assistance or bug reports, create an issue on the project repository.